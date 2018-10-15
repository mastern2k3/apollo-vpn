package com.apollo.pubapi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.apollo.schema.RequestSpec;
import com.apollo.vpnbroker.BrokerLauncher;
import com.google.gson.Gson;
import com.hedera.file.FileAppend;
import com.hedera.file.FileCreate;
import com.hedera.sdk.account.HederaAccount;
import com.hedera.sdk.common.HederaKey;
import com.hedera.sdk.common.HederaTransactionAndQueryDefaults;
import com.hedera.sdk.cryptography.HederaCryptoKeyPair;
import com.hedera.sdk.file.HederaFile;
import com.hedera.sdk.transaction.HederaTransactionResult;
import com.hedera.utilities.ExampleUtilities;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicApiLauncher {

    static final Logger logger = LoggerFactory.getLogger(PublicApiLauncher.class);
    public static Gson _gson = new Gson();

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(7890), 0);

        server.createContext("/getfile", new GetFileHandler());
        server.createContext("/putfile", new PutFileHandler());
        server.createContext("/balance", new GetBalanceHandler());
        server.createContext("/req", new AppFileHandler());

        server.setExecutor(null);

        logger.info("public api starting at http://127.0.0.1:7890/balance");

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

    static class GetBalanceHandler extends BaseHandler {
        @Override
        public String handleToString(HttpExchange t) throws Exception {

            HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

            HederaAccount hederaAccount = new HederaAccount();

            hederaAccount.txQueryDefaults = qd;

            hederaAccount.setHederaAccountID(qd.payingAccountID);

            long balance = hederaAccount.getBalance();

            return "balance: " + String.valueOf(balance);
        }
    }

    static class GetFileHandler extends BaseHandler {
        @Override
        public String handleToString(HttpExchange t) throws Exception {

            String path = t.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf('/') + 1);

            long id = Long.parseLong(idStr);

            HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

            qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);
            HederaFile hederaFile;

            hederaFile = new HederaFile();

            hederaFile.txQueryDefaults = qd;
            hederaFile.fileNum = id;

            byte[] contents = hederaFile.getContents();

            return new String(contents, StandardCharsets.UTF_8);
        }
    }

    static class AppFileHandler extends BaseHandler {
        @Override
        public String handleToString(HttpExchange t) throws Exception {

            String path = t.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf('/') + 1);

            long id = Long.parseLong(idStr);

            HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

            qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);
            HederaFile hederaFile;

            hederaFile = new HederaFile();

            hederaFile.txQueryDefaults = qd;
            hederaFile.fileNum = id;

            hederaFile.getContents();

            Thread.sleep(2000);

//            HederaTransactionResult append = hederaFile.append(_gson.toJson(RequestSpec.of("https://api.myjson.com/bins/gwzhc")).getBytes(StandardCharsets.UTF_8));

            FileAppend.append(hederaFile, _gson.toJson(RequestSpec.of("https://api.myjson.com/bins/gwzhc")).getBytes(StandardCharsets.UTF_8));

            byte[] contents = hederaFile.getContents();

            return new String(contents, StandardCharsets.UTF_8);
        }
    }

    static class PutFileHandler extends BaseHandler {

        @Override
        public String handleToString(HttpExchange t) throws Exception {

            HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

            qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

            HederaFile hederaFile = new HederaFile();

            hederaFile.txQueryDefaults = qd;
            hederaFile.fileNum = 9090;
            hederaFile.expirationTime = Instant.now().plusSeconds(10);

            hederaFile = FileCreate.create(hederaFile, _gson.toJson(RequestSpec.of("https://api.myjson.com/bins/gwzhc")).getBytes(StandardCharsets.UTF_8));

            return "ok " + String.valueOf(hederaFile.fileNum);
        }
    }
}
