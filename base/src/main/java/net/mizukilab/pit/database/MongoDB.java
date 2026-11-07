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
import org.mongojack.JacksonMongoCollection;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * 2 * @Author: KleeLoveLife
 * 3 * @Date: 2020/12/28 23:03
 * 4
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
            this.profiles = new cn.charlotte.pit.data.storage.DocumentCollection<PlayerProfile>(null, sqliteStorage, "players", "uuid", PlayerProfile.class);
            this.mails = new cn.charlotte.pit.data.storage.DocumentCollection<PlayerMailData>(null, sqliteStorage, "mail", "uuid", PlayerMailData.class);
            this.invBackups = new cn.charlotte.pit.data.storage.DocumentCollection<PlayerInvBackup>(null, sqliteStorage, "inv", "backupUuid", PlayerInvBackup.class);
            this.rewards = new cn.charlotte.pit.data.storage.DocumentCollection<FixedRewardData>(null, sqliteStorage, "reward", "mailId", FixedRewardData.class);
            this.eventQueue = new cn.charlotte.pit.data.storage.DocumentCollection<EventQueue>(null, sqliteStorage, "event_queue", "id", EventQueue.class);
            this.cdks = new cn.charlotte.pit.data.storage.DocumentCollection<CDKData>(null, sqliteStorage, "cdk", "cdk", CDKData.class);
            this.trades = new cn.charlotte.pit.data.storage.DocumentCollection<TradeData>(null, sqliteStorage, "trade", "tradeUuid", TradeData.class);
            log.info("Storage backend: SQLite (connected)");
            return;
        }


        String address = ThePit.getInstance().getGlobalConfig().getMongoDBAddress();
        int port = ThePit.getInstance().getGlobalConfig().getMongoDBPort();

        final String mongoUser = ThePit.getInstance().getGlobalConfig().getMongoUser();
        final String mongoPassword = ThePit.getInstance().getGlobalConfig().getMongoPassword();

        final String databaseName;
        if (ThePit.getInstance().getGlobalConfig().getDatabaseName() == null) {
            databaseName = "thePit";
        } else {
            databaseName = ThePit.getInstance().getGlobalConfig().getDatabaseName();
        }

        //hook PowerOFTwo
        ConnectionString connectionString = new ConnectionString("mongodb://" + address + ":" + port);
        MongoClientSettings thePit;
        MongoClientSettings.Builder builder1 = MongoClientSettings.builder().serverApi(ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build()).uuidRepresentation(UuidRepresentation.STANDARD).applyConnectionString(connectionString);
        if (mongoUser != null && mongoPassword != null && !mongoUser.isEmpty() && !mongoPassword.isEmpty()) {
            MongoCredential credential = MongoCredential.createCredential(mongoUser, databaseName, mongoPassword.toCharArray());
            thePit = builder1
                    .credential(credential).applicationName("ThePitRequiredPass")
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

        //create trade index
        MongoCollection<Document> trade = database.getCollection("trade");
        boolean indexFound = false;
        for (Document listIndex : trade.listIndexes()) {
            if (listIndex.get("completeTime") != null) {
                indexFound = true;
                if (listIndex.getInteger("completeTime") == -1) {
                    trade.createIndex(Filters.eq("completeTime", 1));
                }
            }
        }

        if (!indexFound) {
            trade.createIndex(Filters.eq("timeStamp", 1));
        }


        MongoCollection<Document> inv = database.getCollection("inv");
        indexFound = false;
        for (Document listIndex : inv.listIndexes()) {
            if (listIndex.get("timeStamp") != null) {
                indexFound = true;
                if (listIndex.getInteger("timeStamp") == -1) {
                    trade.createIndex(Filters.eq("timeStamp", 1));
                }
            }
        }
        if (!indexFound) {
            trade.createIndex(Filters.eq("timeStamp", 1));
        }


        JacksonMongoCollection.JacksonMongoCollectionBuilder builder = JacksonMongoCollection.builder();
        this.profileCollection = builder.build(this.database.getCollection("players", PlayerProfile.class), PlayerProfile.class, UuidRepresentation.JAVA_LEGACY);

        this.tradeCollection = builder.build(this.database.getCollection("trade", TradeData.class), TradeData.class, UuidRepresentation.JAVA_LEGACY);

        this.mailCollection = builder.build(this.database.getCollection("mail", PlayerMailData.class), PlayerMailData.class, UuidRepresentation.JAVA_LEGACY);

        this.invCollection = builder.build(this.database.getCollection("inv", PlayerInvBackup.class), PlayerInvBackup.class, UuidRepresentation.JAVA_LEGACY);

        this.cdkCollection = builder.build(this.database.getCollection("cdk", CDKData.class), CDKData.class, UuidRepresentation.JAVA_LEGACY);

        this.rewardCollection = builder.build(this.database.getCollection("reward", FixedRewardData.class), FixedRewardData.class, UuidRepresentation.JAVA_LEGACY);

        this.eventQueueCollection = builder.build(this.database.getCollection("event_queue", EventQueue.class), EventQueue.class, UuidRepresentation.JAVA_LEGACY);

        createIndex(mailCollection, "uuidIndex", "uuid");

        this.profiles = new cn.charlotte.pit.data.storage.DocumentCollection<PlayerProfile>(profileCollection, null, "players", "uuid", PlayerProfile.class);
        this.mails = new cn.charlotte.pit.data.storage.DocumentCollection<PlayerMailData>(mailCollection, null, "mail", "uuid", PlayerMailData.class);
        this.invBackups = new cn.charlotte.pit.data.storage.DocumentCollection<PlayerInvBackup>(this.invCollection, null, "inv", "backupUuid", PlayerInvBackup.class);
        this.rewards = new cn.charlotte.pit.data.storage.DocumentCollection<FixedRewardData>(rewardCollection, null, "reward", "mailId", FixedRewardData.class);
        this.eventQueue = new cn.charlotte.pit.data.storage.DocumentCollection<EventQueue>(eventQueueCollection, null, "event_queue", "id", EventQueue.class);
        this.cdks = new cn.charlotte.pit.data.storage.DocumentCollection<CDKData>(cdkCollection, null, "cdk", "cdk", CDKData.class);
        this.trades = new cn.charlotte.pit.data.storage.DocumentCollection<TradeData>(this.tradeCollection, null, "trade", "tradeUuid", TradeData.class);
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

    private void createIndex(MongoCollection<?> collection, String indexName, String fieldName) {
        try {
            IndexOptions indexOptions = new IndexOptions().name(indexName);
            Bson index = Indexes.ascending(fieldName);
            collection.createIndex(index, indexOptions);
        } catch (Exception ignore) {

        }
    }
}
