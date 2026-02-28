package com.oldphonedeals.repository.custom.impl;

import com.mongodb.client.result.UpdateResult;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class PhoneStockRepositoryImpl implements PhoneStockRepository {

  private final MongoTemplate mongoTemplate;

  @Override
  public boolean decreaseStockAndIncreaseSales(String phoneId, int quantity) {
    Query query = Query.query(
      new Criteria().andOperator(
        Criteria.where("_id").is(phoneId),
        Criteria.where("stock").gte(quantity),
        Criteria.where("isDisabled").is(false)
      )
    );

    Update update = new Update()
      .inc("stock", -quantity)
      .inc("salesCount", quantity)
      .inc("version", 1)
      .set("updatedAt", LocalDateTime.now());

    UpdateResult result = mongoTemplate.updateFirst(query, update, Phone.class);
    return result.getModifiedCount() > 0;
  }
}
