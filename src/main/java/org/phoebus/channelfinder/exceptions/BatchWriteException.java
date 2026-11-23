package org.phoebus.channelfinder.exceptions;

import java.text.MessageFormat;
import java.util.List;
import org.phoebus.channelfinder.entity.Channel;

/** Thrown when Elasticsearch refused some or all of the documents in a batch write. */
public class BatchWriteException extends RepositoryException {

  // The message names at most this many of the refused documents, and counts the rest.
  static final int MAX_NAMES = 10;

  private final transient List<Channel> written;

  public BatchWriteException(String messageFormat, List<String> refused) {
    this(messageFormat, refused, List.of());
  }

  /** {@code written} holds the channels that Elasticsearch did write. */
  public BatchWriteException(String messageFormat, List<String> refused, List<Channel> written) {
    super(MessageFormat.format(messageFormat, names(refused)));
    this.written = List.copyOf(written);
  }

  public List<Channel> getWritten() {
    return written;
  }

  private static String names(List<String> refused) {
    if (refused.size() <= MAX_NAMES) {
      return refused.toString();
    }
    return refused.subList(0, MAX_NAMES) + " and " + (refused.size() - MAX_NAMES) + " more";
  }
}
