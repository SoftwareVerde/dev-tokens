package com.softwareverde.devtokens.configuration;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;

public class TokenGenesisConfiguration {
    protected String _tokenName;
    protected String _tokenAbbreviation;
    protected String _tokenUrl;
    protected Long _tokenCount;
    protected Address _batonAddress;
    protected PrivateKey _fundingPrivateKey;
    protected Transaction _fundingTransaction;

    public String getTokenName() {
        return _tokenName;
    }

    public String getTokenAbbreviation() {
        return _tokenAbbreviation;
    }

    public String getTokenUrl() {
        return _tokenUrl;
    }

    public Long getTokenCount() {
        return _tokenCount;
    }

    public Address getBatonAddress() {
        return _batonAddress;
    }

    public PrivateKey getFundingPrivateKey() {
        return _fundingPrivateKey;
    }

    public Transaction getFundingTransaction() {
        return _fundingTransaction;
    }
}
