package com.oldphonedeals.entity;

import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "adminlogs")
public class AdminLog {
    
    @Id
    private String id;
    
    private String adminUserId;
    
    private AdminAction action;
    
    private TargetType targetType;
    
    private String targetId;
    
    private String details;
    
    @CreatedDate
    private LocalDateTime createdAt;
}