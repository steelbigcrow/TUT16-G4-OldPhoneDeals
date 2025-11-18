package com.oldphonedeals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Old Phone Deals 应用程序主类
 *
 * 功能特性：
 * - MongoDB审计支持（自动填充createdAt和updatedAt字段）
 * - 异步任务支持（在AsyncConfig中启用）
 * - JWT认证和授权
 * - 文件上传功能
 * - 邮件发送服务
 */
@SpringBootApplication
@EnableMongoAuditing
public class OldPhoneDealsApplication {
  
  public static void main(String[] args) {
    SpringApplication.run(OldPhoneDealsApplication.class, args);
  }
}