package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderExportResponse {

    private String timestamp;
    private String buyerName;
    private String buyerEmail;
    private List<OrderExportItem> items;
    private Double totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderExportItem {
        private String title;
        private Integer quantity;
    }
}
