package com.oldphonedeals.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * MongoDB配置类
 * 配置MongoTemplate、事务管理器和审计支持
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

  @Value("${spring.data.mongodb.uri}")
  private String mongoUri;

  @Value("${app.mongo.transactions.enabled:false}")
  private boolean mongoTransactionsEnabled;

  @Override
  protected String getDatabaseName() {
    // 从URI中提取数据库名
    ConnectionString connectionString = new ConnectionString(mongoUri);
    return connectionString.getDatabase() != null ? 
           connectionString.getDatabase() : "oldphonedeals";
  }

  @Override
  public MongoClient mongoClient() {
    ConnectionString connectionString = new ConnectionString(mongoUri);
    MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .build();
    return MongoClients.create(mongoClientSettings);
  }

  /**
   * 配置MongoTemplate
   * 用于执行复杂的MongoDB操作
   */
  @Bean
  public MongoTemplate mongoTemplate(MongoDatabaseFactory databaseFactory, 
                                      MappingMongoConverter converter) {
    return new MongoTemplate(databaseFactory, converter);
  }

  /**
   * 配置MongoDB类型映射器
   * 移除默认的_class字段，使文档更简洁
   */
  @Bean
  public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory databaseFactory,
                                                      MongoMappingContext context) {
    DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(databaseFactory);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);
    
    // 移除_class字段
    converter.setTypeMapper(new DefaultMongoTypeMapper(null));
    
    return converter;
  }

  /**
   * 配置MongoDB事务管理器
   * MongoDB事务需要副本集支持；对本地单机 MongoDB（非副本集）默认回退为无资源事务管理器，
   * 以避免 "Transaction numbers are only allowed on a replica set member or mongos" 错误。
   */
  @Bean
  public PlatformTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
    if (mongoTransactionsEnabled) {
      return new MongoTransactionManager(databaseFactory);
    }

    return new NoOpTransactionManager();
  }

  /**
   * 一个“空实现”的事务管理器，用于在单机 MongoDB（非副本集）环境下兼容 @Transactional。
   *
   * 注意：这不会提供真正的 MongoDB 事务能力（原子性/回滚），仅用于避免在不支持事务的环境中报错。
   */
  private static class NoOpTransactionManager implements PlatformTransactionManager {
    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
      // no-op
    }

    @Override
    public void rollback(TransactionStatus status) {
      // no-op
    }
  }
}
