package org.devcloud.ap;

import io.sentry.Sentry;
import lombok.Getter;
import org.devcloud.ap.utils.SQLPostgres;

import java.io.IOException;

import org.devcloud.ap.utils.HTTPServer;
import org.devcloud.ap.utils.apihelper.databsehelper.RoleDatabaseHelper;
import org.devcloud.ap.utils.SentryLogger;
import org.devcloud.ap.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Azubiprojekt {

    @Getter
    static SQLPostgres sqlPostgres;
    private static final Logger logger = LoggerFactory.getLogger(Azubiprojekt.class);

    public static void main(String[] args) {
        SentryLogger.startSentry();
        Config config = new Config();
        config.initConfig();

        logger.info("Starting Azubiprojekt Server");
        try {
            HTTPServer.startServer();
        } catch (IOException e) {
            Sentry.captureException(e);
        }
        sqlPostgres = new SQLPostgres(config.dbhost+":"+config.dbport, config.dbuser, config.dbpass, config.dbtable);
        RoleDatabaseHelper.autoCreate(logger);
    }
}
