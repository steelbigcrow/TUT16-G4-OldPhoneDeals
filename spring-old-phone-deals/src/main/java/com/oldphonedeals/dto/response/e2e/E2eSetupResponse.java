package com.oldphonedeals.dto.response.e2e;

import com.oldphonedeals.enums.PhoneBrand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * E2E 测试初始化响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class E2eSetupResponse {

    private TestUserInfo buyer;
    private TestUserInfo seller;
    private List<TestPhoneInfo> phones;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestUserInfo {
        private String id;
        private String email;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestPhoneInfo {
        private String id;
        private String title;
        private PhoneBrand brand;
        private Double price;
        private Integer stock;
    }
}
