package com.apollo.pubapi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.hedera.file.FileCreate;
import com.hedera.sdk.common.HederaKey;
import com.hedera.sdk.common.HederaTransactionAndQueryDefaults;
import com.hedera.sdk.cryptography.HederaCryptoKeyPair;
import com.hedera.sdk.file.HederaFile;
import com.hedera.utilities.ExampleUtilities;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class PublicApiLauncher {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(7890), 0);

        server.createContext("/getfile", new GetFileHandler());
        server.createContext("/putfile", new PutFileHandler());
        server.setExecutor(null);
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

    static class PutFileHandler extends BaseHandler {

        @Override
        public String handleToString(HttpExchange t) throws Exception {

            HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

            qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

            HederaFile hederaFile = new HederaFile();

            hederaFile.txQueryDefaults = qd;
            hederaFile.fileNum = 9090;
            hederaFile.expirationTime = Instant.now().plusSeconds(10);

            hederaFile = FileCreate.create(hederaFile, "lol wat the fuck".getBytes(StandardCharsets.UTF_8));

            return "ok " + String.valueOf(hederaFile.fileNum);
        }
    }
}
