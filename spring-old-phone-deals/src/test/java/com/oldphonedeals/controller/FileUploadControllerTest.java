package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.security.SecurityConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.service.FileStorageService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FileUploadController 集成测试（WebMvcTest）
 */
@WebMvcTest(value = FileUploadController.class,
    excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = CorsConfig.class
    ))
@Import({ControllerTestConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
@DisplayName("FileUploadController 测试")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("uploadImage_ValidFile_ReturnsSuccessResponse")
    void uploadImage_ValidFile_ReturnsSuccessResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            "test-content".getBytes()
        );

        when(fileStorageService.storeFile(any(), eq("images")))
            .thenReturn("images/generated-name.jpg");

        mockMvc.perform(multipart("/api/upload/image")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("File uploaded successfully"))
            .andExpect(jsonPath("$.data.fileName").value("images/generated-name.jpg"))
            .andExpect(jsonPath("$.data.fileUrl").value("/uploads/images/generated-name.jpg"))
            .andExpect(jsonPath("$.data.originalName").value("test-image.jpg"))
            .andExpect(jsonPath("$.data.contentType").value("image/jpeg"));
    }

    @Test
    @DisplayName("uploadImage_AnonymousUser_ReturnsUnauthorized")
    void uploadImage_AnonymousUser_ReturnsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            "test-content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/image")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));

        verify(fileStorageService, never()).storeFile(any(), eq("images"));
    }
}
