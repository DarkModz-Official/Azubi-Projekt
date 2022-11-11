package org.devcloud.ap.utils;

import io.sentry.Sentry;
import org.devcloud.ap.Azubiprojekt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Scanner;


public class Config {
    static Logger logger = LoggerFactory.getLogger(Config.class);
    Properties props = new Properties();

    public String dbhost;
    public String dbuser;
    public String dbpass;
    public String dbtable;
    public String dbport;
    public String gui;
    public int serverport;
    public String serverhost;
    public String debugmode;

    public void initConfig() {
        try {
            props.load(Azubiprojekt.class.getClassLoader().getResourceAsStream("config.properties"));
            getProperties();
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    public void getProperties() {
        logger.info("Config is imported!");
        if (props.isEmpty()) {
            logger.warn("No Configuration Found!");
            logger.warn("Do you want to create a new one? (y/n)");
            var scanner = new java.util.Scanner(System.in);
            var input = scanner.nextLine();
            if (input.equalsIgnoreCase("y")) {
                logger.info("Creating new config...");
                createConfig();
            } else {
                logger.info("Exiting...");
                System.exit(0);
            }
        }

        dbhost = props.getProperty("dbhost");
        dbport = props.getProperty("dbport");
        dbuser = props.getProperty("dbuser");
        dbpass = props.getProperty("dbpass");
        dbtable = props.getProperty("dbtable");
        gui = props.getProperty("gui");
        serverport = Integer.parseInt(props.getProperty("serverport"));
        serverhost = props.getProperty("serverhost");
        debugmode = props.getProperty("debugmode");
    }

    public void createConfig() {
        Scanner scanner = new Scanner(System.in);
        // Database
        logger.info("Please enter the Database Host:");
        var dbHost = scanner.nextLine();
        logger.info("Please enter the Database Port:");
        var dbPort = scanner.nextLine();
        logger.info("Please enter the Database Username:");
        var dbUser = scanner.nextLine();
        logger.info("Please enter the Database Password:");
        var dbPass = scanner.nextLine();
        logger.info("Please enter the Database Table Name:");
        var dbName = scanner.nextLine();

        // Server
        logger.info("Do you want to Use a GUI? (y/n)");
        var guiconf = scanner.nextLine();
        logger.info("On Which Port should the Server run?");
        var serverPort = scanner.nextLine();
        logger.info("Which ip should the Server run on?");
        var serverIp = scanner.nextLine();

        // Devleoper
        logger.info("Debug Mode? (y/n)");
        var debug = scanner.nextLine();

        props.put("dbhost", dbHost);
        props.put("dbport", dbPort);
        props.put("dbuser", dbUser);
        props.put("dbpass", dbPass);
        props.put("dbname", dbName);
        props.put("gui", guiconf);
        props.put("serverport", serverPort);
        props.put("serverip", serverIp);
        props.put("debug", debug);
    }
}
