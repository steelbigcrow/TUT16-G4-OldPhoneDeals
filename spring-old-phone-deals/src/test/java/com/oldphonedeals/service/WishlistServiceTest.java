package com.oldphonedeals.service;

import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.wishlist.WishlistResponse;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.WishlistService;
import com.oldphonedeals.service.impl.WishlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WishlistService 
 */
@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhoneRepository phoneRepository;

    private WishlistService wishlistService;

    private User user;
    private Phone phone;

    @BeforeEach
    void setUp() {
        wishlistService = new WishlistServiceImpl(userRepository, phoneRepository);

        user = new User();
        user.setId("user-1");
        user.setWishlist(new ArrayList<>());

        phone = Phone.builder()
            .id("phone-1")
            .title("Test Phone")
            .brand(null)
            .image("image.jpg")
            .stock(10)
            .price(10.0)
            .seller(User.builder().firstName("John").lastName("Doe").build())
            .createdAt(LocalDateTime.now())
            .isDisabled(false)
            .build();
    }

    @Test
    void addToWishlist_shouldSucceed_whenUserAndPhoneValid() {
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phone));
            when(phoneRepository.findAllById(any())).thenReturn(List.of(phone));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WishlistResponse response = wishlistService.addToWishlist("user-1", "phone-1");

            assertNotNull(response);
            assertEquals("user-1", response.getUserId());
            assertEquals(1, response.getTotalItems());
            assertEquals("phone-1", response.getPhones().get(0).getId());
            verify(userRepository).save(user);
        }
    }

    @Test
    void addToWishlist_shouldThrowUnauthorized_whenUserIdNotMatchCurrent() {
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("another-user");

            UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> wishlistService.addToWishlist("user-1", "phone-1")
            );

            assertTrue(ex.getMessage().contains("own wishlist"));
            verifyNoInteractions(userRepository, phoneRepository);
        }
    }

    @Test
    void addToWishlist_shouldThrowWhenUserNotFound() {
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");
            when(userRepository.findById("user-1")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> wishlistService.addToWishlist("user-1", "phone-1"));
        }
    }

    @Test
    void addToWishlist_shouldThrowWhenPhoneDisabledOrDuplicate() {
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

            Phone disabledPhone = Phone.builder().id("phone-1").isDisabled(true).build();
            when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(disabledPhone));

            assertThrows(BadRequestException.class,
                () -> wishlistService.addToWishlist("user-1", "phone-1"));

            // change to enabled but already in wishlist
            disabledPhone.setIsDisabled(false);
            user.getWishlist().add("phone-1");
            assertThrows(BadRequestException.class,
                () -> wishlistService.addToWishlist("user-1", "phone-1"));
        }
    }

    @Test
    void removeFromWishlist_shouldRemoveItemAndReturnUpdatedResponse() {
        user.getWishlist().add("phone-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");

            WishlistResponse response = wishlistService.removeFromWishlist("user-1", "phone-1");

            assertNotNull(response);
            assertEquals(0, response.getTotalItems());
            verify(userRepository).save(user);
        }
    }

    @Test
    void getUserWishlist_shouldReturnConvertedPhoneList() {
        user.getWishlist().add("phone-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(phoneRepository.findAllById(List.of("phone-1"))).thenReturn(List.of(phone));

        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");

            WishlistResponse response = wishlistService.getUserWishlist("user-1");

            assertNotNull(response);
            assertEquals("user-1", response.getUserId());
            assertEquals(1, response.getTotalItems());
            PhoneListItemResponse item = response.getPhones().get(0);
            assertEquals("Test Phone", item.getTitle());
            assertEquals(10, item.getStock());
        }
    }
}
