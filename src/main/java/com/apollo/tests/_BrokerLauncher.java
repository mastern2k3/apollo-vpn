package com.apollo.tests;

import com.apollo.vpnbroker.BrokerLauncher;
import com.hedera.file.FileCreate;
import com.hedera.file.FileGetContents;
import com.hedera.sdk.account.HederaAccount;
import com.hedera.sdk.common.HederaKey;
import com.hedera.sdk.common.HederaPrecheckResult;
import com.hedera.sdk.common.HederaTransactionAndQueryDefaults;
import com.hedera.sdk.cryptography.HederaCryptoKeyPair;
import com.hedera.sdk.file.HederaFile;
import com.hedera.utilities.ExampleUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class _BrokerLauncher {

    static final Logger logger = LoggerFactory.getLogger(_BrokerLauncher.class);

    public static void main(String[] args) throws Exception {

        byte[] bytes = "https://api.myjson.com/bins/gwzhc".getBytes(StandardCharsets.UTF_8);

        HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();

        qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

        HederaFile hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = qd;
        hederaFile.fileNum = 9090;

        long newFilename = 0;

        try {
            hederaFile = FileCreate.create(hederaFile, bytes);

            FileGetContents.getContents(hederaFile);

            logger.info("hederaFile.fileNum {}", hederaFile.fileNum);

            newFilename = hederaFile.fileNum;

        } catch (Exception ex) {
            logger.error("Error while getting file contents", ex);
        }

        hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = ExampleUtilities.getTxQueryDefaults();
        hederaFile.fileNum = newFilename;

        Thread.sleep(2000);

        hederaFile.append("appending stuff".getBytes(StandardCharsets.UTF_8));

        Thread.sleep(2000);

        byte[] contents = hederaFile.getContents();

        logger.info("contents: {}", new String(contents, StandardCharsets.UTF_8));
    }
}
