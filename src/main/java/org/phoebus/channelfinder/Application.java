/*
 * #%L
 * ChannelFinder Directory Service
 * %%
 * Copyright (C) 2010 - 2018 Brookhaven National Laboratory / National Synchrotron Light Source II
 * %%
 * Copyright (C) 2010 - 2012 Brookhaven National Laboratory
 * All rights reserved. Use is subject to license terms.
 * #L%
 */
package org.phoebus.channelfinder;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.channelfinder.configuration.ChannelProcessor;
import org.phoebus.channelfinder.configuration.PopulateDBConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.FileCopyUtils;

@EnableAutoConfiguration
@ComponentScan(basePackages = "org.phoebus.channelfinder")
@EnableScheduling
@OpenAPIDefinition(servers = {@Server(url = "/")})
@SpringBootApplication
public class Application implements ApplicationRunner {

  public static final Logger logger = Logger.getLogger(Application.class.getName());

  public static void main(String[] args) {
    // Set the java truststore used by channelfinder
    configureTruststore();
    SpringApplication.run(Application.class, args);
  }

  /** Set the default ssl trust store */
  private static void configureTruststore() {
    if (System.getProperty("javax.net.ssl.trustStore") == null) {
      logger.log(Level.INFO, "using default javax.net.ssl.trustStore");
      try (InputStream in = Application.class.getResourceAsStream("/keystore/cacerts")) {
        // read input
        File tempFile = File.createTempFile("cf-", "-truststore");
        FileOutputStream out = new FileOutputStream(tempFile);
        FileCopyUtils.copy(in, out);
        tempFile.deleteOnExit();
        System.setProperty("javax.net.ssl.trustStore", tempFile.getAbsolutePath());
      } catch (IOException e) {
        logger.log(Level.SEVERE, "failed to configure channelfinder truststore", e);
      }
    }
    if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {
      logger.log(Level.INFO, "using default javax.net.ssl.trustStorePassword");
      System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }
  }

  @Autowired PopulateDBConfiguration service;

  public void run(ApplicationArguments args) throws Exception {
    if (args.containsOption("demo-data")) {
      int numberOfCells =
          args.getOptionValues("demo-data").stream().mapToInt(Integer::valueOf).max().orElse(1);
      logger.log(Level.INFO, "Populating the channelfinder service with demo data");
      service.createDB(numberOfCells);
    }
    if (args.containsOption("cleanup")) {
      int numberOfCells =
          args.getOptionValues("cleanup").stream().mapToInt(Integer::valueOf).max().orElse(1);
      // This is kind of a hack, the create Db is being called to reset the channels and then
      // deleting them
      logger.log(
          Level.INFO,
          "Populating the channelfinder service with demo data first, then deleting them");
      service.createDB(numberOfCells);
      logger.log(Level.INFO, "Cleaning up the populated demo data");
      service.cleanupDB();
    }
  }

  /**
   * List of {@link ChannelProcessor} implementations called when new channels are created or
   * existing channels are updated
   *
   * @return A list of {@link ChannelProcessor}s, if any have been registered over SPI.
   */
  @Bean
  public List<ChannelProcessor> channelProcessors() {
    List<ChannelProcessor> processors = new ArrayList<>();
    ServiceLoader<ChannelProcessor> loader = ServiceLoader.load(ChannelProcessor.class);
    loader.stream()
        .forEach(
            p -> {
              ChannelProcessor notifier = p.get();
              processors.add(notifier);
            });
    return processors;
  }

  /**
   * {@link TaskExecutor} used when calling {@link ChannelProcessor}s.
   *
   * @return A {@link TaskExecutor}
   */
  @Bean
  public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(3);
    taskExecutor.setMaxPoolSize(10);
    taskExecutor.setQueueCapacity(25);

    return taskExecutor;
  }
}
