package org.phoebus.channelfinder.web.v0;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.exceptions.ChannelNotFoundException;
import org.phoebus.channelfinder.exceptions.ChannelValidationException;
import org.phoebus.channelfinder.exceptions.PropertyNotFoundException;
import org.phoebus.channelfinder.exceptions.PropertyValidationException;
import org.phoebus.channelfinder.exceptions.TagNotFoundException;
import org.phoebus.channelfinder.exceptions.TagValidationException;
import org.phoebus.channelfinder.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Translates domain exceptions thrown by v0 service classes to HTTP responses.
 *
 * <p>Scoped to the v0 API; a separate handler covers future API versions.
 *
 * @deprecated v0 API support ends after the planned deprecation period. Replace with a {@code
 *     ChannelFinderExceptionHandler} using {@code ProblemDetail} (RFC 9457) when the v1 API lands.
 */
@Deprecated
@RestControllerAdvice(basePackages = "org.phoebus.channelfinder.web.v0.controller")
public class V0ExceptionHandler {

  private static final Logger logger = Logger.getLogger(V0ExceptionHandler.class.getName());

  @ExceptionHandler(ChannelNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseStatusException handleChannelNotFound(ChannelNotFoundException ex) {
    logger.log(Level.FINE, ex::getMessage);
    return new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(TagNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseStatusException handleTagNotFound(TagNotFoundException ex) {
    logger.log(Level.FINE, ex::getMessage);
    return new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(PropertyNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseStatusException handlePropertyNotFound(PropertyNotFoundException ex) {
    logger.log(Level.FINE, ex::getMessage);
    return new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ChannelValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseStatusException handleChannelValidation(ChannelValidationException ex) {
    logger.log(Level.WARNING, ex.getMessage(), ex);
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(TagValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseStatusException handleTagValidation(TagValidationException ex) {
    logger.log(Level.WARNING, ex.getMessage(), ex);
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(PropertyValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseStatusException handlePropertyValidation(PropertyValidationException ex) {
    logger.log(Level.WARNING, ex.getMessage(), ex);
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseStatusException handleUnauthorized(UnauthorizedException ex) {
    logger.log(Level.WARNING, ex.getMessage(), ex);
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseStatusException handleMessageNotReadable(HttpMessageNotReadableException ex) {
    logger.log(Level.WARNING, ex.getMessage(), ex);
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed request body");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseStatusException handleUnexpected(Exception ex) {
    logger.log(Level.SEVERE, "Unhandled exception", ex);
    return new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }
}
