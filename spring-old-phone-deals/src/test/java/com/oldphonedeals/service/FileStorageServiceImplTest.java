package com.oldphonedeals.service;

import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.exception.FileStorageException;
import com.oldphonedeals.exception.FileValidationException;
import com.oldphonedeals.service.FileStorageService;
import com.oldphonedeals.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileStorageServiceImpl 单元测试
 */
class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private FileStorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new FileStorageProperties();
        properties.setDir(tempDir.toString());
        properties.setAllowedExtensions(Collections.singletonList("jpg"));
        properties.setAllowedMimeTypes(Collections.singletonList("image/jpeg"));
        properties.setMaxSize(1024 * 1024); // 1MB

        fileStorageService = new FileStorageServiceImpl(properties);
    }

    @AfterEach
    void tearDown() {
        // TempDir 会自动清理，无需手动删除
    }

    @Test
    void validateFile_shouldThrowForInvalidFile() {
        // null 或空文件
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(null));

        MockMultipartFile empty = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(empty));

        // 无文件名
        MockMultipartFile noName = new MockMultipartFile("file", (String) null, "image/jpeg", new byte[1]);
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(noName));

        // 含非法路径
        MockMultipartFile invalidPath = new MockMultipartFile("file", "../evil.jpg", "image/jpeg", new byte[1]);
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(invalidPath));

        // 无扩展名
        MockMultipartFile noExt = new MockMultipartFile("file", "file", "image/jpeg", new byte[1]);
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(noExt));

        // 扩展名不允许
        MockMultipartFile wrongExt = new MockMultipartFile("file", "file.png", "image/png", new byte[1]);
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(wrongExt));

        // MIME 类型不允许
        MockMultipartFile wrongMime = new MockMultipartFile("file", "file.jpg", "image/png", new byte[1]);
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(wrongMime));

        // 大小超限
        byte[] large = new byte[2 * 1024 * 1024];
        MockMultipartFile tooLarge = new MockMultipartFile("file", "file.jpg", "image/jpeg", large);
        assertThrows(FileValidationException.class,
            () -> fileStorageService.validateFile(tooLarge));
    }

    @Test
    void storeFile_andLoadAndDelete_shouldWorkForValidFile() throws IOException {
        byte[] content = "test-content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", content);

        String storedPath = fileStorageService.storeFile(file, "images");
        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("images/"));

        Path realPath = tempDir.resolve(storedPath.replace("/", java.io.File.separator));
        assertTrue(Files.exists(realPath));

        Resource resource = fileStorageService.loadFileAsResource(storedPath);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());

        fileStorageService.deleteFile(storedPath);
        assertFalse(Files.exists(realPath));
    }

    @Test
    void loadFileAsResource_shouldThrowWhenNotFound() {
        assertThrows(FileStorageException.class,
            () -> fileStorageService.loadFileAsResource("not-exist.jpg"));
    }

    @Test
    void deleteFile_shouldNotThrowWhenFileMissing() {
        assertDoesNotThrow(() -> fileStorageService.deleteFile("missing.jpg"));
    }
}
