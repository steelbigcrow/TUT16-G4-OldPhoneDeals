package com.oldphonedeals.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件相关异常类单元测试
 */
class FileExceptionTest {

    @Test
    void fileValidationException_shouldStoreMessageAndCause() {
        Throwable cause = new RuntimeException("root");
        FileValidationException ex1 = new FileValidationException("msg1");
        FileValidationException ex2 = new FileValidationException("msg2", cause);

        assertEquals("msg1", ex1.getMessage());
        assertNull(ex1.getCause());

        assertEquals("msg2", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    void fileStorageException_shouldStoreMessageAndCause() {
        Throwable cause = new RuntimeException("root");
        FileStorageException ex1 = new FileStorageException("msg1");
        FileStorageException ex2 = new FileStorageException("msg2", cause);

        assertEquals("msg1", ex1.getMessage());
        assertNull(ex1.getCause());

        assertEquals("msg2", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }
}
