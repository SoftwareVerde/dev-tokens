package com.softwareverde.devtokens.testing;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.concurrent.pool.ThreadPool;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.devtokens.configuration.BitcoinProperties;
import com.softwareverde.devtokens.configuration.ServerProperties;
import com.softwareverde.devtokens.database.DatabaseManager;
import com.softwareverde.devtokens.nodeapi.NodeConnection;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.TokenExchangeCalculator;

public class FakeEnvironment extends Environment {
    public FakeEnvironment() {
        super(
            new ServerProperties() {
                @Override
                public PrivateKey getSlpTokenPrivateKey() {
                    return PrivateKey.createNewKey();
                }
            },
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    @Override
    public ServerProperties getServerProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BitcoinProperties getBitcoinProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ThreadPool getThreadPool() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeConnection newNodeConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MasterInflater getMasterInflater() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DatabaseManager newDatabaseManager() throws DatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SlpTokenId getSlpTokenId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrivateKey getSlpTokenPrivateKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Address getDonationDestinationAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wallet getDeveloperTokenWallet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Wallet getSpvWallet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TokenExchangeCalculator getTokenExchangeCalculator() {
        throw new UnsupportedOperationException();
    }
}
