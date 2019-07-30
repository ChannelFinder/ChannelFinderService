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
package gov.bnl.channelfinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.FileCopyUtils;

import gov.bnl.channelfinder.example.PopulateService;

@EnableAutoConfiguration
@ComponentScan(basePackages="gov.bnl.channelfinder")
@SpringBootApplication
public class Application  implements ApplicationRunner {

    static Logger logger = Logger.getLogger(Application.class.getName());

    public static void main(String[] args){
        // Set the java truststore used by channelfinder
        configureTruststore();
        SpringApplication.run(Application.class, args);
    }

    /**
     * Set the default ssl trust store
     */
    private static void configureTruststore() {
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            logger.log(Level.INFO, "using default javax.net.ssl.trustStore");
            try (InputStream in = Application.class.getResourceAsStream("/keystore/cacerts")) {
                // read input
                File tempFile= File.createTempFile("cf-", "-truststore");
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

    @Autowired
    PopulateService service;

    public void run(ApplicationArguments args) throws Exception {
        if(args.containsOption("demo-data")) {
            int numberOfCells = args.getOptionValues("demo-data").stream().mapToInt(Integer::valueOf).max().orElse(1);
            logger.log(Level.INFO, "Populating the channelfinder service with demo data");
            service.createDB(numberOfCells);
        }
        if(args.containsOption("cleanup")) {
            int numberOfCells = args.getOptionValues("cleanup").stream().mapToInt(Integer::valueOf).max().orElse(1);
            // This is kind of a hack, the create Db is being called to reset the channels and then deleting them
            service.createDB(numberOfCells);
            service.cleanupDB();
        }
    }

}