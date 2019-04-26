package gov.bnl.channelfinder;

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
//package edu.msu.nscl.olog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan(basePackages="gov.bnl.channelfinder")
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // Set the java truststore used by channelfinder
        System.setProperty("javax.net.ssl.trustStore", Application.class.getResource("/keystore/cacerts").getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

        SpringApplication.run(Application.class, args);
    }

}