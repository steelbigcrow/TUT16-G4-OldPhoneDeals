package com.oldphonedeals.repository;

import com.oldphonedeals.TestDataFactory;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.PhoneBrand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PhoneRepository 集成测试
 * <p>
 * 使用 @DataMongoTest 进行轻量级的 MongoDB 集成测试
 * </p>
 */
@DataMongoTest
@ActiveProfiles("test")
@DisplayName("PhoneRepository Integration Tests")
class PhoneRepositoryTest {

  @Autowired
  private PhoneRepository phoneRepository;

  @Autowired
  private UserRepository userRepository;

  private User testSeller;

  @BeforeEach
  void setUp() {
    // 创建测试卖家
    testSeller = TestDataFactory.createDefaultUser();
    testSeller.setId(null);
    testSeller = userRepository.save(testSeller);
  }

  @AfterEach
  void cleanup() {
    phoneRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("应该保存并找到手机 - 通过ID")
  void shouldSaveAndFindPhone_byId() {
    // Given
    Phone phone = createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10);

    // When
    Phone savedPhone = phoneRepository.save(phone);
    Optional<Phone> foundPhone = phoneRepository.findById(savedPhone.getId());

    // Then
    assertNotNull(savedPhone.getId());
    assertTrue(foundPhone.isPresent());
    assertEquals(savedPhone.getId(), foundPhone.get().getId());
    assertEquals("iPhone 13", foundPhone.get().getTitle());
    assertEquals(PhoneBrand.APPLE, foundPhone.get().getBrand());
  }

  @Test
  @DisplayName("应该通过卖家ID查找手机 - 返回列表")
  void shouldFindPhonesBySellerId_returnsList() {
    // Given
    Phone phone1 = createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10);
    Phone phone2 = createTestPhone("iPhone 14", PhoneBrand.APPLE, 899.99, 5);
    Phone phone3 = createTestPhone("Galaxy S21", PhoneBrand.SAMSUNG, 699.99, 8);

    User anotherSeller = TestDataFactory.createUser("seller2", "seller2@test.com", "Jane", "Smith", false);
    anotherSeller.setId(null);
    anotherSeller = userRepository.save(anotherSeller);
    phone3.setSeller(anotherSeller);

    phoneRepository.save(phone1);
    phoneRepository.save(phone2);
    phoneRepository.save(phone3);

    // When
    List<Phone> sellerPhones = phoneRepository.findBySellerId(testSeller.getId());

