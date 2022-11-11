package org.devcloud.ap.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.devcloud.ap.Azubiprojekt;
import org.devcloud.ap.apicalls.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class HTTPServer {
    private static HttpServer server;
    private static final Logger logger = LoggerFactory.getLogger(HTTPServer.class);
    private static Config config = new Config();

    /**
     * Starts the HTTP server
     *
     * @throws IOException for the server
     */
    public static void startServer() throws IOException {
        config.initConfig();
        server = HttpServer.create(new InetSocketAddress(config.serverhost, config.serverport), 0); //Create a new server on port 8001
        server.createContext("/", new APIHandler()); //Create a new context for the API

        Base.register(server);
        User.register(server);
        Group.register(server);
        Member.register(server);
        Role.register(server);

        server.setExecutor(null);   //Create a new executor
        server.start(); //Start the server
    }

    /**
     * Stops the HTTP server
     */
    public void stopServer() {
        server.stop(0); //Stop the server
    }

    public static class APIHandler implements HttpHandler { //Create a new class for the HTTP handler and respond with the index.html
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            String request = httpExchange.getRequestURI().toString();

            File index = new File(Objects.requireNonNull(Azubiprojekt.class.getClassLoader().getResource("gui/index.html")).getPath());
            String response = Files.readString(Paths.get(index.getPath()));

            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream outputStream = httpExchange.getResponseBody();
            for (char write : response.toCharArray())
                outputStream.write(write);
            outputStream.close();

            logger.debug("{} - was requested", request);
        }
    }
}
