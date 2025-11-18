package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售统计响应 DTO
 * 用于返回订单销售统计数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesStatsResponse {

    /**
     * 总销售额
     */
    private BigDecimal totalSales;

    /**
     * 总交易数
     */
    private Long totalTransactions;
}