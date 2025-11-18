package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.phone.ReviewCreateRequest;
import com.oldphonedeals.dto.response.phone.ReviewResponse;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.service.ReviewService;
import com.oldphonedeals.service.impl.ReviewServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReviewService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    private ReviewService reviewService;

    private Phone phone;
    private User user;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl();
        // 通过反射注入 @Autowired 字段，避免修改生产代码
        ReflectionTestUtils.setField(reviewService, "phoneRepository", phoneRepository);
        ReflectionTestUtils.setField(reviewService, "userRepository", userRepository);
        ReflectionTestUtils.setField(reviewService, "orderRepository", orderRepository);

        user = User.builder()
            .id("user-1")
            .firstName("John")
            .lastName("Doe")
            .build();

        phone = Phone.builder()
            .id("phone-1")
            .seller(User.builder().id("seller-1").firstName("Alice").lastName("Smith").build())
            .isDisabled(false)
            .reviews(new ArrayList<>())
            .build();
    }

    @Test
    void addReview_shouldCreateReview_whenValid() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setComment("Great phone");

        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phone));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(phoneRepository.save(any(Phone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.addReview("phone-1", request, "user-1");

        assertNotNull(response.getId());
        assertEquals("user-1", response.getReviewerId());
        assertEquals("Great phone", response.getComment());
        assertFalse(response.getIsHidden());
        assertEquals("John Doe", response.getReviewer());
        verify(phoneRepository).save(phone);
    }

    @Test
    void addReview_shouldThrow_whenPhoneNotFoundOrDisabledOrSelfReviewOrDuplicate() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setComment("Great phone");

        when(phoneRepository.findById("phone-1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.addReview("phone-1", request, "user-1"));

        // phone disabled
        Phone disabledPhone = Phone.builder().id("phone-1").isDisabled(true).build();
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(disabledPhone));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        assertThrows(BadRequestException.class,
            () -> reviewService.addReview("phone-1", request, "user-1"));

        // self review
        disabledPhone.setIsDisabled(false);
        disabledPhone.setSeller(User.builder().id("user-1").build());
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(disabledPhone));
        assertThrows(BadRequestException.class,
            () -> reviewService.addReview("phone-1", request, "user-1"));

        // duplicate review
        Phone phoneWithReview = Phone.builder()
            .id("phone-1")
            .seller(User.builder().id("seller-1").build())
            .isDisabled(false)
            .reviews(new ArrayList<>())
            .build();
        Phone.Review existing = Phone.Review.builder()
            .id("r1")
            .reviewerId("user-1")
            .rating(4)
            .comment("ok")
            .createdAt(LocalDateTime.now())
            .isHidden(false)
            .build();
        phoneWithReview.getReviews().add(existing);
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phoneWithReview));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        assertThrows(BadRequestException.class,
            () -> reviewService.addReview("phone-1", request, "user-1"));
    }

    @Test
    void toggleReviewVisibility_shouldUpdateVisibility_whenAuthorized() {
        Phone.Review review = Phone.Review.builder()
            .id("r1")
            .reviewerId("user-1")
            .rating(5)
            .comment("Nice")
            .createdAt(LocalDateTime.now())
            .isHidden(false)
            .build();
        phone.getReviews().add(review);

        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phone));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(phoneRepository.save(any(Phone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.toggleReviewVisibility("phone-1", "r1", true, "user-1");

        assertTrue(response.getIsHidden());
        assertEquals("John Doe", response.getReviewer());
        verify(phoneRepository).save(phone);
    }

    @Test
    void toggleReviewVisibility_shouldThrow_whenPhoneOrReviewNotFoundOrUnauthorized() {
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.toggleReviewVisibility("phone-1", "r1", true, "user-1"));

        Phone phoneNoReviews = Phone.builder().id("phone-1").reviews(new ArrayList<>()).build();
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phoneNoReviews));
        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.toggleReviewVisibility("phone-1", "r1", true, "user-1"));

        Phone.Review review = Phone.Review.builder()
            .id("r1")
            .reviewerId("other-user")
            .rating(5)
            .comment("Nice")
            .createdAt(LocalDateTime.now())
            .isHidden(false)
            .build();
        phoneNoReviews.getReviews().add(review);
        phoneNoReviews.setSeller(User.builder().id("seller-1").build());
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phoneNoReviews));

        assertThrows(UnauthorizedException.class,
            () -> reviewService.toggleReviewVisibility("phone-1", "r1", true, "user-1"));
    }

    @Test
    void deleteReview_shouldRemove_whenReviewerMatches() {
        Phone.Review review = Phone.Review.builder()
            .id("r1")
            .reviewerId("user-1")
            .rating(5)
            .comment("Nice")
            .createdAt(LocalDateTime.now())
            .isHidden(false)
            .build();
        phone.getReviews().add(review);

        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phone));
        when(phoneRepository.save(any(Phone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> reviewService.deleteReview("phone-1", "r1", "user-1"));
        assertTrue(phone.getReviews().isEmpty());
        verify(phoneRepository).save(phone);
    }

    @Test
    void deleteReview_shouldThrow_whenPhoneOrReviewNotFoundOrUnauthorized() {
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.deleteReview("phone-1", "r1", "user-1"));

        Phone phoneNoReviews = Phone.builder().id("phone-1").reviews(new ArrayList<>()).build();
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phoneNoReviews));
        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.deleteReview("phone-1", "r1", "user-1"));

        Phone.Review review = Phone.Review.builder()
            .id("r1")
            .reviewerId("other-user")
            .rating(5)
            .comment("Nice")
            .createdAt(LocalDateTime.now())
            .isHidden(false)
            .build();
        phoneNoReviews.getReviews().add(review);
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phoneNoReviews));

        assertThrows(UnauthorizedException.class,
            () -> reviewService.deleteReview("phone-1", "r1", "user-1"));
    }

    @Test
    void filterVisibleReviews_shouldApplyVisibilityRulesAndMapReviewerNames() {
        Phone.Review visible = Phone.Review.builder()
            .id("r1").reviewerId("u1").isHidden(false).rating(5).comment("v").createdAt(LocalDateTime.now()).build();
        Phone.Review hiddenBySeller = Phone.Review.builder()
            .id("r2").reviewerId("u2").isHidden(true).rating(4).comment("h1").createdAt(LocalDateTime.now()).build();
        Phone.Review hiddenByReviewer = Phone.Review.builder()
            .id("r3").reviewerId("u3").isHidden(true).rating(3).comment("h2").createdAt(LocalDateTime.now()).build();

        List<Phone.Review> reviews = List.of(visible, hiddenBySeller, hiddenByReviewer);

        User seller = User.builder().id("seller-1").firstName("Seller").lastName("One").build();
        User u1 = User.builder().id("u1").firstName("User").lastName("One").build();
        User u3 = User.builder().id("u3").firstName("User").lastName("Three").build();

        when(userRepository.findAllById(anyList())).thenReturn(List.of(u1, u3));

        // current user is u3, sellerId = seller-1
        List<ReviewResponse> responses = reviewService.filterVisibleReviews(reviews, "u3", "seller-1");

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> r.getReviewerId().equals("u1")));
        assertTrue(responses.stream().anyMatch(r -> r.getReviewerId().equals("u3")));
        assertEquals("User One", responses.stream().filter(r -> r.getReviewerId().equals("u1")).findFirst().get().getReviewer());
        assertEquals("User Three", responses.stream().filter(r -> r.getReviewerId().equals("u3")).findFirst().get().getReviewer());
    }
}
