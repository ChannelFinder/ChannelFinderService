package org.phoebus.channelfinder.exceptions;

public class ArchiverServiceException extends RuntimeException {

  public ArchiverServiceException(String message) {
    super(message);
  }

  public ArchiverServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
