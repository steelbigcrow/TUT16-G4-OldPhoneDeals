package com.oldphonedeals.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    private String firstName;
    
    private String lastName;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    @Builder.Default
    private List<String> wishlist = new ArrayList<>();
    
    @Builder.Default
    private Boolean isAdmin = false;
    
    @Indexed
    @Builder.Default
    private String role = "USER";
    
    @Builder.Default
    private Boolean isDisabled = false;
    
    @Builder.Default
    private Boolean isBan = false;
    
    @Builder.Default
    private Boolean isVerified = false;
    
    private String verifyToken;
    
    private String passwordResetCode;
    
    private LocalDateTime passwordResetExpires;
    
    private LocalDateTime lastLogin;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}