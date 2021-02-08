package com.softwareverde.devtokens.webserver;

import com.softwareverde.devtokens.RedemptionItem;

public interface TokenExchangeCalculator {
    Double getDollarsPerBitcoin();
    Long getSatoshisPerToken();
    Long getTokenRedemptionCost(RedemptionItem.ItemId itemId);
}
