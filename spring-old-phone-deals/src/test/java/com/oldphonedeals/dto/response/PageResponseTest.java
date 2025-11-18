package com.oldphonedeals.dto.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PageResponse 测试
 */
@DisplayName("PageResponse Tests")
class PageResponseTest {

  @Test
  @DisplayName("应该从 Spring Data Page 创建 PageResponse")
  void shouldCreateFromSpringDataPage() {
    // Given
    List<String> content = Arrays.asList("item1", "item2", "item3");
    Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 23);

    // When
    PageResponse<String> response = new PageResponse<>(page);

    // Then
    assertNotNull(response);
    assertEquals(content, response.getContent());
    assertEquals(1, response.getCurrentPage()); // 页码从 1 开始
    assertEquals(3, response.getTotalPages());
    assertEquals(23, response.getTotalItems());
    assertEquals(10, response.getItemsPerPage());
    assertTrue(response.isHasNext());
    assertFalse(response.isHasPrevious());
  }

  @Test
  @DisplayName("应该正确转换页码 - 从0开始到从1开始")
  void shouldCorrectlyConvertPageNumber() {
    // Given - Spring Data 使用 0-based 页码
    Page<String> firstPage = new PageImpl<>(
        Arrays.asList("item1"),
        PageRequest.of(0, 10),
        25
    );
    Page<String> secondPage = new PageImpl<>(
        Arrays.asList("item2"),
        PageRequest.of(1, 10),
        25
    );

    // When
    PageResponse<String> firstResponse = new PageResponse<>(firstPage);
    PageResponse<String> secondResponse = new PageResponse<>(secondPage);

    // Then
    assertEquals(1, firstResponse.getCurrentPage()); // 转换为 1
    assertEquals(2, secondResponse.getCurrentPage()); // 转换为 2
  }

  @Test
  @DisplayName("应该正确设置 hasNext 和 hasPrevious 标志")
  void shouldCorrectlySetNavigationFlags() {
    // Given - 第一页
    Page<String> firstPage = new PageImpl<>(
        Arrays.asList("item1"),
        PageRequest.of(0, 10),
        25
    );

    // Given - 中间页
    Page<String> middlePage = new PageImpl<>(
        Arrays.asList("item2"),
        PageRequest.of(1, 10),
        25
    );

    // Given - 最后一页
    Page<String> lastPage = new PageImpl<>(
        Arrays.asList("item3"),
        PageRequest.of(2, 10),
        25
    );

    // When
    PageResponse<String> firstResponse = new PageResponse<>(firstPage);
    PageResponse<String> middleResponse = new PageResponse<>(middlePage);
    PageResponse<String> lastResponse = new PageResponse<>(lastPage);

    // Then - 第一页
    assertTrue(firstResponse.isHasNext());
    assertFalse(firstResponse.isHasPrevious());

    // Then - 中间页
    assertTrue(middleResponse.isHasNext());
    assertTrue(middleResponse.isHasPrevious());

    // Then - 最后一页
    assertFalse(lastResponse.isHasNext());
    assertTrue(lastResponse.isHasPrevious());
  }

  @Test
  @DisplayName("应该使用 mapper 转换内容类型")
  void shouldTransformContentWithMapper() {
    // Given
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    Page<Integer> page = new PageImpl<>(numbers, PageRequest.of(0, 10), 5);

    // When - 将 Integer 转换为 String
    PageResponse<String> response = new PageResponse<>(page, num -> "Number: " + num);

    // Then
    assertNotNull(response);
    assertEquals(5, response.getContent().size());
    assertEquals("Number: 1", response.getContent().get(0));
    assertEquals("Number: 5", response.getContent().get(4));
    assertEquals(1, response.getCurrentPage());
    assertEquals(1, response.getTotalPages());
    assertEquals(5, response.getTotalItems());
  }

  @Test
  @DisplayName("应该使用静态工厂方法创建")
  void shouldCreateUsingStaticFactoryMethod() {
    // Given
    List<String> content = Arrays.asList("item1", "item2");
    Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);

    // When
    PageResponse<String> response = PageResponse.of(page);

    // Then
    assertNotNull(response);
    assertEquals(content, response.getContent());
    assertEquals(1, response.getCurrentPage());
    assertEquals(1, response.getTotalPages());
    assertEquals(2, response.getTotalItems());
  }

  @Test
  @DisplayName("应该处理空页面")
  void shouldHandleEmptyPage() {
    // Given
    Page<String> emptyPage = new PageImpl<>(
        Arrays.asList(),
        PageRequest.of(0, 10),
        0
    );

    // When
    PageResponse<String> response = new PageResponse<>(emptyPage);

    // Then
    assertNotNull(response);
    assertTrue(response.getContent().isEmpty());
    assertEquals(1, response.getCurrentPage());
    assertEquals(0, response.getTotalPages());
    assertEquals(0, response.getTotalItems());
    assertFalse(response.isHasNext());
    assertFalse(response.isHasPrevious());
  }

  @Test
  @DisplayName("应该处理单页结果")
  void shouldHandleSinglePageResult() {
    // Given
    List<String> content = Arrays.asList("item1", "item2", "item3");
    Page<String> singlePage = new PageImpl<>(content, PageRequest.of(0, 10), 3);

    // When
    PageResponse<String> response = new PageResponse<>(singlePage);

    // Then
    assertNotNull(response);
    assertEquals(3, response.getContent().size());
    assertEquals(1, response.getCurrentPage());
    assertEquals(1, response.getTotalPages());
    assertEquals(3, response.getTotalItems());
    assertFalse(response.isHasNext());
    assertFalse(response.isHasPrevious());
  }

  @Test
  @DisplayName("应该使用 Builder 创建")
  void shouldCreateUsingBuilder() {
    // Given & When
    PageResponse<String> response = PageResponse.<String>builder()
        .content(Arrays.asList("item1", "item2"))
        .currentPage(1)
        .totalPages(5)
        .totalItems(50)
        .itemsPerPage(10)
        .hasNext(true)
        .hasPrevious(false)
        .build();

    // Then
    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertEquals(1, response.getCurrentPage());
    assertEquals(5, response.getTotalPages());
    assertEquals(50, response.getTotalItems());
    assertEquals(10, response.getItemsPerPage());
    assertTrue(response.isHasNext());
    assertFalse(response.isHasPrevious());
  }

  @Test
  @DisplayName("应该计算正确的总页数")
  void shouldCalculateCorrectTotalPages() {
    // Given - 25 items, 10 per page = 3 pages
    Page<String> page = new PageImpl<>(
        Arrays.asList("item"),
        PageRequest.of(0, 10),
        25
    );

    // When
    PageResponse<String> response = new PageResponse<>(page);

    // Then
    assertEquals(3, response.getTotalPages());
  }

  @Test
  @DisplayName("应该使用无参构造器创建")
  void shouldCreateWithNoArgsConstructor() {
    // Given & When
    PageResponse<String> response = new PageResponse<>();

    // Then
    assertNotNull(response);
    assertNull(response.getContent());
    assertEquals(0, response.getCurrentPage());
    assertEquals(0, response.getTotalPages());
    assertEquals(0, response.getTotalItems());
  }
}