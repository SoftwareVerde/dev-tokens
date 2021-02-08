package com.softwareverde.devtokens;

import com.softwareverde.bitcoin.PriceIndexer;
import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.secp256k1.privatekey.PrivateKeyInflater;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionInflater;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.testing.FakeEnvironment;
import com.softwareverde.devtokens.testing.FakeTokenExchangeCalculator;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.TokenExchangeCalculator;
import com.softwareverde.util.Container;
import com.softwareverde.util.type.time.SystemTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TokenExchangeCalculatorTests {
    /**
     * TODO: These tests were created in a way that requires multiple private keys, which was not ideal.
     *  Since these keys aren't published, the tests here are disabled.  These tests should either be refactored to
     *  fake the need for a PrivateKey, or regenerated with an address whose keys have zero value.
     */

    protected static final String GARLIC_TOKEN_PRIVATE_KEY = "";
    protected static final String GARLIC_TOKEN_GENESIS = "02000000011ECC0E19759BDCC5071DD44C05C10C20BDD9F5D2F95234F78B50923AB4715F7E000000006B48304502210095908E903A7E3BCFA60558536E032021AB5112D5B53A3BDD4D7D3CB3C9EF6A57022055843074F933E9DACF6DCAF158F94B1E1B0414BC9D5840D540F203D9AC17F391412103D744C8358DCDE64E382496645BAF9D26F434EF7399156DC7C2F4A2B07040A9FCFFFFFFFF0300000000000000003B6A04534C500001010747454E455349530347544B0C4761726C696320546F6B656E0A6761726C69632E636F6D4C0001010102080000000005F5E10066460200000000001976A914B53DF8E5155751CFC27F0AE2BF6B4B40ABD4652D88AC22020000000000001976A914B53DF8E5155751CFC27F0AE2BF6B4B40ABD4652D88AC00000000";
    protected static final String GARLIC_TOKEN_SLP_ID = "49D9E77785F38E7C2F3C2B7A4760F73D825C050E2EE8B7CB97BEC9CE0AB81160";

    protected static final String DONATION_PRIVATE_KEY = "";
    protected static final String DONATION_TRANSACTION = "020000000147D8FD6BBA0ABEACE49EB71C467F48AEAD521B96E2DFDE392B3EFE612A758E09010000006A4730440220362F91384F98D180E2BD61B3B02003FB6923D8CB479BF753108623EE198BB00A022033174A768D6E4086616E4C4E427BB3A6702E3F205F02586C8053C636B9E3E2634121032595488709D1A879E3A11A3D3D20BA08055BDA25EE774639FB01ABF21E27BDA8FFFFFFFF0220A10700000000001976A9143A4A0C36B70FE4A231EB768AE10383E813985B9988AC18829310000000001976A9146C103A775D553CE9023440BE71D24654809013B488AC00000000"; // E32B68E58D58810BC8FB39D6E5BC5D5CF8EDD12C4A109267B2A363795F4091AD
    protected static final String DONATING_FROM_ADDRESS = "1ArPRHZdrUKtnoWLLsYBq4za89a5nsjcbi";

    protected static class TestEnvironment extends FakeEnvironment {
        protected final SlpTokenId _slpTokenId;
        protected final PrivateKey _privateKey;
        protected final Address _donationDestinationAddress;
        protected final Wallet _wallet;

        public TestEnvironment(final SlpTokenId slpTokenId, final PrivateKey privateKey, final Address donationDestinationAddress, final Wallet wallet) {
            _slpTokenId = slpTokenId;
            _privateKey = privateKey;
            _donationDestinationAddress = donationDestinationAddress;
            _wallet = wallet;
        }

        @Override
        public Wallet getDeveloperTokenWallet() {
            return _wallet;
        }

        @Override
        public SlpTokenId getSlpTokenId() {
            return _slpTokenId;
        }

        @Override
        public PrivateKey getSlpTokenPrivateKey() {
            return _privateKey;
        }

        @Override
        public Address getDonationDestinationAddress() {
            return _donationDestinationAddress;
        }
    }

    @Test
    public void should_calculate_tokens_per_dollar() {
        // Setup
        final RedemptionItemConfiguration redemptionItemConfiguration = null;
        final TokenExchangeCalculator tokenExchangeCalculator = new TokenExchangeCalculatorCore(new SystemTime(), redemptionItemConfiguration, new PriceIndexer() {
            @Override
            public Double getDollarsPerBitcoin() {
                return 220D;
            }
        }, 50D) { };

        // Action
        final Long satoshisPerToken = tokenExchangeCalculator.getSatoshisPerToken();

        // Assert
        System.out.println(satoshisPerToken);
        Assert.assertNotNull(satoshisPerToken);
        Assert.assertTrue(satoshisPerToken > 0L);
    }

    // @Test
    public void token_service_should_distribute_tokens_with_sufficient_donation() {
        // Setup
        final AddressInflater addressInflater = new AddressInflater();
        final TransactionInflater transactionInflater = new TransactionInflater();
        final PrivateKeyInflater privateKeyInflater = new PrivateKeyInflater();

        final SlpTokenId slpTokenId = SlpTokenId.fromHexString(GARLIC_TOKEN_SLP_ID);
        final PrivateKey slpPrivateKey = privateKeyInflater.fromWalletImportFormat(GARLIC_TOKEN_PRIVATE_KEY);
        final Address donationDestinationAddress = (addressInflater.fromPrivateKey(PrivateKey.createNewKey()));
        final PrivateKey privateKey = privateKeyInflater.fromWalletImportFormat(DONATION_PRIVATE_KEY);
        final RedemptionItemConfiguration redemptionItemConfiguration = null;

        final Wallet wallet = new Wallet();
        wallet.addPrivateKey(slpPrivateKey);
        wallet.addTransaction(transactionInflater.fromBytes(ByteArray.fromHexString(GARLIC_TOKEN_GENESIS)));

        final Environment environment = new TestEnvironment(slpTokenId, slpPrivateKey, donationDestinationAddress, wallet);

        final TokenExchangeCalculator tokenExchangeCalculator = new FakeTokenExchangeCalculator(220D, 1D);
        final TokenService tokenService = new TokenService(environment, tokenExchangeCalculator, null);

        final Transaction transaction = transactionInflater.fromBytes(ByteArray.fromHexString(DONATION_TRANSACTION));

        final Map<Address, Double> addressPercentages = new HashMap<Address, Double>();
        addressPercentages.put(addressInflater.fromBase58Check(DONATING_FROM_ADDRESS), 1D);

        // Action
        final TokenService.DistributionResult result = tokenService._claimTransactionAndDistributeTokens(transaction, null, privateKey, addressPercentages);

        // Assert
        Assert.assertTrue(result.wasProcessedSuccessfully);
        Assert.assertNotNull(result.distributionTransactions);
    }

    // @Test
    public void token_service_should_not_distribute_tokens_with_insufficient_donation() {
        // Setup
        final AddressInflater addressInflater = new AddressInflater();
        final TransactionInflater transactionInflater = new TransactionInflater();
        final PrivateKeyInflater privateKeyInflater = new PrivateKeyInflater();

        final SlpTokenId slpTokenId = SlpTokenId.fromHexString(GARLIC_TOKEN_SLP_ID);
        final PrivateKey slpPrivateKey = privateKeyInflater.fromWalletImportFormat(GARLIC_TOKEN_PRIVATE_KEY);
        final Address donationDestinationAddress = addressInflater.fromBase58Check(DONATING_FROM_ADDRESS);

        final Wallet wallet = new Wallet();
        wallet.addPrivateKey(slpPrivateKey);
        wallet.addTransaction(transactionInflater.fromBytes(ByteArray.fromHexString(GARLIC_TOKEN_GENESIS)));

        final Environment environment = new TestEnvironment(slpTokenId, slpPrivateKey, donationDestinationAddress, wallet);

        final Container<Transaction> broadcastedTransaction = new Container<Transaction>();
        final TokenExchangeCalculator tokenExchangeCalculator = new FakeTokenExchangeCalculator(220D, 50D);
        final TokenService tokenService = new TokenService(environment, tokenExchangeCalculator, null) {
            @Override
            protected Boolean _broadcastTransaction(final Transaction transaction) {
                broadcastedTransaction.value = transaction;
                return true;
            }
        };

        final Transaction transaction = transactionInflater.fromBytes(ByteArray.fromHexString(DONATION_TRANSACTION));
        final PrivateKey privateKey = privateKeyInflater.fromWalletImportFormat(DONATION_PRIVATE_KEY);
        final Map<Address, Double> addressPercentages = new HashMap<Address, Double>();
        addressPercentages.put(addressInflater.fromBase58Check(DONATING_FROM_ADDRESS), 1D);

        // Action
        final TokenService.DistributionResult result = tokenService._claimTransactionAndDistributeTokens(transaction, null, privateKey, addressPercentages);

        // Assert
        Assert.assertTrue(result.wasProcessedSuccessfully);
        Assert.assertNull(broadcastedTransaction.value);
    }
}
