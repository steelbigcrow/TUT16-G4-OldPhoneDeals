package com.oldphonedeals.integration.mongo;

import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.repository.PhoneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.*;

class PhoneOptimisticLockIT extends AbstractMongoIT {

  @Autowired
  private PhoneRepository phoneRepository;

  @Test
  void savingStaleVersionThrowsOptimisticLockingFailureException() {
    Phone phone = Phone.builder()
        .title("Optimistic lock test phone")
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

    p1.setPrice(101.0);
    Phone updated = phoneRepository.save(p1);
    assertNotNull(updated.getVersion());

    p2.setPrice(102.0);
    assertThrows(OptimisticLockingFailureException.class, () -> phoneRepository.save(p2));
  }
}

