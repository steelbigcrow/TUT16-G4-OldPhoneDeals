package com.oldphonedeals.controller;

import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.e2e.E2eSetupResponse;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 提供 E2E 测试使用的数据库重置端点，仅在 app.e2e.enabled=true 时启用
 */
@RestController
@RequestMapping("/api/e2e")
@ConditionalOnProperty(name = "app.e2e.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class E2eTestController {

    private static final String DEFAULT_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final PhoneRepository phoneRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<E2eSetupResponse>> reset() {
        log.info("Resetting Mongo collections for E2E tests");

        // 清空数据
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        phoneRepository.deleteAll();
        userRepository.deleteAll();

        // 创建基础用户和商品
        LocalDateTime now = LocalDateTime.now();

        User buyer = userRepository.save(
                User.builder()
                        .firstName("E2E")
                        .lastName("Buyer")
                        .email("e2e-buyer@example.com")
                        .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                        .wishlist(new ArrayList<>())
                        .isAdmin(false)
                        .role("USER")
                        .isDisabled(false)
                        .isBan(false)
                        .isVerified(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        User seller = userRepository.save(
                User.builder()
                        .firstName("E2E")
                        .lastName("Seller")
                        .email("e2e-seller@example.com")
                        .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                        .wishlist(new ArrayList<>())
                        .isAdmin(false)
                        .role("USER")
                        .isDisabled(false)
                        .isBan(false)
                        .isVerified(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        List<Phone.Review> seedReviews = List.of(
                Phone.Review.builder()
                        .id(UUID.randomUUID().toString())
                        .reviewerId(buyer.getId())
                        .rating(5)
                        .comment("Love how smooth this phone feels.")
                        .isHidden(false)
                        .createdAt(now.minusDays(1))
                        .build(),
                Phone.Review.builder()
                        .id(UUID.randomUUID().toString())
                        .reviewerId(seller.getId())
                        .rating(4)
                        .comment("Surprisingly solid battery life.")
                        .isHidden(false)
                        .createdAt(now.minusHours(12))
                        .build()
        );

        Phone phone = Phone.builder()
                .title("E2E Apple iPhone 15 Pro")
                .brand(PhoneBrand.APPLE)
                .image("https://images.unsplash.com/photo-1512499617640-c2f999098c01?auto=format&fit=crop&w=1200&q=80")
                .stock(3)
                .price(1199.0)
                .seller(seller)
                .reviews(seedReviews)
                .isDisabled(false)
                .salesCount(42)
                .createdAt(now)
                .updatedAt(now)
                .build();
        phone = phoneRepository.save(phone);

        E2eSetupResponse response = E2eSetupResponse.builder()
                .buyer(E2eSetupResponse.TestUserInfo.builder()
                        .id(buyer.getId())
                        .email(buyer.getEmail())
                        .password(DEFAULT_PASSWORD)
                        .build())
                .seller(E2eSetupResponse.TestUserInfo.builder()
                        .id(seller.getId())
                        .email(seller.getEmail())
                        .password(DEFAULT_PASSWORD)
                        .build())
                .phones(List.of(
                        E2eSetupResponse.TestPhoneInfo.builder()
                                .id(phone.getId())
                                .title(phone.getTitle())
                                .brand(phone.getBrand())
                                .price(phone.getPrice())
                                .stock(phone.getStock())
                                .build()
                ))
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(response, "E2E dataset prepared")
        );
    }
}
