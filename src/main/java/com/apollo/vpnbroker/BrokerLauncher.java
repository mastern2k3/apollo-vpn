package com.apollo.vpnbroker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class BrokerLauncher {

    static final Logger logger = LoggerFactory.getLogger(BrokerLauncher.class);

    public static class BrokerLoop implements Runnable {

        private volatile boolean cancelled;

        public void run() {

            logger.info("Broker loop started");

            while (!cancelled) {
                try {
                    Thread.sleep(2000);


                } catch (Exception ex) {
                    logger.error("Error on broker loop", ex);
                }
            }
        }

        public void cancel()
        {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(7891), 0);

        server.createContext("/stats", new BrokerLauncher.StateHandler());

        Thread thread = new Thread(new BrokerLoop());

        thread.start();

        server.setExecutor(null);

        logger.info("Broker api starting at http://127.0.0.1:7891/stats");
        server.start();
    }

    static abstract class BaseHandler implements HttpHandler {

        public abstract String handleToString(HttpExchange t) throws Exception;

        @Override
        public void handle(HttpExchange t) throws IOException {

            String response;

            try {
                response = handleToString(t);
                t.sendResponseHeaders(200, response.length());
            } catch (Exception ex) {
                response = ex.toString();
                t.sendResponseHeaders(500, response.length());
            }

            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class StateHandler extends BaseHandler {
        @Override
        public String handleToString(HttpExchange t) throws Exception {
            return "ok";
        }
    }
}
