package com.softwareverde.devtokens;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionDeflater;
import com.softwareverde.bitcoin.transaction.input.TransactionInput;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.bitcoin.transaction.output.identifier.TransactionOutputIdentifier;
import com.softwareverde.bitcoin.transaction.script.slp.send.SlpSendScript;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.bitcoin.wallet.slp.SlpPaymentAmount;
import com.softwareverde.bitcoin.wallet.utxo.SpendableTransactionOutput;
import com.softwareverde.bitcoindotcom.Utxo;
import com.softwareverde.concurrent.service.SleepyService;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.hash.sha256.Sha256Hash;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.devtokens.database.DatabaseManager;
import com.softwareverde.devtokens.gmail.EmailClient;
import com.softwareverde.devtokens.nodeapi.AddressTransactionsResponse;
import com.softwareverde.devtokens.nodeapi.GetTransactionsResponse;
import com.softwareverde.devtokens.nodeapi.NodeConnection;
import com.softwareverde.devtokens.nodeapi.SubmitTransactionResponse;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.TokenExchangeCalculator;
import com.softwareverde.json.Json;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenService extends SleepyService {
    protected final Environment _environment;
    protected final TokenExchangeCalculator _tokenExchangeCalculator;
    protected final EmailClient _emailClient;

    protected final ConcurrentHashMap<Sha256Hash, Integer> _newTransactions = new ConcurrentHashMap<Sha256Hash, Integer>();

    public static void loadWalletWithUnspentTransactionOutputs(final Wallet wallet, final Address address, final DatabaseManager databaseManager, final NodeConnection nodeConnection) throws DatabaseException {
        final AddressTransactionsResponse addressTransactionsResponse = nodeConnection.getSpendableTransactions(address);
        final GetTransactionsResponse getTransactionsResponse = nodeConnection.getTransactions(addressTransactionsResponse.getTransactionHashes());
        final Map<Sha256Hash, Transaction> transactions = getTransactionsResponse.getTransactions();

        final List<Utxo> utxos = addressTransactionsResponse.getUtxos();
        final HashSet<TransactionOutputIdentifier> unspentTransactionOutputIdentifiers = new HashSet<TransactionOutputIdentifier>();
        for (final Utxo utxo : utxos) {
            final Sha256Hash transactionHash = utxo.getTransactionHash();
            final Integer outputIndex = utxo.getOutputIndex();

            final TransactionOutputIdentifier transactionOutputIdentifier = new TransactionOutputIdentifier(transactionHash, outputIndex);
            unspentTransactionOutputIdentifiers.add(transactionOutputIdentifier);
        }

        for (final Sha256Hash transactionHash : transactions.keySet()) {
            final Boolean isAlreadyRedeemed = databaseManager.wasTransactionProcessed(transactionHash);
            if (isAlreadyRedeemed) { continue; }

            final Transaction transaction = transactions.get(transactionHash);
            wallet.addTransaction(transaction);

            final List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
            for (int outputIndex = 0; outputIndex < transactionOutputs.getCount(); ++outputIndex) {
                final TransactionOutputIdentifier transactionOutputIdentifier = new TransactionOutputIdentifier(transactionHash, outputIndex);
                if (! unspentTransactionOutputIdentifiers.contains(transactionOutputIdentifier)) {
                    wallet.markTransactionOutputAsSpent(transactionHash, outputIndex);
                }
            }
        }
    }

    protected Boolean _broadcastTransaction(final Transaction transaction) {
        try (final NodeConnection nodeConnection = _environment.newNodeConnection()) {
            final SubmitTransactionResponse submitTransactionResponse = nodeConnection.submitTransaction(transaction);

            if (! submitTransactionResponse.wasSuccessful()) {
                final MasterInflater masterInflater = _environment.getMasterInflater();
                final TransactionDeflater transactionDeflater = masterInflater.getTransactionDeflater();
                final ByteArray transactionBytes = transactionDeflater.toBytes(transaction);
                Logger.warn("Unable to send Transaction: " + transactionBytes);

                return false;
            }

            return true;
        }
    }

    protected static class DistributionResult {
        public static final DistributionResult FAILURE = new DistributionResult(false, null);
        public static final DistributionResult INSUFFICIENT_AMOUNT = new DistributionResult(true, null);
        public static DistributionResult success(final List<Transaction> transactions) {
            return new DistributionResult(true, transactions);
        }

        public final Boolean wasProcessedSuccessfully;
        public final List<Transaction> distributionTransactions;

        public DistributionResult(final Boolean wasSuccessful, final List<Transaction> transactions) {
            this.wasProcessedSuccessfully = wasSuccessful;
            this.distributionTransactions = transactions;
        }
    }

    protected DistributionResult _claimTransactionAndDistributeTokens(final Transaction transaction, final Address donationAddress, final PrivateKey privateKey, final Map<Address, Double> tokenDestinationAddressPercentages) {
        final int maxOutputCount = (SlpSendScript.MAX_OUTPUT_COUNT - 2); // Excessively go under the max output count in order to ensure there is room for change addresses and non-sponsorship change.

        final Wallet wallet = new Wallet();

        if (privateKey != null) { // Can be null if `transaction` sends to a watched addresses whose SLP distributions are sponsored by the dev-tokens app.
            wallet.addPrivateKey(privateKey);
        }
        else if (donationAddress != null) {
            wallet.addWatchedAddress(donationAddress);
        }
        else {
            Logger.warn("Both DonationAddress and PrivateKey are null, cannot distribute tokens.");
            return DistributionResult.FAILURE;
        }

        wallet.addTransaction(transaction);
        final List<SpendableTransactionOutput> spendableDonationOutputs = wallet.getTransactionOutputs();

        final Wallet mainWallet = _environment.getDeveloperTokenWallet();
        final SlpTokenId slpTokenId = _environment.getSlpTokenId();
        final PrivateKey slpTokenPrivateKey = _environment.getSlpTokenPrivateKey();
        final Address donationDestinationAddress = _environment.getDonationDestinationAddress();

        final boolean isSponsoredDistribution = (privateKey == null); // True if the transaction cost is being funded by the DevToken/main wallet.
        final int devTokenWalletOutputCount = (isSponsoredDistribution ? 1 : 0);

        final Long dustAmount = wallet.getDustThreshold(true);

        final long netDonationAmount;
        final long totalTokenAmount;
        final long transactionCostEstimate;
        {
            final long bytesPerOutput = 34L;
            final long outputCount = (2L + tokenDestinationAddressPercentages.size() + devTokenWalletOutputCount);
            final long newTransactionEstimatedCount = ((outputCount + maxOutputCount - 1L) / maxOutputCount); // Round up...
            transactionCostEstimate = ((500L + (outputCount * bytesPerOutput)) * newTransactionEstimatedCount); // The estimated cost to submit the transaction.
            final Long satoshisPerToken = _tokenExchangeCalculator.getSatoshisPerToken();
            if (satoshisPerToken == null) { return DistributionResult.FAILURE; }

            final Long donatedAmount = (isSponsoredDistribution ? wallet.getWatchedBalance(donationAddress) : wallet.getBalance());

            totalTokenAmount = (donatedAmount / satoshisPerToken);
            netDonationAmount = (donatedAmount - (isSponsoredDistribution ? (transactionCostEstimate * 2L) : transactionCostEstimate));
        }
        if (totalTokenAmount < 1L) { return DistributionResult.INSUFFICIENT_AMOUNT; }
        if (netDonationAmount < dustAmount) {
            final Sha256Hash transactionHash = transaction.getHash();
            Logger.info("Transaction " + transactionHash + " net donation amount below dust threshold. Consider increasing satoshisPerToken requirement.");
            return DistributionResult.INSUFFICIENT_AMOUNT;
        }

        wallet.addPrivateKey(slpTokenPrivateKey);
        final List<SpendableTransactionOutput> spendableTokenTransactionOutputs = mainWallet.getTransactionOutputsAndSpendableTokens(slpTokenId, true);
        for (final SpendableTransactionOutput spendableTransactionOutput : spendableTokenTransactionOutputs) {
            final TransactionOutputIdentifier transactionOutputIdentifier = spendableTransactionOutput.getIdentifier();
            final Transaction spendableTokenTransaction = mainWallet.getTransaction(transactionOutputIdentifier.getTransactionHash());
            wallet.addTransaction(spendableTokenTransaction);
        }

        final MutableList<MutableList<SlpPaymentAmount>> paymentAmountsArray = new MutableList<>(1);
        { // Add SLP Payout Amounts/Outputs.
            MutableList<SlpPaymentAmount> paymentAmounts = new MutableList<>();

            if (! isSponsoredDistribution) { // Otherwise, rely on the change address to return funds to the main wallet...
                paymentAmounts.add(new SlpPaymentAmount(donationDestinationAddress, netDonationAmount, 0L));
            }

            int i = 0;
            long runningTotalPayoutAmount = 0L;
            for (final Address tokenDestinationAddress : tokenDestinationAddressPercentages.keySet()) {
                final boolean isLastDestination = ((i + 1) == tokenDestinationAddressPercentages.size());

                final long tokenAmount;
                if (isLastDestination) {
                    tokenAmount = (totalTokenAmount - runningTotalPayoutAmount); // In the case of rounding excess, give the last output the remaining tokens.
                }
                else {
                    final Double percentage = tokenDestinationAddressPercentages.get(tokenDestinationAddress);
                    tokenAmount = (long) (totalTokenAmount * percentage);
                }

                if (tokenAmount > 0L) {
                    runningTotalPayoutAmount += tokenAmount;
                    paymentAmounts.add(new SlpPaymentAmount(tokenDestinationAddress, dustAmount, tokenAmount));
                }

                i += 1;

                if (Util.areEqual(maxOutputCount, paymentAmounts.getCount())) {
                    paymentAmountsArray.add(paymentAmounts);
                    paymentAmounts = new MutableList<>();
                }
            }

            if (! paymentAmounts.isEmpty()) {
                paymentAmountsArray.add(paymentAmounts);
            }
        }

        { // Ensure all total SLP payment amount matches the intended amount...
            long summedSlpTokenAmount = 0L;
            for (final List<SlpPaymentAmount> paymentAmounts : paymentAmountsArray) {
                for (final SlpPaymentAmount slpPaymentAmount : paymentAmounts) {
                    summedSlpTokenAmount += slpPaymentAmount.tokenAmount;
                }
            }

            if (summedSlpTokenAmount != totalTokenAmount) {
                Logger.warn("Error calculating payout amounts, total payout amount does not equal calculated amount. (" + summedSlpTokenAmount + " != " + totalTokenAmount + ")");
                return DistributionResult.FAILURE;
            }
        }

        final MutableList<Transaction> newTransactions = new MutableList<>();
        for (final List<SlpPaymentAmount> paymentAmounts : paymentAmountsArray) {
            final List<TransactionOutputIdentifier> requiredTransactionOutputIdentifiers;
            {
                final MutableList<TransactionOutputIdentifier> transactionOutputIdentifiers = new MutableList<TransactionOutputIdentifier>(spendableDonationOutputs.getCount());
                for (final SpendableTransactionOutput spendableTransactionOutput : spendableDonationOutputs) {
                    transactionOutputIdentifiers.add(spendableTransactionOutput.getIdentifier());
                }
                requiredTransactionOutputIdentifiers = transactionOutputIdentifiers;
            }

            final Address mainWalletChangeAddress = mainWallet.getReceivingAddress();
            final Transaction newTransaction = wallet.createSlpTokenTransaction(slpTokenId, paymentAmounts, mainWalletChangeAddress, requiredTransactionOutputIdentifiers);
            if (newTransaction == null) { return DistributionResult.FAILURE; }

            newTransactions.add(newTransaction);
            wallet.addTransaction(newTransaction);
        }

        return DistributionResult.success(newTransactions);
    }

    protected Boolean _reclaimTokens(final PrivateKey redemptionAddressPrivateKey, final List<Transaction> redemptionAddressTransactions) {
        final SlpTokenId slpTokenId = _environment.getSlpTokenId();

        final Wallet wallet = new Wallet();
        wallet.addPrivateKey(redemptionAddressPrivateKey);
        for (final Transaction transaction : redemptionAddressTransactions) {
            wallet.addTransaction(transaction);
        }
        final List<SpendableTransactionOutput> spendableDonationOutputs = wallet.getTransactionOutputs();

        final Long dustAmount = wallet.getDustThreshold(true);
        final Long redeemedTokensCount = wallet.getSlpTokenBalance(slpTokenId);

        final Wallet mainWallet = _environment.getDeveloperTokenWallet();
        final PrivateKey slpTokenPrivateKey = _environment.getSlpTokenPrivateKey();

        wallet.addPrivateKey(slpTokenPrivateKey);
        final List<SpendableTransactionOutput> spendableTokenTransactionOutputs = mainWallet.getTransactionOutputsAndSpendableTokens(slpTokenId, true);
        for (final SpendableTransactionOutput spendableTransactionOutput : spendableTokenTransactionOutputs) {
            final TransactionOutputIdentifier transactionOutputIdentifier = spendableTransactionOutput.getIdentifier();
            final Transaction spendableTokenTransaction = mainWallet.getTransaction(transactionOutputIdentifier.getTransactionHash());
            wallet.addTransaction(spendableTokenTransaction);
        }

        final Address mainWalletAddress = mainWallet.getReceivingAddress();
        final MutableList<SlpPaymentAmount> paymentAmounts = new MutableList<SlpPaymentAmount>(1);
        paymentAmounts.add(new SlpPaymentAmount(mainWalletAddress, dustAmount, redeemedTokensCount));

        final List<TransactionOutputIdentifier> requiredTransactionOutputIdentifiers;
        {
            final MutableList<TransactionOutputIdentifier> transactionOutputIdentifiers = new MutableList<TransactionOutputIdentifier>(spendableDonationOutputs.getCount());
            for (final SpendableTransactionOutput spendableTransactionOutput : spendableDonationOutputs) {
                transactionOutputIdentifiers.add(spendableTransactionOutput.getIdentifier());
            }
            requiredTransactionOutputIdentifiers = transactionOutputIdentifiers;
        }

        final Transaction transaction = wallet.createSlpTokenTransaction(slpTokenId, paymentAmounts, mainWalletAddress, requiredTransactionOutputIdentifiers);
        if (transaction == null) { return false; }

        final Boolean broadcastWasSuccessful = _broadcastTransaction(transaction);
        if (broadcastWasSuccessful) {
            mainWallet.addTransaction(transaction);
        }
        return broadcastWasSuccessful;
    }

    protected void _processDonationAddresses() throws DatabaseException {
        try (final DatabaseManager databaseManager = _environment.newDatabaseManager()) {
            final List<Address> donationAddresses = databaseManager.getDonationAddresses();

            final List<Sha256Hash> transactionHashes;
            final AddressTransactionsResponse addressTransactionsResponse;
            try (final NodeConnection nodeConnection = _environment.newNodeConnection()) {
                addressTransactionsResponse = nodeConnection.getSpendableTransactions(donationAddresses);

                if (! addressTransactionsResponse.wasSuccessful()) {
                    Logger.debug("Unable to load Transactions for donation addresses.");
                    return;
                }

                transactionHashes = addressTransactionsResponse.getTransactionHashes();
            }

            final Map<Sha256Hash, Transaction> transactions;
            final Map<TransactionOutputIdentifier, Address> previousOutputAddresses;
            final Map<TransactionOutputIdentifier, Long> previousOutputAmounts;
            {
                final GetTransactionsResponse getTransactionsResponse;
                try (final NodeConnection nodeConnection = _environment.newNodeConnection()) {
                    getTransactionsResponse = nodeConnection.getTransactions(transactionHashes);
                }
                if (! getTransactionsResponse.wasSuccessful()) {
                    Logger.debug("Unable to load Transactions for donation addresses.");
                    return;
                }

                transactions = getTransactionsResponse.getTransactions();
                previousOutputAddresses = getTransactionsResponse.getPreviousOutputAddresses();
                previousOutputAmounts = getTransactionsResponse.getPreviousOutputAmounts();
            }

            for (final Address donationAddress : donationAddresses) {
                final List<Sha256Hash> addressTransactionHashes = addressTransactionsResponse.getTransactionHashes(donationAddress);
                for (final Sha256Hash transactionHash : addressTransactionHashes) {
                    _newTransactions.remove(transactionHash); // Dequeue the Transaction for retries...

                    final Transaction transaction = transactions.get(transactionHash);
                    final Boolean transactionHasBeenProcessed = databaseManager.wasTransactionProcessed(transactionHash);
                    if (transactionHasBeenProcessed) { continue; }

                    Logger.debug("Processing: " + transactionHash);

                    final long totalInputAmount;
                    {
                        long total = 0L;
                        for (final TransactionInput transactionInput : transaction.getTransactionInputs()) {
                            final TransactionOutputIdentifier previousOutputIdentifier = TransactionOutputIdentifier.fromTransactionInput(transactionInput);
                            final Long inputAmount = previousOutputAmounts.get(previousOutputIdentifier);
                            total += inputAmount;
                        }
                        totalInputAmount = total;
                    }

                    final Address returnAddress = databaseManager.getReturnAddress(donationAddress);

                    final HashMap<Address, Double> inputPercentages = new HashMap<Address, Double>();
                    if (returnAddress != null) { // Override the token distributions to the returnAddress...
                        inputPercentages.put(returnAddress, 1D);
                    }
                    else { // Map the inputs to their contribution ratios...
                        for (final TransactionInput transactionInput : transaction.getTransactionInputs()) {
                            final Address inputAddress = previousOutputAddresses.get(TransactionOutputIdentifier.fromTransactionInput(transactionInput));
                            final Long inputAmount = previousOutputAmounts.get(TransactionOutputIdentifier.fromTransactionInput(transactionInput));
                            final Double inputPercent = (inputAmount.doubleValue() / totalInputAmount);

                            final Double existingPercent = Util.coalesce(inputPercentages.get(inputAddress), 0D);
                            inputPercentages.put(inputAddress, (existingPercent + inputPercent));
                        }
                    }

                    final PrivateKey donationAddressPrivateKey = databaseManager.getPrivateKey(donationAddress);
                    final DistributionResult distributionResult = _claimTransactionAndDistributeTokens(transaction, donationAddress, donationAddressPrivateKey, inputPercentages);

                    if (distributionResult.wasProcessedSuccessfully) {
                        databaseManager.markTransactionAsProcessed(transactionHash);

                        for (final Transaction distributionTransaction : distributionResult.distributionTransactions) {
                            if (distributionTransaction != null) {
                                final Sha256Hash distributionTransactionHash = distributionTransaction.getHash();

                                // Mark the new transaction as processed in order to prevent a self-feedback donation loop...
                                databaseManager.markTransactionAsProcessed(distributionTransactionHash);

                                final Boolean broadcastWasSuccessful = _broadcastTransaction(distributionTransaction);
                                if (broadcastWasSuccessful) {
                                    final Wallet mainWallet = _environment.getDeveloperTokenWallet();
                                    mainWallet.addTransaction(distributionTransaction);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void _processRedemptionAddresses() throws DatabaseException {
        final SlpTokenId slpTokenId = _environment.getSlpTokenId();

        try (final DatabaseManager databaseManager = _environment.newDatabaseManager()) {
            final List<Address> redemptionAddresses = databaseManager.getRedemptionAddresses();

            for (final Address redemptionAddress : redemptionAddresses) {
                final RedemptionItem.ItemId itemId = databaseManager.getRedemptionItem(redemptionAddress);
                if (itemId == null) { continue; } // Deprecated Item...

                final Long requiredTokenAmount = _tokenExchangeCalculator.getTokenRedemptionCost(itemId);

                final PrivateKey redemptionAddressPrivateKey = databaseManager.getPrivateKey(redemptionAddress);

                final Wallet wallet = new Wallet();
                wallet.addPrivateKey(redemptionAddressPrivateKey);

                try (final NodeConnection nodeConnection = _environment.newNodeConnection()) {
                    TokenService.loadWalletWithUnspentTransactionOutputs(wallet, redemptionAddress, databaseManager, nodeConnection);
                }

                for (final Transaction transaction : wallet.getTransactions()) {
                    final Sha256Hash transactionHash = transaction.getHash();
                    _newTransactions.remove(transactionHash); // Dequeue the Transaction for retries...
                }

                final Long tokenBalance = wallet.getSlpTokenBalance(slpTokenId);
                if (tokenBalance >= requiredTokenAmount) {
                    // final boolean tokensReclaimedSuccessfully;
                    // { // Recover the tokens from the redemption address...
                    //     tokensReclaimedSuccessfully = _reclaimTokens(redemptionAddressPrivateKey, wallet.getTransactions());
                    // }

                    // if (tokensReclaimedSuccessfully) {
                    { // Send Notification Email...
                        if (_emailClient != null) {
                            final Json formData = databaseManager.getRedemptionFormData(redemptionAddress);
                            final StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(tokenBalance);
                            stringBuilder.append(" Verde Points have been deposited into ");
                            stringBuilder.append(redemptionAddress.toBase58CheckEncoded());
                            stringBuilder.append(".\n\n");
                            stringBuilder.append(formData.toFormattedString());

                            _emailClient.email("Verde Points Redeemed", stringBuilder.toString());
                        }
                    }

                    for (final Transaction transaction : wallet.getTransactions()) {
                        final Sha256Hash transactionHash = transaction.getHash();
                        databaseManager.markTransactionAsProcessed(transactionHash);
                    }
                }
            }
        }
    }

    @Override
    protected void _onStart() { }

    @Override
    protected Boolean _run() {
        try {
            _processDonationAddresses();

            _processRedemptionAddresses();

            if (_newTransactions.isEmpty()) {
                return false;
            }

            { // While there were new transactions announced that weren't successfully processed then try multiple times, after a delay.
                for (final Sha256Hash transactionHash : _newTransactions.keySet()) {
                    final Integer attemptCount = _newTransactions.get(transactionHash);
                    if (attemptCount == null) { continue; }

                    if (attemptCount > 2) {
                        _newTransactions.remove(transactionHash);
                        continue;
                    }

                    _newTransactions.put(transactionHash, (attemptCount + 1));
                }

                Thread.sleep(5000L);

                return true;
            }
        }
        catch (final Exception exception) {
            Logger.debug(exception);
            return false;
        }
    }

    @Override
    protected void _onSleep() { }

    public TokenService(final Environment environment, final TokenExchangeCalculator tokenExchangeCalculator, final EmailClient emailClient) {
        _environment = environment;
        _tokenExchangeCalculator = tokenExchangeCalculator;
        _emailClient = emailClient;
    }

    public void onNewTransaction(final Sha256Hash transactionHash, final Map<TransactionOutputIdentifier, Long> slpOutputAmounts) {
        _newTransactions.put(transactionHash, 0);
    }
}
