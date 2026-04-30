package org.phoebus.channelfinder.configuration;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpConnectorConfig {

  @Value("${server.http.port:8080}")
  private int port;

  @Bean
  @ConditionalOnProperty(name = "server.http.enable")
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpConnectorCustomizer() {
    return factory -> factory.addAdditionalConnectors(getHttpConnector());
  }

  private Connector getHttpConnector() {
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setScheme("http");
    connector.setPort(port);
    return connector;
  }
}
