package com.softwareverde.devtokens.testing;

import com.softwareverde.bitcoin.PriceIndexer;
import com.softwareverde.devtokens.TokenExchangeCalculatorCore;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.util.type.time.SystemTime;

public class FakeTokenExchangeCalculator extends TokenExchangeCalculatorCore {
    public FakeTokenExchangeCalculator(final Double dollarsPerBitcoin, final Double dollarsPerToken) {
        this(null, dollarsPerBitcoin, dollarsPerToken);
    }

    public FakeTokenExchangeCalculator(final RedemptionItemConfiguration redemptionItemConfiguration, final Double dollarsPerBitcoin, final Double dollarsPerToken) {
        super(new SystemTime(), redemptionItemConfiguration, new PriceIndexer() {
            @Override
            public Double getDollarsPerBitcoin() {
                return dollarsPerBitcoin;
            }
        }, dollarsPerToken);
    }
}
