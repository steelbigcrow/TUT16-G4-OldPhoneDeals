package com.oldphonedeals.integration.mongo;

import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import com.oldphonedeals.repository.custom.impl.PhoneStockRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.*;

@Import(PhoneStockRepositoryImpl.class)
class PhoneStockOptimisticLockIT extends AbstractMongoIT {

  @Autowired
  private PhoneRepository phoneRepository;

  @Autowired
  private PhoneStockRepository phoneStockRepository;

  @Test
  void decreasingStockBumpsVersionAndPreventsStaleSave() {
    Phone phone = Phone.builder()
        .title("Stock optimistic lock test phone")
        .brand(PhoneBrand.APPLE)
        .stock(10)
        .price(100.0)
        .build();

    Phone saved = phoneRepository.save(phone);
    assertNotNull(saved.getId());
    assertNotNull(saved.getVersion());

    Phone p1 = phoneRepository.findById(saved.getId()).orElseThrow();
    Phone p2 = phoneRepository.findById(saved.getId()).orElseThrow();
    assertEquals(p1.getVersion(), p2.getVersion());
    Long originalVersion = p1.getVersion();

    boolean stockUpdated = phoneStockRepository.decreaseStockAndIncreaseSales(saved.getId(), 1);
    assertTrue(stockUpdated);

    Phone afterStockUpdate = phoneRepository.findById(saved.getId()).orElseThrow();
    assertEquals(9, afterStockUpdate.getStock());
    assertNotEquals(originalVersion, afterStockUpdate.getVersion());

    p2.setPrice(102.0);
    assertThrows(OptimisticLockingFailureException.class, () -> phoneRepository.save(p2));
  }
}

