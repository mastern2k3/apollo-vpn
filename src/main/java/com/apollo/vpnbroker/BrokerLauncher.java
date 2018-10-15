package com.apollo.vpnbroker;

import com.hedera.file.DemoFile;
import com.hedera.file.FileCreate;
import com.hedera.sdk.account.HederaAccount;
import com.hedera.sdk.common.HederaFileID;
import com.hedera.sdk.common.HederaKey;
import com.hedera.sdk.common.HederaTransactionAndQueryDefaults;
import com.hedera.sdk.cryptography.HederaCryptoKeyPair;
import com.hedera.sdk.file.HederaFile;
import com.hedera.utilities.ExampleUtilities;
import com.hederahashgraph.api.proto.java.FileID;

import java.nio.charset.StandardCharsets;

public class BrokerLauncher {



    public static void main(String[] args) throws Exception {

//        DemoFile.main();


        byte[] bytes = "https://api.myjson.com/bins/gwzhc".getBytes(StandardCharsets.UTF_8);

        HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();
        qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

        System.out.print(qd.payingAccountID);

        HederaAccount hederaAccount = new HederaAccount();

        hederaAccount.txQueryDefaults = qd;

        hederaAccount.setHederaAccountID(qd.payingAccountID);

        long balance = hederaAccount.getBalance();

        System.out.print("balance ");
        System.out.print(balance);

        HederaFile hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = qd;
        hederaFile.fileNum = 9090;

        try {
            hederaFile = FileCreate.create(hederaFile, bytes);
        } catch (Exception ex) {
            System.out.print(ex);
        }

        System.out.print("hederaFile.fileNum ");
        System.out.print(hederaFile.fileNum);
    }
}
