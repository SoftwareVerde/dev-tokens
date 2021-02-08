package com.softwareverde.devtokens.configuration;

import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.secp256k1.privatekey.PrivateKeyInflater;
import com.softwareverde.bitcoin.transaction.TransactionInflater;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.util.Util;

import java.util.Properties;

public class TokenGenesisConfigurationLoader {
    protected final MasterInflater _masterInflater;

    public TokenGenesisConfigurationLoader(final MasterInflater masterInflater) {
        _masterInflater = masterInflater;
    }

    public TokenGenesisConfiguration loadTokenGenesisConfiguration(final Properties properties) {
        final PrivateKeyInflater privateKeyInflater = new PrivateKeyInflater();
        final AddressInflater addressInflater = _masterInflater.getAddressInflater();
        final TransactionInflater transactionInflater = _masterInflater.getTransactionInflater();

        final TokenGenesisConfiguration tokenGenesisConfiguration = new TokenGenesisConfiguration();

        tokenGenesisConfiguration._tokenName = properties.getProperty("genesisToken.tokenName", "");
        tokenGenesisConfiguration._tokenAbbreviation = properties.getProperty("genesisToken.tokenAbbreviation", "");
        tokenGenesisConfiguration._tokenCount = Util.parseLong(properties.getProperty("genesisToken.tokenCount", ""));
        tokenGenesisConfiguration._tokenUrl = properties.getProperty("genesisToken.tokenUrl", "");

        final String batonAddressString = properties.getProperty("genesisToken.batonAddress", "");
        tokenGenesisConfiguration._batonAddress = Util.coalesce(addressInflater.fromBase58Check(batonAddressString), addressInflater.fromBase32Check(batonAddressString));

        final String privateKeyString = properties.getProperty("genesisToken.fundingPrivateKey", "");
        tokenGenesisConfiguration._fundingPrivateKey = Util.coalesce(PrivateKey.fromHexString(privateKeyString), privateKeyInflater.fromWalletImportFormat(privateKeyString));

        final String transactionDataString = properties.getProperty("genesisToken.fundingTransaction", "");
        tokenGenesisConfiguration._fundingTransaction = transactionInflater.fromBytes(ByteArray.fromHexString(transactionDataString));

        return tokenGenesisConfiguration;
    }
}
