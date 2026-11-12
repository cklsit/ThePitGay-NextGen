package net.mizukilab.pit.database;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.*;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.Getter;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mongojack.JacksonMongoCollection;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 数据访问门面。
 *
 * <p>后端由 {@code storage.type} 决定：{@code sqlite}（默认，单文件 thepit.db）或 {@code mongodb}。
 * 业务代码只依赖 {@link cn.charlotte.pit.data.storage.DocumentCollection}，
 * 切换后端时序列化结果保持一致。</p>
 */
@Getter
public class MongoDB {

    private static final Logger log = ThePit.getInstance().getLogger();

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private JacksonMongoCollection<PlayerProfile> profileCollection;
    private JacksonMongoCollection<TradeData> tradeCollection;
    private JacksonMongoCollection<PlayerMailData> mailCollection;
    private JacksonMongoCollection<PlayerInvBackup> invCollection;
    private JacksonMongoCollection<CDKData> cdkCollection;
    private JacksonMongoCollection<FixedRewardData> rewardCollection;
    private JacksonMongoCollection<EventQueue> eventQueueCollection;

    // 统一存储访问层（Mongo / SQLite 双后端）
    private cn.charlotte.pit.data.storage.SqliteStorage sqliteStorage;
    private cn.charlotte.pit.data.storage.DocumentCollection<PlayerProfile> profiles;
    private cn.charlotte.pit.data.storage.DocumentCollection<PlayerMailData> mails;
    private cn.charlotte.pit.data.storage.DocumentCollection<PlayerInvBackup> invBackups;
    private cn.charlotte.pit.data.storage.DocumentCollection<FixedRewardData> rewards;
    private cn.charlotte.pit.data.storage.DocumentCollection<EventQueue> eventQueue;
    private cn.charlotte.pit.data.storage.DocumentCollection<CDKData> cdks;
    private cn.charlotte.pit.data.storage.DocumentCollection<TradeData> trades;
    /** 管理端保存的神话物品模板（原实现直接读写 Mongo 集合）。 */
    private RawCollection savedMythicItems;

