package com.oldphonedeals.repository;

import com.oldphonedeals.entity.ProcessedMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends MongoRepository<ProcessedMessage, String> {
  boolean existsByMessageId(String messageId);
}
