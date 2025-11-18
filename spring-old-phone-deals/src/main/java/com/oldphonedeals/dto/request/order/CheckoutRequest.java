package com.oldphonedeals.dto.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结账请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    
    /**
     * 收货地址
     */
    @NotNull(message = "Address is required")
    @Valid
    private AddressInfo address;
    
    /**
     * 地址信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressInfo {
        
        /**
         * 街道地址
         */
        @NotBlank(message = "Street is required")
        private String street;
        
        /**
         * 城市
         */
        @NotBlank(message = "City is required")
        private String city;
        
        /**
         * 州/省
         */
        @NotBlank(message = "State is required")
        private String state;
        
        /**
         * 邮编
         */
        @NotBlank(message = "Zip code is required")
        private String zip;
        
        /**
         * 国家
         */
        @NotBlank(message = "Country is required")
        private String country;
    }
}