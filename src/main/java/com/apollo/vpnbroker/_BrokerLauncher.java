package com.apollo.vpnbroker;

import com.hedera.file.FileCreate;
import com.hedera.file.FileGetContents;
import com.hedera.sdk.account.HederaAccount;
import com.hedera.sdk.common.HederaKey;
import com.hedera.sdk.common.HederaPrecheckResult;
import com.hedera.sdk.common.HederaTransactionAndQueryDefaults;
import com.hedera.sdk.cryptography.HederaCryptoKeyPair;
import com.hedera.sdk.file.HederaFile;
import com.hedera.utilities.ExampleUtilities;

import java.nio.charset.StandardCharsets;

public class _BrokerLauncher {



    public static void main(String[] args) throws Exception {

//        DemoFile.main();


        byte[] bytes = "https://api.myjson.com/bins/gwzhc".getBytes(StandardCharsets.UTF_8);

        HederaTransactionAndQueryDefaults qd = ExampleUtilities.getTxQueryDefaults();
        HederaTransactionAndQueryDefaults fd = ExampleUtilities.getTxQueryDefaults();

        qd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);
        fd.fileWacl = new HederaCryptoKeyPair(HederaKey.KeyType.ED25519);

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

        FileGetContents.getContents(hederaFile);

        System.out.print("hederaFile.fileNum ");
        System.out.print(hederaFile.fileNum);

        hederaFile = new HederaFile();

        hederaFile.txQueryDefaults = qd;
        hederaFile.fileNum = 9099;

        HederaPrecheckResult precheckResult = hederaFile.getPrecheckResult();

        System.out.print("precheckResult ");
        System.out.print(precheckResult);

        byte[] contents = hederaFile.getContents();

        System.out.print("contents: ");
        System.out.print(new String(contents, StandardCharsets.UTF_8));
    }
}
