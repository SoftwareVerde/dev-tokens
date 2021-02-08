package com.softwareverde.devtokens.util;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.script.locking.LockingScript;
import com.softwareverde.bitcoin.transaction.script.slp.SlpScriptBuilder;
import com.softwareverde.bitcoin.transaction.script.slp.SlpScriptInflater;
import com.softwareverde.bitcoin.transaction.script.slp.genesis.MutableSlpGenesisScript;
import com.softwareverde.bitcoin.transaction.script.slp.genesis.SlpGenesisScript;
import com.softwareverde.bitcoin.wallet.PaymentAmount;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.devtokens.configuration.TokenGenesisConfiguration;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.Util;

public class SlpTokenCreator {
    protected final MasterInflater _masterInflater;

    public SlpTokenCreator(final MasterInflater masterInflater) {
        _masterInflater = masterInflater;
    }

    public Transaction createSlpToken(final TokenGenesisConfiguration tokenGenesisConfiguration) {
        final Address batonAddress;
        final LockingScript lockingScript;
        {
            final SlpScriptBuilder slpScriptBuilder = new SlpScriptBuilder();
            final MutableSlpGenesisScript slpGenesisScript = new MutableSlpGenesisScript();

            final String tokenName = tokenGenesisConfiguration.getTokenName();
            if (Util.isBlank(tokenName)) {
                Logger.error("Invalid token name: " + tokenName);
                return null;
            }

            final String tokenAbbreviation = tokenGenesisConfiguration.getTokenAbbreviation();
            if (Util.isBlank(tokenAbbreviation)) {
                Logger.error("Invalid token abbreviation: " + tokenAbbreviation);
                return null;
            }

            final String tokenUrl = tokenGenesisConfiguration.getTokenUrl();
            final Long tokenCount = tokenGenesisConfiguration.getTokenCount();
            if (tokenCount < 1L) {
                Logger.error("Invalid token count: " + tokenCount);
                return null;
            }

            batonAddress = tokenGenesisConfiguration.getBatonAddress();

            slpGenesisScript.setTokenName(tokenName);
            slpGenesisScript.setTokenAbbreviation(tokenAbbreviation);
            slpGenesisScript.setDocumentUrl(tokenUrl);
            slpGenesisScript.setDocumentHash(null);
            slpGenesisScript.setDecimalCount(0);
            slpGenesisScript.setBatonOutputIndex((batonAddress != null ? 2 : null));
            slpGenesisScript.setTokenCount(tokenCount);
            lockingScript = slpScriptBuilder.createGenesisScript(slpGenesisScript);

            if (lockingScript == null) {
                Logger.error("Unable to create SlpGenesisScript.");
                return null;
            }

            final SlpScriptInflater slpScriptInflater = new SlpScriptInflater();
            final SlpGenesisScript reInflatedSlpGenesisScript = slpScriptInflater.genesisScriptFromScript(lockingScript);
            if (! Util.areEqual(slpGenesisScript, reInflatedSlpGenesisScript)) {
                Logger.error("Unable to verify SlpGenesisScript.");
                return null;
            }
        }

        final Address changeAddress;
        final Wallet wallet = new Wallet();
        {
            final PrivateKey privateKey = tokenGenesisConfiguration.getFundingPrivateKey();
            if (privateKey == null) {
                Logger.error("Invalid funding PrivateKey.");
                return null;
            }
            wallet.addPrivateKey(privateKey);

            final AddressInflater addressInflater = _masterInflater.getAddressInflater();
            changeAddress = addressInflater.fromPrivateKey(privateKey, true);

            final Transaction fundingTransaction = tokenGenesisConfiguration.getFundingTransaction();
            if (fundingTransaction == null) {
                Logger.error("Invalid funding Transaction.");
                return null;
            }
            wallet.addTransaction(fundingTransaction);
        }


        final Long dustAmount = wallet.getDustThreshold(true);
        final MutableList<PaymentAmount> paymentAmounts = new MutableList<PaymentAmount>();
        paymentAmounts.add(new PaymentAmount(changeAddress, dustAmount));
        if (batonAddress != null) {
            paymentAmounts.add(new PaymentAmount(batonAddress, dustAmount));
        }
        final Transaction transaction = wallet.createTransaction(paymentAmounts, changeAddress, lockingScript);

        if (! Transaction.isSlpTransaction(transaction)) {
            Logger.error("Error creating Genesis Transaction.");
            return null;
        }

        return transaction;
    }
}
