package org.phoebus.channelfinder.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the legacy ChannelFinder HTTP API path prefix.
 *
 * <p>The property {@code channelfinder.legacy.service-root} controls the first path segment of all
 * legacy-API URLs (e.g. {@code ChannelFinder/resources/channels}). Multi-service deployments
 * typically use different roots per service to distinguish paths at a shared reverse proxy.
 *
 * <p>The value is normalized on startup: surrounding whitespace and leading and trailing slashes
 * are stripped. An empty value serves the endpoints directly under {@code /}. The controllers map
 * the normalized value.
 */
@Component
@ConfigurationProperties(prefix = "channelfinder.legacy")
public class LegacyApiProperties {

  static final String DEFAULT_ROOT = "ChannelFinder";

  private String serviceRoot = DEFAULT_ROOT;

  @PostConstruct
  void normalize() {
    if (serviceRoot == null) {
      serviceRoot = DEFAULT_ROOT;
      return;
    }
    serviceRoot = serviceRoot.strip();
    while (serviceRoot.startsWith("/")) serviceRoot = serviceRoot.substring(1);
    while (serviceRoot.endsWith("/"))
      serviceRoot = serviceRoot.substring(0, serviceRoot.length() - 1);
  }

  public String getServiceRoot() {
    return serviceRoot;
  }

  public void setServiceRoot(String serviceRoot) {
    this.serviceRoot = serviceRoot;
  }
}
