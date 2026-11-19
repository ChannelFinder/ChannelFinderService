package org.phoebus.channelfinder.exceptions;

/**
 * Thrown when a {@link org.phoebus.channelfinder.configuration.ChannelPreProcessor} encounters a
 * hard failure that should abort the channel create/update operation.
 *
 * <p>This differs from warnings (returned in {@link
 * org.phoebus.channelfinder.configuration.PreProcessResult}) in that it immediately stops
 * processing and prevents persistence. Use this for validation failures, missing mandatory
 * properties, or fatal external service errors (e.g. notification service unreachable).
 */
public class ChannelPreProcessException extends RuntimeException {

  /**
   * Constructs a new exception with the given detail message.
   *
   * @param message the detail message describing the failure reason.
   */
  public ChannelPreProcessException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the given detail message and cause.
   *
   * @param message the detail message describing the failure reason.
   * @param cause the underlying cause of this exception.
   */
  public ChannelPreProcessException(String message, Throwable cause) {
    super(message, cause);
  }
}
