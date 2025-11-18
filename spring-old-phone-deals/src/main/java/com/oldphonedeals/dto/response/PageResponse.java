package com.oldphonedeals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应类
 * <p>
 * 该类用于封装分页查询的结果，提供标准化的分页信息。
 * 页码从 1 开始计数，与前端保持一致（Spring Data 内部从 0 开始）。
 * </p>
 *
 * @param <T> 分页内容的数据类型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {

  /**
   * 当前页的数据列表
   */
  private List<T> content;

  /**
   * 当前页码（从 1 开始）
   */
  private int currentPage;

  /**
   * 总页数
   */
  private int totalPages;

  /**
   * 总条目数
   */
  private long totalItems;

  /**
   * 每页条目数
   */
  private int itemsPerPage;

  /**
   * 是否有下一页
   */
  private boolean hasNext;

  /**
   * 是否有上一页
   */
  private boolean hasPrevious;

  /**
   * 从 Spring Data 的 Page 对象创建 PageResponse
   * <p>
   * 注意：Spring Data 的页码从 0 开始，这里转换为从 1 开始
   * </p>
   *
   * @param page Spring Data 的 Page 对象
   */
  public PageResponse(Page<T> page) {
    this.content = page.getContent();
    this.currentPage = page.getNumber() + 1; // 转换为从 1 开始
    this.totalPages = page.getTotalPages();
    this.totalItems = page.getTotalElements();
    this.itemsPerPage = page.getSize();
    this.hasNext = page.hasNext();
    this.hasPrevious = page.hasPrevious();
  }

  /**
   * 从 Spring Data 的 Page 对象创建 PageResponse，并对内容进行转换
   * <p>
   * 该方法允许将 Page 中的实体对象转换为 DTO 对象
   * </p>
   *
   * @param page Spring Data 的 Page 对象
   * @param mapper 实体到 DTO 的转换函数
   * @param <E> 实体类型
   */
  public <E> PageResponse(Page<E> page, Function<E, T> mapper) {
    this.content = page.getContent().stream()
        .map(mapper)
        .collect(Collectors.toList());
    this.currentPage = page.getNumber() + 1; // 转换为从 1 开始
    this.totalPages = page.getTotalPages();
    this.totalItems = page.getTotalElements();
    this.itemsPerPage = page.getSize();
    this.hasNext = page.hasNext();
    this.hasPrevious = page.hasPrevious();
  }

  /**
   * 使用 Spring Data 的 Page.map 方法创建 PageResponse
   * <p>
   * 这是推荐的创建方式，直接使用 Spring Data 的 map 功能进行转换
   * </p>
   *
   * @param mappedPage 已经通过 map 转换的 Page 对象
   * @param <E> 原始实体类型
   * @return PageResponse 实例
   */
  public static <E, T> PageResponse<T> of(Page<T> mappedPage) {
    return new PageResponse<>(mappedPage);
  }
}