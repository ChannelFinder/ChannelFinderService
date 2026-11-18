package org.phoebus.channelfinder.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LegacyApiPropertiesTest {

  private static String normalized(String serviceRoot) {
    LegacyApiProperties properties = new LegacyApiProperties();
    properties.setServiceRoot(serviceRoot);
    properties.normalize();
    return properties.getServiceRoot();
  }

  static Stream<Arguments> slashesAndWhitespaceAreStripped() {
    return Stream.of(
        arguments(named("surrounding whitespace and slashes", " /cf/api/ "), "cf/api"),
        arguments(named("doubled slashes", "//cf//"), "cf"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void slashesAndWhitespaceAreStripped(String serviceRoot, String expected) {
    assertEquals(expected, normalized(serviceRoot));
  }

  static Stream<Named<String>> emptyRootStaysEmpty() {
    return Stream.of(named("empty", ""), named("a single slash", "/"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void emptyRootStaysEmpty(String serviceRoot) {
    assertEquals("", normalized(serviceRoot));
  }

  @Test
  void missingRootIsTheDefault() {
    assertEquals("ChannelFinder", normalized(null));
  }
}
