package com.softwareverde.devtokens;

import com.softwareverde.cryptography.hash.sha256.Sha256Hash;

public interface NewAnnouncementCallback {
    void onNewTransaction(Sha256Hash transactionHash);
    void onNewBlock(Sha256Hash blockHash);
}
