package org.devcloud.ap.utils;

import io.sentry.Sentry;
import lombok.Getter;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLPostgres {

    static Logger logger = LoggerFactory.getLogger(SQLPostgres.class);
    @Getter SessionFactory sessionFactory;
    @Getter boolean connection;

    public SQLPostgres(String host, String user, String password, String database) {
        String url = "jdbc:postgresql://%host%/%database%?ApplicationName=azubiprojekt";
        url = url.replace("%host%", host).replace("%database%", database);

        StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder()
                .configure()
                .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting(AvailableSettings.DRIVER, "org.postgresql.Driver")
                .applySetting(AvailableSettings.URL, url)
                .applySetting(AvailableSettings.USER, user)
                .applySetting(AvailableSettings.PASS, password)
                ;
        try {
            this.sessionFactory = new MetadataSources(
                    standardServiceRegistryBuilder.build()
            ).buildMetadata().buildSessionFactory();
            logger.info("SQL: SessionFactory wurde Erstellt");
            this.connection = true;
        } catch (Exception e) {
            logger.error("SQL: SessionFactory konnte nicht erstellt werden. Error: ");
            Sentry.captureException(e);
            this.connection = false;
        }
    }

    public Session openSession() throws HibernateException {
        return this.sessionFactory.openSession();
    }

    public void closeSession(Session session) {
        session.close();
    }
}
