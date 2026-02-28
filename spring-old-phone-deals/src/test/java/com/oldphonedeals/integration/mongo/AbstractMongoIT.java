package com.oldphonedeals.integration.mongo;

import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DataMongoTest
abstract class AbstractMongoIT {

  @Container
  static final MongoDBContainer MONGO = new MongoDBContainer(
      DockerImageName.parse("mongo:7.0.5")
  );

  @DynamicPropertySource
  static void registerMongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
  }
}

