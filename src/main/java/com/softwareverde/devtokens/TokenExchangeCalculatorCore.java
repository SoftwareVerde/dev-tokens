package com.softwareverde.devtokens;

import com.softwareverde.bitcoin.PriceIndexer;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoindotcom.BitcoinDotComPriceIndexer;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.webserver.TokenExchangeCalculator;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.type.time.SystemTime;

public class TokenExchangeCalculatorCore implements TokenExchangeCalculator {
    protected final SystemTime _systemTime;
    protected final PriceIndexer _priceIndexer;
    protected final RedemptionItemConfiguration _redemptionItemConfiguration;
    protected final Double _dollarsPerToken;

    protected Long _lastPriceQueryTimestamp = 0L;
    protected Double _cachedDollarsPerBitcoin = null;

    protected void _checkCache() {
        final Long now = _systemTime.getCurrentTimeInSeconds();
        final long secondsSinceLastUpdate = (now - _lastPriceQueryTimestamp);
        final long maxCacheAgeInSeconds = (5L * 60L); // 5 Minutes...
        if (secondsSinceLastUpdate >= maxCacheAgeInSeconds) {
            final Double dollarsPerBitcoin = _priceIndexer.getDollarsPerBitcoin();
            if ( (dollarsPerBitcoin == null) || (dollarsPerBitcoin < 1D) ) {
                Logger.error("Unable to load Bitcoin price.");
                return;
            }

            _cachedDollarsPerBitcoin = dollarsPerBitcoin;
            _lastPriceQueryTimestamp = now;
        }
    }

    protected TokenExchangeCalculatorCore(final SystemTime systemTime, final RedemptionItemConfiguration redemptionItemConfiguration, final PriceIndexer priceIndexer, final Double dollarsPerToken) {
        _systemTime = systemTime;
        _redemptionItemConfiguration = redemptionItemConfiguration;
        _priceIndexer = priceIndexer;
        _dollarsPerToken = dollarsPerToken;
    }

    public TokenExchangeCalculatorCore(final RedemptionItemConfiguration redemptionItemConfiguration, final Double dollarsPerToken) {
        _systemTime = new SystemTime();
        _priceIndexer = new BitcoinDotComPriceIndexer();
        _redemptionItemConfiguration = redemptionItemConfiguration;
        _dollarsPerToken = dollarsPerToken;
    }

    @Override
    public synchronized Double getDollarsPerBitcoin() {
        _checkCache();

        return _cachedDollarsPerBitcoin;
    }

    @Override
    public synchronized Long getSatoshisPerToken() {
        _checkCache();
        if (_cachedDollarsPerBitcoin == null) { return null; }

        final Double bchPerToken = (_dollarsPerToken / _cachedDollarsPerBitcoin);
        return (long) (Transaction.SATOSHIS_PER_BITCOIN * bchPerToken);
    }

    @Override
    public Long getTokenRedemptionCost(final RedemptionItem.ItemId itemId) {
        final RedemptionItem redemptionItem = _redemptionItemConfiguration.getRedemptionItemById(itemId);
        if (redemptionItem == null) { return null; }

        return redemptionItem.getTokenAmount();
    }
}
