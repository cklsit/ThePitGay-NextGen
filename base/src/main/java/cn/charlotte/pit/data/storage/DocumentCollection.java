package cn.charlotte.pit.data.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.ReplaceOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mongojack.JacksonMongoCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

/**
 * 一个「文档集合」的抽象，屏蔽底层是 Mongo 还是 SQLite。
 * <p>
 * 只暴露 {@link #findOne}/{@link #find}/{@link #findAll}/{@link #replaceOne}/{@link #deleteOne} 原语；
 * SQLite 后端用 Jackson 把文档序列化为 JSON 存到 {@link SqliteStorage} 的单表里，
 * 匹配与查找在内存中按字段值过滤，写入时以 {@code keyField} 作为行主键。
 */
public final class DocumentCollection<T> {

    private final JacksonMongoCollection<T> mongo;
    private final SqliteStorage sqlite;
    private final String name;
    private final String keyField;
    private final Class<T> type;
    private final ObjectMapper mapper;

    public DocumentCollection(@Nullable JacksonMongoCollection<T> mongo,
                              @Nullable SqliteStorage sqlite,
                              @NotNull String name,
                              @NotNull String keyField,
                              @NotNull Class<T> type) {
        this.mongo = mongo;
        this.sqlite = sqlite;
        this.name = name;
        this.keyField = keyField;
        this.type = type;
        this.mapper = sqlite == null ? new ObjectMapper() : sqlite.getMapper();
    }

    @Nullable
    public T findOne(@NotNull String field, @NotNull Object value) {
        if (mongo != null) {
            return mongo.findOne(eq(field, value));
        }
        for (T doc : sqlite.loadAll(name, type)) {
            if (value.toString().equals(fieldOf(doc, field))) {
                return doc;
            }
        }
        return null;
    }

    @Nullable
    public T findOne() {
        if (mongo != null) {
            return mongo.find().first();
        }
        List<T> all = sqlite.loadAll(name, type);
        return all.isEmpty() ? null : all.get(0);
    }

    @NotNull
    public List<T> find(@NotNull String field, @NotNull Object value) {
        if (mongo != null) {
            List<T> result = new ArrayList<>();
            mongo.find(eq(field, value)).forEach(result::add);
            return result;
        }
        List<T> result = new ArrayList<>();
        for (T doc : sqlite.loadAll(name, type)) {
            if (value.toString().equals(fieldOf(doc, field))) {
                result.add(doc);
            }
        }
        return result;
    }

    @NotNull
    public List<T> findAll() {
        if (mongo != null) {
            List<T> result = new ArrayList<>();
            mongo.find().forEach(result::add);
            return result;
        }
        return sqlite.loadAll(name, type);
    }

    public void replaceOne(@NotNull String field, @NotNull Object value, @NotNull T document) {
        if (mongo != null) {
            mongo.replaceOne(eq(field, value), document);
            return;
        }
        String id;
        if (field.equals(keyField)) {
            id = value.toString();
        } else {
            String existingId = fieldOf(document, keyField);
            if (existingId == null || existingId.isEmpty()) {
                existingId = UUID.randomUUID().toString();
            }
            id = existingId;
        }
        sqlite.save(name, id, document);
    }

    public void deleteOne(@NotNull String field, @NotNull Object value) {
        if (mongo != null) {
            mongo.deleteOne(eq(field, value));
            return;
        }
        sqlite.delete(name, value.toString());
    }

    private String fieldOf(T doc, String field) {
        JsonNode node = mapper.valueToTree(doc);
        JsonNode value = node.get(field);
        return value == null ? null : value.asText();
    }

    public Class<T> getType() {
        return type;
    }
}
