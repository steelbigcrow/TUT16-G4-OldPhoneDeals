package com.oldphonedeals.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    
    @Id
    private String id;
    
    private String userId;
    
    private List<OrderItem> items;
    
    private Double totalAmount;
    
    private Address address;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    // Nested OrderItem class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        
        private String phoneId;
        
        private String title;
        
        private Integer quantity;
        
        private Double price;
    }
    
    // Nested Address class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        
        private String street;
        
        private String city;
        
        private String state;
        
        private String zip;
        
        private String country;
    }
}