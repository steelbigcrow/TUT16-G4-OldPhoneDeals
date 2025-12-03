package com.oldphonedeals.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated user order response.
 * <p>
 * Wraps a page of OrderResponse items plus pagination metadata, matching the
 * user-orders OpenSpec (items + pagination).
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPageResponse {

  /**
   * Orders on the current page.
   */
  private List<OrderResponse> items;

  /**
   * Pagination metadata for the current query.
   */
  private Pagination pagination;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Pagination {

    /**
     * Current page number (1-based).
     */
    private int currentPage;

    /**
     * Page size (items per page).
     */
    private int pageSize;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Total number of items across all pages.
     */
    private long totalItems;
  }
}