    public void connect() {
        log.info("Connecting to database... (正在连接数据库<<<<)");
        Instant connects = Instant.now();

        String storageType = "sqlite";
        net.mizukilab.pit.config.PitGlobalConfig globalConfig = ThePit.getInstance().getGlobalConfig();
        if (globalConfig != null) {
            String configured = globalConfig.getStorageType();
            if (configured != null && !configured.isEmpty()) {
                storageType = configured;
            }
        }
        if ("sqlite".equalsIgnoreCase(storageType)) {
            try {
                this.sqliteStorage = new cn.charlotte.pit.data.storage.SqliteStorage(
                        new java.io.File(ThePit.getInstance().getDataFolder(), "thepit.db"));
            } catch (java.sql.SQLException e) {
                throw new RuntimeException("初始化 SQLite 存储失败", e);
            }
            this.profiles = new cn.charlotte.pit.data.storage.DocumentCollection<>(null, sqliteStorage, "players", "uuid", PlayerProfile.class);
            this.mails = new cn.charlotte.pit.data.storage.DocumentCollection<>(null, sqliteStorage, "mail", "uuid", PlayerMailData.class);
            this.invBackups = new cn.charlotte.pit.data.storage.DocumentCollection<>(null, sqliteStorage, "inv", "backupUuid", PlayerInvBackup.class);
            this.rewards = new cn.charlotte.pit.data.storage.DocumentCollection<>(null, sqliteStorage, "reward", "mailId", FixedRewardData.class);
            this.eventQueue = new cn.charlotte.pit.data.storage.DocumentCollection<>(null, sqliteStorage, "event_queue", "id", EventQueue.class);
            this.cdks = new cn.charlotte.pit.data.storage.DocumentCollection<>(null, sqliteStorage, "cdk", "cdk", CDKData.class);
            this.trades = new cn.charlotte.pit.data.storage.DocumentCollection<>(null, sqliteStorage, "trade", "tradeUuid", TradeData.class);
            this.savedMythicItems = new RawCollection(null, sqliteStorage, "saved_mythic_items");
            log.info("Storage backend: SQLite (connected)");
            log.info("Connected! (连接成功>>>>)");
            log.info("Costs " + ChronoUnit.MILLIS.between(connects, Instant.now()));
            return;
        }

        String address = ThePit.getInstance().getGlobalConfig().getMongoDBAddress();
        int port = ThePit.getInstance().getGlobalConfig().getMongoDBPort();

        final String mongoUser = ThePit.getInstance().getGlobalConfig().getMongoUser();
        final String mongoPassword = ThePit.getInstance().getGlobalConfig().getMongoPassword();
        final String certDatabase = ThePit.getInstance().getGlobalConfig().getCertDatabaseName();
        final String databaseName;
        if (ThePit.getInstance().getGlobalConfig().getDatabaseName() == null) {
            databaseName = "thePit";
        } else {
            databaseName = ThePit.getInstance().getGlobalConfig().getDatabaseName();
        }

        ConnectionString connectionString = new ConnectionString("mongodb://" + address + ":" + port);
        MongoClientSettings thePit;
        MongoClientSettings.Builder builder1 = MongoClientSettings.builder().serverApi(ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build()).uuidRepresentation(UuidRepresentation.STANDARD).applyConnectionString(connectionString);
        if (mongoUser != null && mongoPassword != null && !mongoUser.isEmpty() && !mongoPassword.isEmpty()) {
            String authDatabase = certDatabase == null || certDatabase.isBlank() ? databaseName : certDatabase;
            MongoCredential credential = MongoCredential.createCredential(mongoUser, authDatabase, mongoPassword.toCharArray());
            thePit = builder1
                    .credential(credential).applicationName("ThePitSafe")
                    .build();
        } else {
            thePit = builder1.applicationName("ThePitUnsafe").build();
        }
        this.mongoClient = MongoClients.create(thePit);
        this.database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection("players");

        createIndex(collection, "uuidIndex", "uuid");

        createIndex(collection, "lowerNameIndex", "lowerName");

        final MongoCollection<Document> tradeCollection = database.getCollection("trade");
        createIndex(tradeCollection, "playerAIndex", "playerA");
        createIndex(tradeCollection, "playerBIndex", "playerB");
        createIndex(tradeCollection, "tradeUuidIndex", "tradeUuid");


        final MongoCollection<Document> invCollection = database.getCollection("inv");
        createIndex(invCollection, "uuidIndex", "uuid");
        createIndex(invCollection, "backupUuidIndex", "backupUuid");

        ensureTimeIndex(tradeCollection, "completeTime");
        ensureTimeIndex(invCollection, "timeStamp");

        JacksonMongoCollection.JacksonMongoCollectionBuilder builder = JacksonMongoCollection.builder();
        this.profileCollection = builder.build(this.database.getCollection("players", PlayerProfile.class), PlayerProfile.class, UuidRepresentation.JAVA_LEGACY);

        this.tradeCollection = builder.build(this.database.getCollection("trade", TradeData.class), TradeData.class, UuidRepresentation.JAVA_LEGACY);

        this.mailCollection = builder.build(this.database.getCollection("mail", PlayerMailData.class), PlayerMailData.class, UuidRepresentation.JAVA_LEGACY);

        this.invCollection = builder.build(this.database.getCollection("inv", PlayerInvBackup.class), PlayerInvBackup.class, UuidRepresentation.JAVA_LEGACY);

        this.cdkCollection = builder.build(this.database.getCollection("cdk", CDKData.class), CDKData.class, UuidRepresentation.JAVA_LEGACY);

        this.rewardCollection = builder.build(this.database.getCollection("reward", FixedRewardData.class), FixedRewardData.class, UuidRepresentation.JAVA_LEGACY);

        this.eventQueueCollection = builder.build(this.database.getCollection("event_queue", EventQueue.class), EventQueue.class, UuidRepresentation.JAVA_LEGACY);

        createIndex(mailCollection, "uuidIndex", "uuid");

        this.profiles = new cn.charlotte.pit.data.storage.DocumentCollection<>(profileCollection, null, "players", "uuid", PlayerProfile.class);
        this.mails = new cn.charlotte.pit.data.storage.DocumentCollection<>(mailCollection, null, "mail", "uuid", PlayerMailData.class);
        this.invBackups = new cn.charlotte.pit.data.storage.DocumentCollection<>(this.invCollection, null, "inv", "backupUuid", PlayerInvBackup.class);
        this.rewards = new cn.charlotte.pit.data.storage.DocumentCollection<>(rewardCollection, null, "reward", "mailId", FixedRewardData.class);
        this.eventQueue = new cn.charlotte.pit.data.storage.DocumentCollection<>(eventQueueCollection, null, "event_queue", "id", EventQueue.class);
        this.cdks = new cn.charlotte.pit.data.storage.DocumentCollection<>(cdkCollection, null, "cdk", "cdk", CDKData.class);
        this.trades = new cn.charlotte.pit.data.storage.DocumentCollection<>(this.tradeCollection, null, "trade", "tradeUuid", TradeData.class);
        this.savedMythicItems = new RawCollection(database.getCollection("saved_mythic_items"), null, "saved_mythic_items");
        log.info("Storage backend: MongoDB (connected)");
        log.info("Connected! (连接成功>>>>)");
        log.info("Costs " + ChronoUnit.MILLIS.between(connects, Instant.now()));
    }

