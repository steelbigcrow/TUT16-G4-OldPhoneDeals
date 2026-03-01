package com.oldphonedeals.repository;

import com.oldphonedeals.entity.SagaLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SagaLogRepository extends MongoRepository<SagaLog, String> {
  Optional<SagaLog> findBySagaId(String sagaId);
}
