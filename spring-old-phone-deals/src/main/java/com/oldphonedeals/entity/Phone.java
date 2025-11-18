package com.oldphonedeals.entity;

import com.oldphonedeals.enums.PhoneBrand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "phones")
public class Phone {
    
    @Id
    private String id;
    
    private String title;
    
    private PhoneBrand brand;
    
    private String image;
    
    private Integer stock;
    
    @DBRef
    private User seller;
    
    private Double price;
    
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
    
    @Builder.Default
    private Boolean isDisabled = false;
    
    @Builder.Default
    private Integer salesCount = 0;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Nested Review class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        
        private String id;
        
        private String reviewerId;
        
        private Integer rating;
        
        private String comment;
        
        @Builder.Default
        private Boolean isHidden = false;
        
        @CreatedDate
        private LocalDateTime createdAt;
    }
    
    // Calculate average rating
    public Double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .filter(r -> !r.getIsHidden())
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}