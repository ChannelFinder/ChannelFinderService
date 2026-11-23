package org.phoebus.channelfinder.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BatchWriteExceptionTest {

  private static final String FORMAT = "Failed to index channels {0}";

  @Test
  void messageNamesEveryRefusedDocumentUpToTheLimit() {
    BatchWriteException e = new BatchWriteException(FORMAT, List.of("ch1", "ch2"));

    assertEquals("Failed to index channels [ch1, ch2]", e.getMessage());
  }

  @Test
  void messageNamesTheFirstTenAndCountsTheRest() {
    List<String> refused = IntStream.rangeClosed(1, 12).mapToObj(i -> "ch" + i).toList();

    BatchWriteException e = new BatchWriteException(FORMAT, refused);

    assertEquals(
        "Failed to index channels [ch1, ch2, ch3, ch4, ch5, ch6, ch7, ch8, ch9, ch10] and 2 more",
        e.getMessage());
  }
}