    public void close() {
        if (sqliteStorage != null) {
            sqliteStorage.close();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * 后端无关的「原始文档集合」：给管理端保存的物品模板这类临时数据用。
     *
     * <p>SQLite 后端下文档以 JSON 存在单表里，过滤 / 排序 / 分页在内存里做；
     * 这类数据量很小（管理端手动保存），可以接受。</p>
     */
    public static final class RawCollection {

        private final MongoCollection<Document> mongo;
        private final cn.charlotte.pit.data.storage.SqliteStorage sqlite;
        private final String name;

        RawCollection(@Nullable MongoCollection<Document> mongo,
                      @Nullable cn.charlotte.pit.data.storage.SqliteStorage sqlite,
                      @NotNull String name) {
            this.mongo = mongo;
            this.sqlite = sqlite;
            this.name = name;
        }

        public void insertOne(@NotNull Map<String, Object> document) {
            if (mongo != null) {
                mongo.insertOne(new Document(document));
                return;
            }
            String id = document.get("_id") == null ? UUID.randomUUID().toString() : String.valueOf(document.get("_id"));
            sqlite.save(name, id, document);
        }

        /** 按字段 upsert。 */
        public void replaceOne(@NotNull String field, @NotNull Object value, @NotNull Map<String, Object> document) {
            if (mongo != null) {
                mongo.replaceOne(Filters.eq(field, value), new Document(document),
                        new com.mongodb.client.model.ReplaceOptions().upsert(true));
                return;
            }
            Map<String, Object> existing = findOne(field, value);
            Object id = document.get("_id");
            if (id == null) {
                id = existing == null ? null : existing.get("_id");
            }
            String rowId = id == null ? UUID.randomUUID().toString() : String.valueOf(id);
            if (existing != null) {
                sqlite.delete(name, String.valueOf(existing.get("_id") == null ? rowId : existing.get("_id")));
            }
            sqlite.save(name, rowId, document);
        }

        @Nullable
        public Map<String, Object> findOne(@NotNull String field, @NotNull Object value) {
            List<Map<String, Object>> found = find(field, value);
            return found.isEmpty() ? null : found.get(0);
        }

        /** 删除并返回删除条数。 */
        public long deleteOneCount(@NotNull String field, @NotNull Object value) {
            List<Map<String, Object>> matched = find(field, value);
            for (Map<String, Object> document : matched) {
                Object id = document.get("_id");
                if (id == null) {
                    if (mongo != null) {
                        mongo.deleteOne(Filters.eq(field, value));
                    }
                    continue;
                }
                if (mongo != null) {
                    mongo.deleteOne(Filters.eq("_id", id));
                } else {
                    sqlite.delete(name, String.valueOf(id));
                }
            }
            return matched.size();
        }

        /** 分页：按 {@code sortField} 排序后取 [skip, skip+limit)。 */
        @NotNull
        public List<Map<String, Object>> page(@Nullable String field, @Nullable Object value,
                                              @NotNull String sortField, boolean desc,
                                              int skip, int limit) {
            List<Map<String, Object>> source = field == null ? findAll() : find(field, value);
            List<Map<String, Object>> sorted = sortDesc(source, sortField);
            if (!desc) {
                List<Map<String, Object>> reversed = new ArrayList<>(sorted);
                java.util.Collections.reverse(reversed);
                sorted = reversed;
            }
            int from = Math.min(Math.max(0, skip), sorted.size());
            int to = Math.min(from + Math.max(0, limit), sorted.size());
            return new ArrayList<>(sorted.subList(from, to));
        }

        public long countAll() {
            return findAll().size();
        }

        @NotNull
        public List<Map<String, Object>> findAll() {
            if (mongo != null) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Document document : mongo.find()) {
                    result.add(new LinkedHashMap<>(document));
                }
                return result;
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<?, ?> raw : sqlite.loadAll(name, Map.class)) {
                Map<String, Object> converted = new LinkedHashMap<>();
                raw.forEach((key, value) -> converted.put(String.valueOf(key), value));
                result.add(converted);
            }
            return result;
        }

        @NotNull
        public List<Map<String, Object>> find(@NotNull String field, @NotNull Object value) {
            List<Map<String, Object>> matched = new ArrayList<>();
            String expected = String.valueOf(value);
            for (Map<String, Object> document : findAll()) {
                Object actual = document.get(field);
                if (actual != null && expected.equals(String.valueOf(actual))) {
                    matched.add(document);
                }
            }
            return matched;
        }

        public long count(@Nullable String field, @Nullable Object value) {
            if (field == null) {
                return findAll().size();
            }
            return find(field, value).size();
        }

        public void deleteOne(@NotNull String field, @NotNull Object value) {
            List<Map<String, Object>> matched = find(field, value);
            for (Map<String, Object> document : matched) {
                Object id = document.get("_id");
                if (id == null) continue;
                if (mongo != null) {
                    mongo.deleteOne(Filters.eq("_id", id));
                } else {
                    sqlite.delete(name, String.valueOf(id));
                }
            }
        }

        /** 按字段降序排序（SQLite 后端在内存里排）。 */
        @NotNull
        public List<Map<String, Object>> sortDesc(@NotNull List<Map<String, Object>> documents, @NotNull String field) {
            List<Map<String, Object>> copy = new ArrayList<>(documents);
            copy.sort(Comparator.comparingDouble((Map<String, Object> document) -> {
                Object value = document.get(field);
                return value instanceof Number number ? number.doubleValue() : 0.0D;
            }).reversed());
            return copy;
        }
    }

    static void ensureTimeIndex(MongoCollection<Document> collection, String field) {
        try (var indexes = collection.listIndexes().iterator()) {
            while (indexes.hasNext()) {
                Document index = indexes.next();
                Document key = index.get("key", Document.class);
                // Either direction supports a single-field sort. Preserve existing index names.
                if (key != null && key.size() == 1 && key.get(field) instanceof Number order
                        && Math.abs(order.doubleValue()) == 1.0
                        && !index.containsKey("partialFilterExpression") && !Boolean.TRUE.equals(index.getBoolean("sparse"))) return;
            }
        }
        collection.createIndex(Indexes.ascending(field));
    }

    private void createIndex(MongoCollection<?> collection, String indexName, String fieldName) {
        try {
            IndexOptions indexOptions = new IndexOptions().name(indexName);
            Bson index = Indexes.ascending(fieldName);
            collection.createIndex(index, indexOptions);
        } catch (Exception ignore) {

        }
    }
}
