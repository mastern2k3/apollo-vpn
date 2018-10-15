package com.apollo.vpnbroker;

import com.hedera.sdk.account.HederaAccount;

public class BrokerLauncher {

    public static void main(String[] args) {
        HederaAccount hederaAccount = new HederaAccount(123, 123, 123);

        System.out.print(hederaAccount);
    }
}
