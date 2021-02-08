package com.softwareverde.devtokens.webserver;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.concurrent.pool.ThreadPool;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.devtokens.configuration.BitcoinProperties;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.configuration.ServerProperties;
import com.softwareverde.devtokens.database.DatabaseManager;
import com.softwareverde.devtokens.database.DevTokensDatabase;
import com.softwareverde.devtokens.nodeapi.BitcoinDotComNodeConnection;
import com.softwareverde.devtokens.nodeapi.BitcoinDotComTransactionCache;
import com.softwareverde.devtokens.nodeapi.BitcoinDotComTransactionDatabaseCache;
import com.softwareverde.devtokens.nodeapi.NodeConnection;

public class Environment implements com.softwareverde.http.server.servlet.routed.Environment {
    protected final ServerProperties _serverProperties;
    protected final BitcoinProperties _bitcoinProperties;
    protected final RedemptionItemConfiguration _redemptionItemConfiguration;
    protected final ThreadPool _threadPool;
    protected final MasterInflater _masterInflater;
    protected final DevTokensDatabase _database;
    protected final Wallet _developerTokenWallet;
    protected final Wallet _spvWallet;
    protected final TokenExchangeCalculator _tokenExchangeCalculator;

    public Environment(final ServerProperties serverProperties, final BitcoinProperties bitcoinProperties, final RedemptionItemConfiguration redemptionItemConfiguration, final ThreadPool threadPool, final DevTokensDatabase devTokenDatabase, final MasterInflater masterInflater, final TokenExchangeCalculator tokenExchangeCalculator) {
        _serverProperties = serverProperties;
        _bitcoinProperties = bitcoinProperties;
        _redemptionItemConfiguration = redemptionItemConfiguration;
        _threadPool = threadPool;
        _masterInflater = masterInflater;
        _database = devTokenDatabase;

        final PrivateKey slpTokenPrivateKey = serverProperties.getSlpTokenPrivateKey();
        _developerTokenWallet = new Wallet();
        _developerTokenWallet.addPrivateKey(slpTokenPrivateKey);

        _spvWallet = new Wallet();

        _tokenExchangeCalculator = tokenExchangeCalculator;
    }

    public ServerProperties getServerProperties() {
        return _serverProperties;
    }

    public BitcoinProperties getBitcoinProperties() {
        return _bitcoinProperties;
    }

    public RedemptionItemConfiguration getRedemptionItemConfiguration() {
        return _redemptionItemConfiguration;
    }

    public ThreadPool getThreadPool() {
        return _threadPool;
    }

    public NodeConnection newNodeConnection() {
        final BitcoinDotComTransactionCache transactionCache = new BitcoinDotComTransactionDatabaseCache(this);
        return new BitcoinDotComNodeConnection(_masterInflater, transactionCache);
    }

    public MasterInflater getMasterInflater() {
        return _masterInflater;
    }

    public DatabaseManager newDatabaseManager() throws DatabaseException {
        final DatabaseConnection databaseConnection = _database.newConnection();
        return new DatabaseManager(databaseConnection, _masterInflater);
    }

    public SlpTokenId getSlpTokenId() {
        return _serverProperties.getSlpTokenId();
    }

    public PrivateKey getSlpTokenPrivateKey() {
        return _serverProperties.getSlpTokenPrivateKey();
    }

    public Address getDonationDestinationAddress() {
        return _serverProperties.getDestinationAddress();
    }

    public Wallet getDeveloperTokenWallet() {
        return _developerTokenWallet;
    }

    public Wallet getSpvWallet() {
        return _spvWallet;
    }

    public TokenExchangeCalculator getTokenExchangeCalculator() {
        return _tokenExchangeCalculator;
    }
}
