package com.oldphonedeals.exception;

/**
 * Thrown when a request attempts to update a resource using a stale version.
 * <p>
 * This is used to surface optimistic locking conflicts to the API client as HTTP 409.
 * </p>
 */
public class VersionConflictException extends RuntimeException {

  public VersionConflictException(String message) {
    super(message);
  }

  public VersionConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}

