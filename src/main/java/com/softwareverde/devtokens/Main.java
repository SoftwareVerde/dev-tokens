package com.softwareverde.devtokens;

import com.softwareverde.bitcoin.CoreInflater;
import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.secp256k1.privatekey.PrivateKeyDeflater;
import com.softwareverde.bitcoin.server.configuration.NodeProperties;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.pool.DatabaseConnectionPool;
import com.softwareverde.bitcoin.server.module.spv.SpvModule;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.slp.SlpUtil;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionDeflater;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.bitcoin.transaction.output.identifier.TransactionOutputIdentifier;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.concurrent.pool.cached.CachedThreadPool;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.immutable.ImmutableList;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.hash.sha256.Sha256Hash;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.devtokens.configuration.BitcoinProperties;
import com.softwareverde.devtokens.configuration.Configuration;
import com.softwareverde.devtokens.configuration.DatabaseProperties;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.configuration.ServerProperties;
import com.softwareverde.devtokens.configuration.TokenGenesisConfiguration;
import com.softwareverde.devtokens.database.DatabaseManager;
import com.softwareverde.devtokens.database.DevTokensDatabase;
import com.softwareverde.devtokens.gmail.EmailClient;
import com.softwareverde.devtokens.nodeapi.AddressTransactionsResponse;
import com.softwareverde.devtokens.nodeapi.BitcoinDotComNodeConnection;
import com.softwareverde.devtokens.nodeapi.GetTransactionsResponse;
import com.softwareverde.devtokens.nodeapi.NodeConnection;
import com.softwareverde.devtokens.util.SlpTokenCreator;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.TokenExchangeCalculator;
import com.softwareverde.devtokens.webserver.WebServer;
import com.softwareverde.logging.LineNumberAnnotatedLog;
import com.softwareverde.logging.LogLevel;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main {
    protected static void exitFailure() {
        System.exit(1);
    }

    protected static void exitSuccess() {
        System.exit(0);
    }

    protected static void printError(final String errorMessage) {
        System.err.println(errorMessage);
    }

    protected static void printUsage() {
        Main.printError("To generate the token private key, run:");
        Main.printError("    java -jar " + System.getProperty("java.class.path") + " <configuration-file> INIT");
        Main.printError("");
        Main.printError("To mint the token and complete configuration, run:");
        Main.printError("    java -jar " + System.getProperty("java.class.path") + " <configuration-file> <redemption-items-file> GENESIS");
        Main.printError("");
        Main.printError("Once both are complete, to start the server, run:");
        Main.printError("    java -jar " + System.getProperty("java.class.path") + " <configuration-file> <redemption-items-file>");
        Main.printError("");
    }

    protected static DevTokensDatabase loadDatabase(final DatabaseProperties databaseProperties) {
        return DevTokensDatabase.newInstance(DevTokensDatabase.DEV_TOKENS, databaseProperties);
    }

    protected static Configuration loadConfigurationFile(final String configurationFilename, final String redemptionItemFilename, final MasterInflater masterInflater) {
        final File configurationFile =  new File(configurationFilename);
        if (! configurationFile.isFile()) {
            Main.printError("Invalid configuration file: " + configurationFilename);
            Main.exitFailure();
        }

        final File redemptionItemFile = new File(redemptionItemFilename);
        if (! redemptionItemFile.isFile()) {
            Main.printError("Invalid redemption configuration file: " + redemptionItemFilename);
            Main.exitFailure();
        }

        return new Configuration(configurationFile, redemptionItemFile, masterInflater);
    }

    protected static void init(final MasterInflater masterInflater, final String configurationFileName) {
        Logger.setLogLevel(LogLevel.ERROR);

        final PrivateKeyDeflater privateKeyDeflater = new PrivateKeyDeflater();
        final AddressInflater addressInflater = masterInflater.getAddressInflater();
        final TransactionDeflater transactionDeflater = masterInflater.getTransactionDeflater();

        System.out.println("Generating private key...");
        final PrivateKey privateKey = PrivateKey.createNewKey();
        final String privateKeyString = privateKeyDeflater.toWalletImportFormat(privateKey, true);
        final Address address = addressInflater.fromPrivateKey(privateKey, true);

        System.out.println("Private Key: " + privateKeyString);
        System.out.println("Address: " + address.toBase32CheckEncoded());

        System.out.println();
        System.out.println("Send at least 2048 satoshis (10k+ recommended) to " + address.toBase58CheckEncoded() + ", then press enter.");

        Transaction transaction = null;
        do {
            try { System.in.read(); } catch (final Exception exception) { }

            try (final BitcoinDotComNodeConnection nodeConnection = new BitcoinDotComNodeConnection(masterInflater, null)) {
                final AddressTransactionsResponse addressTransactionsResponse = nodeConnection.getSpendableTransactions(address);
                final List<Sha256Hash> transactionHashes = addressTransactionsResponse.getTransactionHashes();
                if (transactionHashes.isEmpty()) {
                    System.out.println("No transactions found yet. Press enter to try again.");
                    continue;
                }

                final GetTransactionsResponse getTransactionsResponse = nodeConnection.getTransactions(transactionHashes);
                final Map<Sha256Hash, Transaction> transactions = getTransactionsResponse.getTransactions();

                final Sha256Hash transactionHash = transactionHashes.get(0);
                transaction = transactions.get(transactionHash);

                if (transaction == null) {
                    System.out.println("Unable to load transaction: " + transactionHash);
                }
            }
        } while (transaction == null);

        System.out.println("Preparation for generating the token is complete.");
        System.out.println("Update " + configurationFileName + " with the following settings:");

        final ByteArray transactionBytes = transactionDeflater.toBytes(transaction);
        System.out.println();
        System.out.println("    genesisToken.fundingPrivateKey = " + privateKeyString);
        System.out.println("    genesisToken.fundingTransaction = " + transactionBytes);
        System.out.println();

        System.out.println("Once done, run this application again with the GENESIS flag (\"./run-genesis.sh\") to create the token.");
        Main.exitSuccess();
    }

    protected static void createSlpToken(final MasterInflater masterInflater, final String configurationFileName, final Configuration configuration) {
        Logger.setLogLevel(LogLevel.ERROR);
        Logger.setLogLevel("com.softwareverde.bitcoin.wallet", LogLevel.ERROR);

        final PrivateKeyDeflater privateKeyDeflater = new PrivateKeyDeflater();

        System.out.println("Creating SLP dev token...");
        final SlpTokenCreator slpTokenCreator = new SlpTokenCreator(masterInflater);
        final TokenGenesisConfiguration tokenGenesisConfiguration = configuration.getTokenGenesisConfiguration();
        final Transaction transaction = slpTokenCreator.createSlpToken(tokenGenesisConfiguration);
        if (transaction == null) {
            Logger.info("Unable to create token.");
            Main.exitFailure();
        }

        try (final BitcoinDotComNodeConnection nodeConnection = new BitcoinDotComNodeConnection(masterInflater, null)) {
            nodeConnection.submitTransaction(transaction);
        }

        final PrivateKey privateKey = tokenGenesisConfiguration.getFundingPrivateKey();
        final String privateKeyString = privateKeyDeflater.toWalletImportFormat(privateKey, true);

        System.out.println("Token created.");
        System.out.println("Update " + configurationFileName + " with the following settings:");
        System.out.println();
        System.out.println("    server.slpTokenId = " + transaction.getHash());
        System.out.println("    server.slpTokenPrivateKey = " + privateKeyString);
        System.out.println();

        System.out.println("Be sure you've also set the donation destination address.");
        System.out.println("Once done, run this application (\"./run.sh\") to start the server.");
        Main.exitSuccess();
    }

    protected static List<Transaction> getSpendableTransactions(final Address address, final Environment environment) {
        return Main.getSpendableTransactions(new ImmutableList<Address>(address), environment);
    }

    protected static List<Transaction> getSpendableTransactions(final List<Address> addresses, final Environment environment) {
        final AddressTransactionsResponse addressTransactionsResponse;
        try (final NodeConnection nodeConnection = environment.newNodeConnection()) {
            addressTransactionsResponse = nodeConnection.getSpendableTransactions(addresses);
            if (! addressTransactionsResponse.wasSuccessful()) {
                Logger.error("Unable to load Address Transactions for Addresses.");
                return null;
            }
        }

        final List<Sha256Hash> transactionHashes = addressTransactionsResponse.getTransactionHashes();
        final Map<Sha256Hash, Transaction> transactions;
        try (final NodeConnection nodeConnection = environment.newNodeConnection()) {
            final GetTransactionsResponse getTransactionsResponse = nodeConnection.getTransactions(transactionHashes);

            if (! getTransactionsResponse.wasSuccessful()) {
                Logger.error("Unable to load Transactions: " + getTransactionsResponse.getErrorMessage());
                return null;
            }

            transactions = getTransactionsResponse.getTransactions();
        }

        final MutableList<Transaction> transactionsList = new MutableList<Transaction>(transactionHashes.getCount());
        for (final Sha256Hash transactionHash : transactionHashes) {
            final Transaction transaction = transactions.get(transactionHash);
            if (transaction == null) {
                Logger.warn(transactionHash + " does not exist.");
            }

            transactionsList.add(transaction);
        }
        return transactionsList;
    }

    public static void main(final String[] commandLineArguments) {
        Logger.setLog(LineNumberAnnotatedLog.getInstance());
        Logger.setLogLevel(LogLevel.DEBUG);
        Logger.setLogLevel("com.softwareverde.util", LogLevel.ERROR);
        Logger.setLogLevel("com.softwareverde.bitcoin.jni", LogLevel.ERROR);
        Logger.setLogLevel("com.softwareverde.network.socket.Socket", LogLevel.INFO);
        Logger.setLogLevel("com.softwareverde.network", LogLevel.INFO);
        Logger.setLogLevel("com.softwareverde.async.lock.IndexLock", LogLevel.WARN);
        Logger.setLogLevel("com.softwareverde.bitcoin.server.module.node.manager", LogLevel.INFO);

        if (commandLineArguments.length < 2) {
            Main.printUsage();
            Main.exitFailure();
            return;
        }

        final MasterInflater masterInflater = new CoreInflater();
        final String configurationFilename = commandLineArguments[0];

        if (Util.areEqual(commandLineArguments[1].toLowerCase(), "init")) {
            Main.init(masterInflater, configurationFilename);
            return;
        }

        if (commandLineArguments.length < 3) {
            Main.printUsage();
            Main.exitFailure();
            return;
        }

        final String redemptionItemsConfigurationFilename = commandLineArguments[1];
        final Configuration configuration = Main.loadConfigurationFile(configurationFilename, redemptionItemsConfigurationFilename, masterInflater);

        if (Util.areEqual(commandLineArguments[2].toLowerCase(), "genesis")) {
            Main.createSlpToken(masterInflater, configurationFilename, configuration);
            return;
        }
        else if (! Util.areEqual(commandLineArguments[2].toLowerCase(), "server")) {
            Main.printUsage();
            Main.exitFailure();
            return;
        }

        final ServerProperties serverProperties = configuration.getServerProperties();
        if (serverProperties.getDestinationAddress() == null) {
            Main.printError("Invalid or missing destination address in configuration.");
            Main.exitFailure();
            return;
        }

        Logger.info("[Starting Database]");
        final DevTokensDatabase database = Main.loadDatabase(configuration.getDatabaseProperties());
        final BitcoinProperties bitcoinProperties = configuration.getBitcoinProperties();
        final RedemptionItemConfiguration redemptionItemConfiguration = configuration.getRedemptionItemConfiguration();

        final Double dollarsPerToken = serverProperties.getDollarsPerToken();
        final TokenExchangeCalculator tokenExchangeCalculator = new TokenExchangeCalculatorCore(redemptionItemConfiguration, dollarsPerToken);

        final CachedThreadPool threadPool = new CachedThreadPool(64, 1000L);
        threadPool.start();

        {
            if (serverProperties.getSlpTokenId() == null) {
                Logger.error("Invalid SlpTokenId in configuration.");
                Main.exitFailure();
            }

            if (serverProperties.getSlpTokenPrivateKey() == null) {
                Logger.error("Invalid SlpTokenId PrivateKey in configuration.");
                Main.exitFailure();
            }

            if (serverProperties.getDestinationAddress() == null) {
                Logger.error("Invalid donation destination Address in configuration.");
                Main.exitFailure();
            }
        }

        final Environment environment = new Environment(serverProperties, bitcoinProperties, redemptionItemConfiguration, threadPool, database, masterInflater, tokenExchangeCalculator);

        final Wallet developerTokenWallet = environment.getDeveloperTokenWallet();
        final Wallet spvWallet = environment.getSpvWallet();

        try (final DatabaseManager databaseManager = environment.newDatabaseManager()) { // Save any static donation PrivateKeys
            final AddressInflater addressInflater = masterInflater.getAddressInflater();
            for (final PrivateKey privateKey : serverProperties.getStaticDonationPrivateKeys()) {
                final Address compressedAddress = addressInflater.fromPrivateKey(privateKey, true);
                final Address decompressedAddress = addressInflater.fromPrivateKey(privateKey, false);

                databaseManager.storeDonationAddress(privateKey, compressedAddress, null);
                databaseManager.storeDonationAddress(privateKey, decompressedAddress, null);
            }

            for (final Address address : serverProperties.getStaticDonationWatchedAddresses()) {
                databaseManager.storeDonationAddress(null, address, null);
            }
        }
        catch (final DatabaseException exception) {
            Logger.error(exception);
            Main.exitFailure();
            return;
        }

        // Add all previous private keys to the SPV wallet...
        try (final DatabaseManager databaseManager = environment.newDatabaseManager()) {
            { // Donation PrivateKeys
                final List<Address> donationAddresses = databaseManager.getDonationAddresses();
                for (final Address address : donationAddresses) {
                    final PrivateKey privateKey = databaseManager.getPrivateKey(address);
                    if (privateKey != null) {
                        spvWallet.addPrivateKey(privateKey);
                    }
                    else {
                        spvWallet.addWatchedAddress(address);
                    }
                }
            }

            { // Redemption PrivateKeys
                final List<Address> redemptionAddresses = databaseManager.getRedemptionAddresses();
                for (final Address address : redemptionAddresses) {
                    final PrivateKey privateKey = databaseManager.getPrivateKey(address);
                    if (privateKey != null) {
                        spvWallet.addPrivateKey(privateKey);
                    }
                }
            }
        }
        catch (final DatabaseException exception) {
            Logger.error(exception);
            Main.exitFailure();
            return;
        }

        final Thread spvThread;
        final SpvModule spvModule;
        {
            Logger.info("[Starting SPV Module]");
            final List<NodeProperties> seedNodeProperties = bitcoinProperties.getSeedNodeProperties();
            final DatabaseConnectionPool databaseConnectionPool = new DatabaseConnectionPool() {
                @Override
                public DatabaseConnection newConnection() throws DatabaseException {
                    return database.newConnection();
                }

                @Override
                public void close() throws DatabaseException { }
            };

            final com.softwareverde.bitcoin.server.Environment serverEnvironment = new com.softwareverde.bitcoin.server.Environment(database, databaseConnectionPool);
            spvModule = new SpvModule(serverEnvironment, seedNodeProperties, 8, spvWallet);
            spvThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    spvModule.initialize();
                    spvModule.loop();
                }
            });
        }

        {
            Logger.info("[Updating Wallet]");
            final AddressInflater addressInflater = masterInflater.getAddressInflater();

            final PrivateKey privateKey = environment.getSlpTokenPrivateKey();
            final SlpTokenId slpTokenId = environment.getSlpTokenId();
            final Address fundingAddress = addressInflater.fromPrivateKey(privateKey, true);

            final List<Transaction> fundingAddressTransactions = Main.getSpendableTransactions(fundingAddress, environment);
            if (fundingAddressTransactions == null) { Main.exitFailure(); }
            for (final Transaction transaction : fundingAddressTransactions) {
                developerTokenWallet.addTransaction(transaction);
            }

            final Long balanceInSatoshis = developerTokenWallet.getBalance();
            final Long tokenBalance = developerTokenWallet.getSlpTokenBalance(slpTokenId);
            final Double dollarsPerBitcoin = tokenExchangeCalculator.getDollarsPerBitcoin();
            if (dollarsPerBitcoin == null) {
                Logger.error("Unable to load Bitcoin price: " + dollarsPerBitcoin);
                Main.exitFailure();
            }
            final Double satoshisPerDollar = (Transaction.SATOSHIS_PER_BITCOIN / dollarsPerBitcoin);
            final Long satoshisPerToken = tokenExchangeCalculator.getSatoshisPerToken();

            Logger.info("       BCH Price: " + dollarsPerBitcoin);
            Logger.info("     Token Price: " + satoshisPerToken + " ($" + StringUtil.formatPercent((float) (satoshisPerToken / satoshisPerDollar), false) + ")");
            Logger.info("  Wallet Balance: " + balanceInSatoshis + " BCH (sats) ($" + StringUtil.formatPercent((float) (balanceInSatoshis / satoshisPerDollar), false) + ")");
            Logger.info("   Token Balance: " + tokenBalance + " SLP");

            final long minBalance = (developerTokenWallet.getDustThreshold(true) * 2L);
            final long minTokenBalance = 1L;
            if ((balanceInSatoshis < minBalance) || (tokenBalance < minTokenBalance)) {
                Logger.error("Low wallet balance.");
                Main.exitFailure();
            }

            try (final DatabaseManager databaseManager = environment.newDatabaseManager()) {
                final List<Address> donationAddresses = databaseManager.getDonationAddresses();
                final List<Transaction> addressTransactions = Main.getSpendableTransactions(donationAddresses, environment);
                if (addressTransactions == null) { Main.exitFailure(); }
                for (final Transaction transaction : addressTransactions) {
                    spvWallet.addTransaction(transaction);
                }
            }
            catch (final Exception exception) {
                Logger.warn("Unable to load initial donation address transactions.");
            }
        }

        final EmailClient emailClient;
        {
            emailClient = new EmailClient(environment);
        }

        final TokenService tokenService;
        {
            Logger.info("[Starting Token Service]");
            tokenService = new TokenService(environment, tokenExchangeCalculator, emailClient);
            tokenService.start();
            tokenService.wakeUp();
        }

        final WebServer webServer = new WebServer(environment, new NewAddressCreatedCallback() {
            @Override
            public void newAddressCreated(final Address address) {
                spvModule.onWalletKeysUpdated();
            }
        });

        spvModule.setNewTransactionCallback(new SpvModule.NewTransactionCallback() {
            @Override
            public void onNewTransactionReceived(final Transaction transaction) {
                final Sha256Hash transactionHash = transaction.getHash();
                Logger.debug("New Transaction: " + transactionHash);

                final Map<TransactionOutputIdentifier, Long> slpOutputAmounts = new HashMap<TransactionOutputIdentifier, Long>();
                final SlpTokenId slpTokenId = environment.getSlpTokenId();
                if (Transaction.isSlpTransaction(transaction)) {
                    final SlpTokenId transactionSlpTokenId = SlpUtil.getTokenId(transaction);
                    if (Util.areEqual(slpTokenId, transactionSlpTokenId)) {
                        final List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
                        for (int i = 0; i < transactionOutputs.getCount(); ++i) {
                            final Long tokenAmount = SlpUtil.getOutputTokenAmount(transaction, i);
                            if (tokenAmount > 0) {
                                final TransactionOutputIdentifier transactionOutputIdentifier = new TransactionOutputIdentifier(transactionHash, i);
                                slpOutputAmounts.put(transactionOutputIdentifier, tokenAmount);
                            }
                        }
                    }
                }

                webServer.onNewTransaction(transaction, slpOutputAmounts);
                tokenService.onNewTransaction(transactionHash, slpOutputAmounts);
                tokenService.wakeUp();
            }
        });

        Logger.info("[Starting Web Server]");
        webServer.start();

        Logger.info("[Starting SPV Wallet]");
        spvThread.start();

        while (true) {
            try { Thread.sleep(1000); } catch (final Exception exception) { break; }
        }

        Logger.info("[Shutting Down]");
        spvThread.interrupt();
        webServer.stop();
        tokenService.stop();
        threadPool.stop();

        try { spvThread.join(); } catch (final Exception exception) { }
    }
}