    // Then
    assertEquals(2, sellerPhones.size());
    assertTrue(sellerPhones.stream().allMatch(p -> p.getSeller().getId().equals(testSeller.getId())));
  }

  @Test
  @DisplayName("应该通过卖家ID查找手机 - 返回分页")
  void shouldFindPhonesBySellerId_returnsPage() {
    // Given
    for (int i = 0; i < 5; i++) {
      Phone phone = createTestPhone("Phone " + i, PhoneBrand.APPLE, 500.0 + i * 100, 10);
      phoneRepository.save(phone);
    }

    Pageable pageable = PageRequest.of(0, 3);

    // When
    Page<Phone> phonePage = phoneRepository.findBySellerId(testSeller.getId(), pageable);

    // Then
    assertEquals(3, phonePage.getContent().size());
    assertEquals(5, phonePage.getTotalElements());
    assertEquals(2, phonePage.getTotalPages());
  }

  @Test
  @DisplayName("应该通过品牌查找手机")
  void shouldFindPhonesByBrand() {
    // Given
    phoneRepository.save(createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10));
    phoneRepository.save(createTestPhone("iPhone 14", PhoneBrand.APPLE, 899.99, 5));
    phoneRepository.save(createTestPhone("Galaxy S21", PhoneBrand.SAMSUNG, 699.99, 8));

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Phone> applePhones = phoneRepository.findByBrand(PhoneBrand.APPLE, pageable);

    // Then
    assertEquals(2, applePhones.getContent().size());
    assertTrue(applePhones.getContent().stream()
        .allMatch(p -> p.getBrand() == PhoneBrand.APPLE));
  }

  @Test
  @DisplayName("应该查找未禁用的手机")
  void shouldFindNotDisabledPhones() {
    // Given
    Phone phone1 = createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10);
    Phone phone2 = createTestPhone("iPhone 14", PhoneBrand.APPLE, 899.99, 5);
    Phone phone3 = createTestPhone("Galaxy S21", PhoneBrand.SAMSUNG, 699.99, 8);
    phone3.setIsDisabled(true);

    phoneRepository.save(phone1);
    phoneRepository.save(phone2);
    phoneRepository.save(phone3);

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Phone> activePhones = phoneRepository.findByIsDisabledFalse(pageable);

    // Then
    assertEquals(2, activePhones.getContent().size());
    assertTrue(activePhones.getContent().stream()
        .allMatch(p -> !p.getIsDisabled()));
  }

  @Test
  @DisplayName("应该通过品牌和未禁用状态查找手机")
  void shouldFindPhonesByBrandAndNotDisabled() {
    // Given
    Phone phone1 = createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10);
    Phone phone2 = createTestPhone("iPhone 14", PhoneBrand.APPLE, 899.99, 5);
    phone2.setIsDisabled(true);
    Phone phone3 = createTestPhone("Galaxy S21", PhoneBrand.SAMSUNG, 699.99, 8);

    phoneRepository.save(phone1);
    phoneRepository.save(phone2);
    phoneRepository.save(phone3);

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Phone> applePhones = phoneRepository.findByBrandAndIsDisabledFalse(PhoneBrand.APPLE, pageable);

    // Then
    assertEquals(1, applePhones.getContent().size());
    assertEquals("iPhone 13", applePhones.getContent().get(0).getTitle());
    assertEquals(PhoneBrand.APPLE, applePhones.getContent().get(0).getBrand());
    assertFalse(applePhones.getContent().get(0).getIsDisabled());
  }

  @Test
  @DisplayName("应该通过标题搜索手机 - 不区分大小写")
  void shouldSearchPhonesByTitle_caseInsensitive() {
    // Given
    phoneRepository.save(createTestPhone("iPhone 13 Pro", PhoneBrand.APPLE, 999.99, 10));
    phoneRepository.save(createTestPhone("iPhone 14", PhoneBrand.APPLE, 899.99, 5));
    phoneRepository.save(createTestPhone("Galaxy S21", PhoneBrand.SAMSUNG, 699.99, 8));

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Phone> searchResults = phoneRepository.searchByTitle("iphone", pageable);

    // Then
    assertEquals(2, searchResults.getContent().size());
    assertTrue(searchResults.getContent().stream()
        .allMatch(p -> p.getTitle().toLowerCase().contains("iphone")));
  }

  @Test
  @DisplayName("应该通过标题搜索手机 - 仅返回未禁用的商品")
  void shouldSearchPhonesByTitle_onlyActivePhones() {
    // Given
    Phone phone1 = createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10);
    Phone phone2 = createTestPhone("iPhone 14", PhoneBrand.APPLE, 899.99, 5);
    phone2.setIsDisabled(true);

    phoneRepository.save(phone1);
    phoneRepository.save(phone2);

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Phone> searchResults = phoneRepository.searchByTitle("iphone", pageable);

    // Then
    assertEquals(1, searchResults.getContent().size());
    assertEquals("iPhone 13", searchResults.getContent().get(0).getTitle());
  }

  @Test
  @DisplayName("应该通过价格范围查找手机")
  void shouldFindPhonesByPriceRange() {
    // Given
    phoneRepository.save(createTestPhone("Budget Phone", PhoneBrand.NOKIA, 199.99, 10));
    phoneRepository.save(createTestPhone("Mid Phone", PhoneBrand.SAMSUNG, 499.99, 5));
    phoneRepository.save(createTestPhone("Premium Phone", PhoneBrand.APPLE, 999.99, 8));

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Phone> midRangePhones = phoneRepository.findByPriceRange(400.0, 600.0, pageable);

    // Then
    assertEquals(1, midRangePhones.getContent().size());
    assertEquals("Mid Phone", midRangePhones.getContent().get(0).getTitle());
    assertTrue(midRangePhones.getContent().get(0).getPrice() >= 400.0);
    assertTrue(midRangePhones.getContent().get(0).getPrice() <= 600.0);
  }

  @Test
  @DisplayName("应该查找库存最少的5个手机")
  void shouldFindTop5PhonesByLowestStock() {
    // Given
    phoneRepository.save(createTestPhone("Phone 1", PhoneBrand.APPLE, 500.0, 5));
    phoneRepository.save(createTestPhone("Phone 2", PhoneBrand.SAMSUNG, 500.0, 2));
    phoneRepository.save(createTestPhone("Phone 3", PhoneBrand.NOKIA, 500.0, 8));
    phoneRepository.save(createTestPhone("Phone 4", PhoneBrand.HUAWEI, 500.0, 1));
    phoneRepository.save(createTestPhone("Phone 5", PhoneBrand.LG, 500.0, 10));
    phoneRepository.save(createTestPhone("Phone 6", PhoneBrand.APPLE, 500.0, 3));

    // When
    List<Phone> lowStockPhones = phoneRepository.findTop5ByIsDisabledFalseOrderByStockAsc();

    // Then
    assertEquals(5, lowStockPhones.size());
    assertEquals(1, lowStockPhones.get(0).getStock()); // Phone 4
    assertEquals(2, lowStockPhones.get(1).getStock()); // Phone 2
    assertEquals(3, lowStockPhones.get(2).getStock()); // Phone 6
  }

  @Test
  @DisplayName("应该查找库存低于指定值的手机")
  void shouldFindPhonesByStockLessThanEqual() {
    // Given
    phoneRepository.save(createTestPhone("Phone 1", PhoneBrand.APPLE, 500.0, 2));
    phoneRepository.save(createTestPhone("Phone 2", PhoneBrand.SAMSUNG, 500.0, 5));
    phoneRepository.save(createTestPhone("Phone 3", PhoneBrand.NOKIA, 500.0, 3));
    phoneRepository.save(createTestPhone("Phone 4", PhoneBrand.HUAWEI, 500.0, 10));

    Pageable pageable = PageRequest.of(0, 10);

    // When
    List<Phone> lowStockPhones = phoneRepository
        .findByStockLessThanEqualAndIsDisabledFalseOrderByStockAsc(5, pageable);

    // Then
    assertEquals(3, lowStockPhones.size());
    assertEquals(2, lowStockPhones.get(0).getStock());
    assertEquals(3, lowStockPhones.get(1).getStock());
    assertEquals(5, lowStockPhones.get(2).getStock());
  }

  @Test
  @DisplayName("应该删除卖家的所有手机")
  void shouldDeletePhonesBySellerId() {
    // Given
    phoneRepository.save(createTestPhone("Phone 1", PhoneBrand.APPLE, 500.0, 10));
    phoneRepository.save(createTestPhone("Phone 2", PhoneBrand.SAMSUNG, 600.0, 5));

    User anotherSeller = TestDataFactory.createUser("seller2", "seller2@test.com", "Jane", "Smith", false);
    anotherSeller.setId(null);
    anotherSeller = userRepository.save(anotherSeller);
    Phone phone3 = createTestPhone("Phone 3", PhoneBrand.NOKIA, 400.0, 8);
    phone3.setSeller(anotherSeller);
    phoneRepository.save(phone3);

    // When
    phoneRepository.deleteBySellerId(testSeller.getId());

    // Then
    List<Phone> remainingPhones = phoneRepository.findAll();
    assertEquals(1, remainingPhones.size());
    assertEquals(anotherSeller.getId(), remainingPhones.get(0).getSeller().getId());
  }

  @Test
  @DisplayName("应该返回空结果 - 当卖家没有手机时")
  void shouldReturnEmpty_whenSellerHasNoPhones() {
    // Given
    String nonExistentSellerId = "non-existent-seller-id";

    // When
    List<Phone> phones = phoneRepository.findBySellerId(nonExistentSellerId);

    // Then
    assertTrue(phones.isEmpty());
  }

  @Test
  @DisplayName("应该返回空结果 - 当品牌没有手机时")
  void shouldReturnEmpty_whenBrandHasNoPhones() {
    // Given
    phoneRepository.save(createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10));

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<Phone> nokiaPhones = phoneRepository.findByBrand(PhoneBrand.NOKIA, pageable);

    // Then
    assertTrue(nokiaPhones.getContent().isEmpty());
  }

  @Test
  @DisplayName("应该正确处理分页参数")
  void shouldHandlePaginationCorrectly() {
    // Given
    for (int i = 0; i < 10; i++) {
      phoneRepository.save(createTestPhone("Phone " + i, PhoneBrand.APPLE, 500.0 + i * 10, 10));
    }

    // When
    Page<Phone> page1 = phoneRepository.findBySellerId(testSeller.getId(), PageRequest.of(0, 3));
    Page<Phone> page2 = phoneRepository.findBySellerId(testSeller.getId(), PageRequest.of(1, 3));

    // Then
    assertEquals(3, page1.getContent().size());
    assertEquals(3, page2.getContent().size());
    assertEquals(10, page1.getTotalElements());
    assertEquals(4, page1.getTotalPages());
    assertNotEquals(page1.getContent().get(0).getId(), page2.getContent().get(0).getId());
  }

  @Test
  @DisplayName("应该正确处理排序")
  void shouldHandleSortingCorrectly() {
    // Given
    phoneRepository.save(createTestPhone("Phone C", PhoneBrand.APPLE, 500.0, 10));
    phoneRepository.save(createTestPhone("Phone A", PhoneBrand.SAMSUNG, 600.0, 5));
    phoneRepository.save(createTestPhone("Phone B", PhoneBrand.NOKIA, 400.0, 8));

    Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());

    // When
    Page<Phone> sortedPhones = phoneRepository.findByIsDisabledFalse(pageable);

    // Then
    assertEquals(3, sortedPhones.getContent().size());
    assertEquals("Phone A", sortedPhones.getContent().get(0).getTitle());
    assertEquals("Phone B", sortedPhones.getContent().get(1).getTitle());
    assertEquals("Phone C", sortedPhones.getContent().get(2).getTitle());
  }

  @Test
  @DisplayName("应该更新手机信息")
  void shouldUpdatePhoneInformation() {
    // Given
    Phone phone = createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10);
    Phone savedPhone = phoneRepository.save(phone);

    // When
    savedPhone.setTitle("iPhone 13 Pro");
    savedPhone.setPrice(999.99);
    savedPhone.setStock(5);
    Phone updatedPhone = phoneRepository.save(savedPhone);

    // Then
    Optional<Phone> foundPhone = phoneRepository.findById(updatedPhone.getId());
    assertTrue(foundPhone.isPresent());
    assertEquals("iPhone 13 Pro", foundPhone.get().getTitle());
    assertEquals(999.99, foundPhone.get().getPrice());
    assertEquals(5, foundPhone.get().getStock());
  }

  @Test
  @DisplayName("应该删除手机")
  void shouldDeletePhone() {
    // Given
    Phone phone = createTestPhone("iPhone 13", PhoneBrand.APPLE, 799.99, 10);
    Phone savedPhone = phoneRepository.save(phone);

    // When
    phoneRepository.deleteById(savedPhone.getId());

    // Then
    Optional<Phone> foundPhone = phoneRepository.findById(savedPhone.getId());
    assertFalse(foundPhone.isPresent());
  }

  @Test
  @DisplayName("应该计数所有手机")
  void shouldCountAllPhones() {
    // Given
    phoneRepository.save(createTestPhone("Phone 1", PhoneBrand.APPLE, 500.0, 10));
    phoneRepository.save(createTestPhone("Phone 2", PhoneBrand.SAMSUNG, 600.0, 5));
    phoneRepository.save(createTestPhone("Phone 3", PhoneBrand.NOKIA, 400.0, 8));

    // When
    long count = phoneRepository.count();

    // Then
    assertEquals(3, count);
  }

  // Helper method to create test phone
  private Phone createTestPhone(String title, PhoneBrand brand, double price, int stock) {
    return Phone.builder()
        .title(title)
        .brand(brand)
        .price(price)
        .stock(stock)
        .image("/images/test.jpg")
        .seller(testSeller)
        .isDisabled(false)
        .salesCount(0)
        .build();
  }
}