package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserReviewResponse {
    private String reviewId;
    private String phoneId;
    private String phoneTitle;
    private String phoneBrand;
    private Double phonePrice;
    private Integer phoneStock;
    private Double averageRating;
    private Integer reviewsCount;
    private Integer reviewRating;
    private String reviewComment;
    private LocalDateTime reviewCreatedAt;
    private Boolean isHidden;
}
