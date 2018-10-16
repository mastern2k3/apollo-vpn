package com.apollo.vpnbroker;

import com.apollo.pubapi.PublicApiLauncher;
import com.apollo.schema.PushSpec;
import com.apollo.schema.RequestSpec;
import com.apollo.schema.ResponseSpec;
import com.apollo.schema.StatsSpec;
import com.google.gson.Gson;
import com.hedera.file.FileAppend;
import com.hedera.file.FileCreate;
import com.hedera.file.FileGetContents;
import com.hedera.sdk.account.HederaAccount;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class BrokerLauncher {

    static final Logger logger = LoggerFactory.getLogger(BrokerLauncher.class);
    public static Gson _gson = new Gson();

    private static HederaFile _requestsFile;
    private static HederaFile _responseFile;


    public static void main(String[] args) throws Exception {

        _requestsFile = createRequestsFile();

        Thread.sleep(2000);

        _responseFile = createResponseFile();

        HttpServer server = HttpServer.create(new InetSocketAddress(7891), 8);

        server.createContext("/api/stats", new BrokerLauncher.StateHandler());
        server.createContext("/api/getfile", new BrokerLauncher.GetFileHandler());
        server.createContext("/api/postreq", new BrokerLauncher.PostRequestHandler());

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

            HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

            HederaAccount hederaAccount = new HederaAccount();

            hederaAccount.txQueryDefaults = qd;

            hederaAccount.setHederaAccountID(qd.payingAccountID);

            StatsSpec statsSpec = new StatsSpec();

            statsSpec.balance = hederaAccount.getBalance();
            statsSpec.reqFileNum = _requestsFile.fileNum;
            statsSpec.resFileNum = _responseFile.fileNum;

            return _gson.toJson(statsSpec);
        }
    }

    static class PostRequestHandler extends BaseHandler {
        @Override
        public String handleToString(HttpExchange t) throws Exception {

            String path = t.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf('/') + 1);

            byte[] decodedBytes = Base64.getDecoder().decode(idStr);
            String decodedString = new String(decodedBytes);

            RequestSpec requestSpec = RequestSpec.of(decodedString);

            FileAppend.append(_requestsFile, (_gson.toJson(requestSpec) + "\n").getBytes(StandardCharsets.UTF_8));

            PushSpec pushSpec = new PushSpec();

            pushSpec.reqId = requestSpec.id;
            pushSpec.respUrl = "http://localhost:7891/api/getfile/" + String.valueOf(_responseFile.fileNum);

            return _gson.toJson(pushSpec);
        }
    }

    static class GetFileHandler extends BaseHandler {
        @Override
        public String handleToString(HttpExchange t) throws Exception {

            String path = t.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf('/') + 1);

            long id = Long.parseLong(idStr);

            byte[] contents = FileGetContents.fileCache.getOrDefault(id, null);

            if (contents != null)
                return new String(contents, StandardCharsets.UTF_8);

            HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

            qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);
            HederaFile hederaFile;

            hederaFile = new HederaFile();

            hederaFile.txQueryDefaults = qd;
            hederaFile.fileNum = id;

            contents = FileGetContents.getContents(hederaFile);

            if (contents != null && contents.length > 0)
                FileGetContents.fileCache.put(id, contents);

            return new String(contents, StandardCharsets.UTF_8);
        }
    }

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

                    Thread.sleep(2000);

                    writeResponses(responses);

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

    private static void writeResponses(ArrayList<ResponseSpec> responses) throws Exception {

        StringBuilder builder = new StringBuilder();

        for (ResponseSpec spec : responses) {
            builder.append(_gson.toJson(spec));
            builder.append("\n");
        }

        byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);

        _responseFile.append(bytes);
    }

    private static HederaFile createRequestsFile() throws Exception {

        HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

        qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

        HederaFile hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = qd;

        hederaFile =
            FileCreate.create(
                hederaFile,
                (_gson.toJson(RequestSpec.of("https://api.myjson.com/bins/gwzhc")) + "\n").getBytes(StandardCharsets.UTF_8));

        return hederaFile;
    }

    private static HederaFile createResponseFile() throws Exception {

        HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

        qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

        HederaFile hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = qd;

        hederaFile = FileCreate.create(hederaFile, new byte[0]);

        return hederaFile;
    }

    public static ResponseSpec LaunchRequest(RequestSpec requestSpec) throws Exception {

        URL url = new URL(requestSpec.uri);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int status = con.getResponseCode();

        // Finally, let’s read the response of the request and place it in a content String:
        InputStream in = con.getInputStream();

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;

        // read bytes from the input stream and store them in buffer
        while ((len = in.read(buffer)) != -1) {
            // write bytes from the buffer into output stream
            os.write(buffer, 0, len);
        }

        in.close();

        // To close the connection, we can use the disconnect() method:
        con.disconnect();

        HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

        qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

        HederaFile hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = qd;

        byte[] content = os.toByteArray();

        hederaFile = FileCreate.create(hederaFile, os.toByteArray());

        FileGetContents.fileCache.put(hederaFile.fileNum, content);

        ResponseSpec responseSpec = new ResponseSpec();

        responseSpec.id = requestSpec.id;
        responseSpec.contentFileNum = hederaFile.fileNum;

        return responseSpec;
    }

    public static RequestSpec[] getPendingRequests() throws Exception {

        byte[] response = FileGetContents.getContents(_responseFile);

        Set<String> answered;

        if (response.length == 0) {
            answered = new HashSet<>();
        } else {
            answered =
                Arrays.stream(new String(response, StandardCharsets.UTF_8).split("\n"))
                    .map(st -> _gson.fromJson(st, ResponseSpec.class).id)
                    .collect(Collectors.toSet());
        }

        Thread.sleep(2000);

        byte[] contents = FileGetContents.getContents(_requestsFile);

        if (contents.length == 0) {
            return new RequestSpec[0];
        }

        RequestSpec[] requestSpecs =
            Arrays.stream(new String(contents, StandardCharsets.UTF_8).split("\n"))
                .map(st -> _gson.fromJson(st, RequestSpec.class))
                .filter(st -> !answered.contains(st.id))
                .toArray(RequestSpec[]::new);

        return requestSpecs;
    }
}
