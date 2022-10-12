package org.devcloud.ap.apicalls;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.devcloud.ap.Azubiprojekt;
import org.devcloud.ap.database.PgUser;
import org.devcloud.ap.utils.JSONCreator;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class User {
    private static final Logger logger = LoggerFactory.getLogger(User.class);

    public static void register(HttpServer httpServer) {
        httpServer.createContext("/api/user/create", new Create());
        httpServer.createContext("/api/user/delete", new Delete());
        httpServer.createContext("/api/user/edit", new Edit());
        httpServer.createContext("/api/user/login", new Login());
    }

    private User() {}

    private final static String error = "error";

    private static void addResponseHeaders(HttpExchange httpExchange) {
        httpExchange.getResponseHeaders().add("Content-Type", "application/json");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    }

    private static void writeResponse(HttpExchange httpExchange, String response, int statusCode) throws IOException {
        httpExchange.sendResponseHeaders(statusCode, response.length());

        OutputStream outputStream = httpExchange.getResponseBody();
        for(char write : response.toCharArray())
            outputStream.write(write);
        outputStream.close();
    }

    private static JSONCreator getJSONCreator(int statusCode) {
        return new JSONCreator().addKeys("statuscode").addValue(statusCode);
    }

    private static void debugRequest(URI requestURI) {
        logger.debug("{} - was requested", requestURI);
    }

    private static HashMap<String, String> getEntities(URI uri) {
        HashMap<String, String> feedback = new HashMap<>();
        String query = uri.getQuery();
        if (query == null) {
            logger.debug("Nothing found in the List");
            return feedback;
        }

        String[] list = query.split("&");
        logger.debug("Found list length {}", list.length);

        for (String raw : list) {
            String[] splitter = raw.split("=");
            if(splitter.length == 2) {
                logger.debug("Found key {} with value {}", splitter[0], splitter[1]);
                feedback.put(splitter[0], splitter[1]);
            }
            else
                logger.debug("No key and value found!");

        }
        return feedback;
    }

    private enum EUser {
        USERNAME("username"), PASSWORD("password"), EMAIL("email"), TOKEN("token");
        final String name;
        EUser(String name) { this.name = name; }
        @Override
        public String toString() {return name.toLowerCase(); }
    }


    private enum EUserPattern {
        /*
         * Name:
         * mindestens 3 zeichen
         * erlaubt sind:
         * groß und klein buchstaben
         * 0-9, _ und -
         * Password:
         * mindestens 8 zeichen
         * es muss mindestens:
         * ein Großbuchstabe, Kleinbuchstabe, spezial character
         */
        NAME("^[a-zA-Z0-9-_]{3,}$"),
        PASSWORD("^(?=.*?[A-Z])(?=(.*[a-z]){1,})(?=(.*[\\W])*)(?!.*\\s).{8,}$"),
        EMAIL("^[a-zA-Z0-9.!#$%&'*+=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

        final String aPattern;
        EUserPattern(String pattern) { this.aPattern = pattern; }
        @Override
        public String toString() {return aPattern; }

        public boolean isMatch(CharSequence input) {
            Pattern pattern = Pattern.compile(aPattern);
            Matcher matcher = pattern.matcher(input);
            return !matcher.find();
        }
    }

    private static final String TOKEN_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static String createToken() {
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 64; i++) {
            sb.append(TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }

    private static class Create implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addResponseHeaders(httpExchange);

            if(!Azubiprojekt.getSqlPostgres().isConnection()) {
                String response = getJSONCreator(500)
                        .addKeys(error)
                        .addValue("Datenbank ist nicht Erreichbar!").toString();

                writeResponse(httpExchange, response, 500);
                return;
            }

            URI requestURI = httpExchange.getRequestURI();
            debugRequest(requestURI);

            // Prüfe ob alles den syntax vorgibt

            HashMap<String, String> query = getEntities(requestURI);
            if(query.isEmpty()) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden keine Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(!query.containsKey(EUser.USERNAME.toString()) || !query.containsKey(EUser.PASSWORD.toString()) || !query.containsKey(EUser.EMAIL.toString())) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden nicht die richtigen Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.NAME.isMatch(query.get(EUser.USERNAME.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Name entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.PASSWORD.isMatch(query.get(EUser.PASSWORD.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Das Passwort entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.EMAIL.isMatch(query.get(EUser.EMAIL.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Die E-Mail entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            // öffne verbindung

            Session session = Azubiprojekt.getSqlPostgres().openSession();
            session.beginTransaction();

            // erstelle user

            String randomToken = createToken();
            PgUser pgUser = new PgUser(
                    query.get(EUser.USERNAME.toString()),
                    query.get(EUser.PASSWORD.toString()),
                    query.get(EUser.EMAIL.toString()),
                    randomToken);

            // prüfe ob der user existiert
            String queryString = "SELECT COUNT(*) FROM PgUser pguser WHERE pguser.username= :username";
            Query queryDatabase = session.createQuery(queryString, Long.class);
            queryDatabase.setParameter("username", pgUser.getUsername());
            Long count = (Long) queryDatabase.uniqueResult();

            logger.debug("Es wurden {} user gefunden.", count);

            if(count > 0) {
                // user exist and close session
                session.close();

                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Username wurde schon vergeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            // add user
            session.persist(pgUser);

            session.getTransaction().commit();
            session.close();
            logger.debug("ID {} wurde mit dem User {} erfolgreich erstellt.", pgUser.getUserid(), pgUser.getUsername());


            String response = getJSONCreator(201)
                    .addKeys("success", "name", "email", "token")
                    .addValue( "User wurde Erfolgreich erstellt!", query.get(EUser.USERNAME.toString()), query.get(EUser.EMAIL.toString()), randomToken ).toString();

            writeResponse(httpExchange, response, 200);
        }
    }

    private static class Delete implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addResponseHeaders(httpExchange);

            if(!Azubiprojekt.getSqlPostgres().isConnection()) {
                String response = getJSONCreator(500)
                        .addKeys(error)
                        .addValue("Datenbank ist nicht Erreichbar!").toString();

                writeResponse(httpExchange, response, 500);
                return;
            }

            URI requestURI = httpExchange.getRequestURI();
            debugRequest(requestURI);

            HashMap<String, String> query = getEntities(requestURI);
            if(query.isEmpty()) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden keine Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(!query.containsKey(EUser.TOKEN.toString())) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden nicht die richtigen Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            // öffne verbindung

            Session session = Azubiprojekt.getSqlPostgres().openSession();
            session.beginTransaction();

            // hole user informationen
            String queryString = "FROM PgUser pguser WHERE pguser.usertoken= :usertoken";
            Query queryDatabase = session.createQuery(queryString, PgUser.class);
            queryDatabase.setParameter("usertoken", query.get(EUser.TOKEN.toString()));
            PgUser pgUser = (PgUser) queryDatabase.uniqueResult();

            if(pgUser == null) {
                session.close();
                // user existiert nicht
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Token ist nicht gültig.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            session.remove(pgUser);
            session.getTransaction().commit();
            session.close();

            logger.debug("Der benutzer dem den token {} gehört hatte wurde gelöscht.", query.get(EUser.TOKEN.toString()));

            String response = getJSONCreator(201)
                    .addKeys("success")
                    .addValue( "User wurde erfolgreich gelöscht!").toString();


            writeResponse(httpExchange, response, 200);
        }
    }

    private static class Edit implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addResponseHeaders(httpExchange);

            if(!Azubiprojekt.getSqlPostgres().isConnection()) {
                String response = getJSONCreator(500)
                        .addKeys(error)
                        .addValue("Datenbank ist nicht Erreichbar!").toString();

                writeResponse(httpExchange, response, 500);
                return;
            }

            URI requestURI = httpExchange.getRequestURI();
            debugRequest(requestURI);

            // Prüfe ob alles den syntax vorgibt

            HashMap<String, String> query = getEntities(requestURI);
            if(query.isEmpty()) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden keine Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(!query.containsKey(EUser.USERNAME.toString()) || !query.containsKey(EUser.PASSWORD.toString()) || !query.containsKey(EUser.EMAIL.toString())) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden nicht die richtigen Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.NAME.isMatch(query.get(EUser.USERNAME.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Name entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.PASSWORD.isMatch(query.get(EUser.PASSWORD.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Das Passwort entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.EMAIL.isMatch(query.get(EUser.EMAIL.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Die E-Mail entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            // öffne verbindung

            Session session = Azubiprojekt.getSqlPostgres().openSession();
            session.beginTransaction();

            // hole mir die user informationen
            String queryString = "FROM PgUser pguser WHERE pguser.username= :username";
            Query queryDatabase = session.createQuery(queryString, PgUser.class);
            queryDatabase.setParameter("username", query.get(EUser.USERNAME.toString()));
            PgUser pgUser = (PgUser) queryDatabase.uniqueResult();

            if(pgUser == null) {
                session.close();
                // user existiert nicht
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Username existiert nicht.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            logger.debug("Es wurden der User {} gefunden.", pgUser.getUsername());

            // Passwort prüfen
            if(!query.get(EUser.PASSWORD.toString()).equals(pgUser.getUserpassword())) {
                session.close();
                // passwort falsch
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Username oder das Passwort ist falsch.").toString();

                writeResponse(httpExchange, response, 400);
            }

            // new random token
            String randomToken = createToken();
            pgUser.setUsertoken(randomToken);

            pgUser.setUsermail(query.get(EUser.EMAIL.toString()));
            pgUser.setUserpassword(query.get(EUser.PASSWORD.toString()));

            session.merge(pgUser);
            session.getTransaction().commit();
            session.close();

            logger.debug("ID {} wurde mit dem User {} ein neuer Token gesetzt und Informationen aktualisiert.", pgUser.getUserid(), pgUser.getUsername());


            String response = getJSONCreator(201)
                    .addKeys("success", "name", "email", "token")
                    .addValue( "User hat sich Erfolgreich bearbeitet!", query.get(EUser.USERNAME.toString()), query.get(EUser.EMAIL.toString()), randomToken ).toString();

            writeResponse(httpExchange, response, 200);
        }
    }

    private static class Login implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addResponseHeaders(httpExchange);

            if(!Azubiprojekt.getSqlPostgres().isConnection()) {
                String response = getJSONCreator(500)
                        .addKeys(error)
                        .addValue("Datenbank ist nicht Erreichbar!").toString();

                writeResponse(httpExchange, response, 500);
                return;
            }

            URI requestURI = httpExchange.getRequestURI();
            debugRequest(requestURI);

            // Prüfe ob alles den syntax vorgibt

            HashMap<String, String> query = getEntities(requestURI);
            if(query.isEmpty()) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden keine Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(!query.containsKey(EUser.USERNAME.toString()) || !query.containsKey(EUser.PASSWORD.toString())) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Es wurden nicht die richtigen Informationen mitgegeben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.NAME.isMatch(query.get(EUser.USERNAME.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Name entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            if(EUserPattern.PASSWORD.isMatch(query.get(EUser.PASSWORD.toString()))) {
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Das Passwort entspricht nicht den Vorgaben.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            // öffne verbindung

            Session session = Azubiprojekt.getSqlPostgres().openSession();
            session.beginTransaction();

            // hole mir die user informationen
            String queryString = "FROM PgUser pguser WHERE pguser.username= :username";
            Query queryDatabase = session.createQuery(queryString, PgUser.class);
            queryDatabase.setParameter("username", query.get(EUser.USERNAME.toString()));
            PgUser pgUser = (PgUser) queryDatabase.uniqueResult();

            if(pgUser == null) {
                session.close();
                // user existiert nicht
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Username oder das Passwort ist falsch.").toString();

                writeResponse(httpExchange, response, 400);
                return;
            }

            logger.debug("Es wurden der User {} gefunden.", pgUser.getUsername());

            // Passwort prüfen
            if(!query.get(EUser.PASSWORD.toString()).equals(pgUser.getUserpassword())) {
                session.close();
                // passwort falsch
                String response = getJSONCreator(400)
                        .addKeys(error)
                        .addValue("Der Username oder das Passwort ist falsch.").toString();

                writeResponse(httpExchange, response, 400);
            }

            // new random token
            String randomToken = createToken();
            pgUser.setUsertoken(randomToken);

            session.merge(pgUser);

            session.getTransaction().commit();
            session.close();

            logger.debug("ID {} wurde mit dem User {} ein neuer Token gesetzt.", pgUser.getUserid(), pgUser.getUsername());


            String response = getJSONCreator(201)
                    .addKeys("success", "name", "email", "token")
                    .addValue( "User hat sich Erfolgreich eingeloggt!", query.get(EUser.USERNAME.toString()), query.get(EUser.EMAIL.toString()), randomToken ).toString();

            writeResponse(httpExchange, response, 200);
        }
    }
}
