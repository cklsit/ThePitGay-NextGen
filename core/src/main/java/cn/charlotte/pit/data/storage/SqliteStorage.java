package cn.charlotte.pit.data.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 SQLite 的文档存储（默认存储后端）。
 * <p>
 * 设计：所有实体都以「集合 + 主键 + JSON」的形式存放在同一张表里，
 * 与 Mongo 的 collection/document 一一对应，切换后端时序列化结果保持一致。
 * <pre>
 * CREATE TABLE documents (
 *   collection TEXT NOT NULL,
 *   id         TEXT NOT NULL,
 *   data       TEXT NOT NULL,
 *   updated_at INTEGER NOT NULL,
 *   PRIMARY KEY (collection, id)
 * );
 * </pre>
 */
public class SqliteStorage implements Closeable {

    private final Connection connection;
    private final ObjectMapper mapper;

    public SqliteStorage(@NotNull File databaseFile) throws SQLException {
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("无法创建数据库目录: " + parent);
        }
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        init();
    }

    private void init() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS documents (
                      collection TEXT NOT NULL,
                      id         TEXT NOT NULL,
                      data       TEXT NOT NULL,
                      updated_at INTEGER NOT NULL,
                      PRIMARY KEY (collection, id)
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_documents_collection ON documents(collection)");
            // WAL 提升并发读性能
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
        }
    }

    public synchronized void save(@NotNull String collection, @NotNull String id, @NotNull Object value) {
        try {
            String json = mapper.writeValueAsString(value);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO documents (collection, id, data, updated_at) VALUES (?, ?, ?, ?)
                    ON CONFLICT(collection, id) DO UPDATE SET data = excluded.data, updated_at = excluded.updated_at
                    """)) {
                statement.setString(1, collection);
                statement.setString(2, id);
                statement.setString(3, json);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("保存文档失败: " + collection + "/" + id, exception);
        }
    }

    @Nullable
    public synchronized <T> T load(@NotNull String collection, @NotNull String id, @NotNull Class<T> type) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT data FROM documents WHERE collection = ? AND id = ?")) {
            statement.setString(1, collection);
            statement.setString(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapper.readValue(resultSet.getString("data"), type);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("读取文档失败: " + collection + "/" + id, exception);
        }
    }

    @NotNull
    public synchronized <T> List<T> loadAll(@NotNull String collection, @NotNull Class<T> type) {
        List<T> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT data FROM documents WHERE collection = ?")) {
            statement.setString(1, collection);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapper.readValue(resultSet.getString("data"), type));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("批量读取失败: " + collection, exception);
        }
        return result;
    }

    public synchronized void delete(@NotNull String collection, @NotNull String id) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM documents WHERE collection = ? AND id = ?")) {
            statement.setString(1, collection);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("删除文档失败: " + collection + "/" + id, exception);
        }
    }

    public synchronized boolean exists(@NotNull String collection, @NotNull String id) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM documents WHERE collection = ? AND id = ? LIMIT 1")) {
            statement.setString(1, collection);
            statement.setString(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("查询文档失败: " + collection + "/" + id, exception);
        }
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
