package com.apollo.vpnbroker;

import com.apollo.schema.RequestSpec;
import com.apollo.schema.ResponseSpec;
import com.google.gson.Gson;
import com.hedera.sdk.common.HederaKey;
import com.hedera.sdk.common.HederaTransactionAndQueryDefaults;
import com.hedera.sdk.cryptography.HederaCryptoKeyPair;
import com.hedera.sdk.file.HederaFile;
import com.hedera.utilities.ExampleUtilities;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BrokerLauncher {

    static final Logger logger = LoggerFactory.getLogger(BrokerLauncher.class);
    public static Gson _gson = new Gson();

    public static class BrokerLoop implements Runnable {

        private volatile boolean cancelled;

        public void run() {

            logger.info("Broker loop started");

            while (!cancelled) {
                try {
                    Thread.sleep(2000);

                    RequestSpec[] pendingRequests = getPendingRequests();

                    logger.info("Pending requests {}", pendingRequests.length);

                    ArrayList<ResponseSpec> responses = new ArrayList<>();

                    for (RequestSpec spec : pendingRequests) {
                        try {
                            responses.add(LaunchRequest(spec));
                        } catch (Exception ex) {
                            logger.error("Error on broker loop request", ex);
                        }
                    }

                    for (ResponseSpec spec : responses) {
                        logger.info("fetched {}", _gson.toJson(spec));
                    }

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

    private static final long REQUEST_FILE_ID = 1023;

    public static ResponseSpec LaunchRequest(RequestSpec requestSpec) throws IOException {

        URL url = new URL(requestSpec.uri);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int status = con.getResponseCode();

        // Finally, let’s read the response of the request and place it in a content String:
        BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getInputStream()));

        String inputLine;

        StringBuffer content = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();

        // To close the connection, we can use the disconnect() method:
        con.disconnect();

        ResponseSpec responseSpec = new ResponseSpec();

        responseSpec.id = requestSpec.id;
        responseSpec.content = content.toString();

        return responseSpec;
    }

    public static RequestSpec[] getPendingRequests() throws Exception {

        long id = REQUEST_FILE_ID ;

        HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

        qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);
        HederaFile hederaFile;

        hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = qd;
        hederaFile.fileNum = id;

        byte[] contents = hederaFile.getContents();

        String[] lines = new String(contents, StandardCharsets.UTF_8).split("\n");

        return Arrays.stream(lines).map(st -> _gson.fromJson(st, RequestSpec.class)).toArray(RequestSpec[]::new);
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
