package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneReviewListResponse {
    private boolean success;
    private String message;
    private long total;
    private int page;
    private int limit;
    private List<ReviewManagementResponse> reviews;

    public static PhoneReviewListResponse success(long total, int page, int limit, List<ReviewManagementResponse> reviews) {
        return PhoneReviewListResponse.builder()
                .success(true)
                .total(total)
                .page(page)
                .limit(limit)
                .reviews(reviews != null ? reviews : Collections.emptyList())
                .build();
    }

    public static PhoneReviewListResponse error(String message) {
        return PhoneReviewListResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
