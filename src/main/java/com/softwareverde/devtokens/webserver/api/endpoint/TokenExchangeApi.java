package com.softwareverde.devtokens.webserver.api.endpoint;

import com.softwareverde.devtokens.RedemptionItem;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.TokenExchangeCalculator;
import com.softwareverde.devtokens.webserver.api.ApiResult;
import com.softwareverde.devtokens.webserver.api.v1.get.TokenExchangeRateHandler;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.json.Json;

import java.util.HashMap;
import java.util.Map;

public class TokenExchangeApi extends ApiEndpoint {
    public static class GetDevTokenExchangeRateResult extends ApiResult {
        private final Map<RedemptionItem.ItemId, Long> _redemptionItemMap = new HashMap<>();
        private Long _satoshisPerDevToken;
        private Double _dollarsPerBitcoin;

        public void setDollarsPerBitcoin(final Double dollarsPerBitcoin) {
            _dollarsPerBitcoin = dollarsPerBitcoin;
        }

        public void setSatoshisPerDevToken(final Long satoshisPerDevToken) {
            _satoshisPerDevToken = satoshisPerDevToken;
        }

        public void setDevTokensPerRedemptionItem(final RedemptionItem.ItemId redemptionItem, final Long devTokenCount) {
            _redemptionItemMap.put(redemptionItem, devTokenCount);
        }

        @Override
        public Json toJson() {
            final Json json = super.toJson();
            json.put("dollarsPerBitcoin", _dollarsPerBitcoin);
            json.put("satoshisPerDevToken", _satoshisPerDevToken);

            final Json redemptionItemsJson = new Json(false);
            for (final RedemptionItem.ItemId redemptionItemId : _redemptionItemMap.keySet()) {
                final Long itemId = redemptionItemId.longValue();
                final Long devTokenCount = _redemptionItemMap.get(redemptionItemId);

                redemptionItemsJson.put(String.valueOf(itemId), devTokenCount);
            }

            json.put("redemptionItems", redemptionItemsJson);

            return json;
        }
    }

    public TokenExchangeApi(final String apiPrePath, final Environment environment) {
        super(environment);

        final RedemptionItemConfiguration redemptionItemConfiguration = environment.getRedemptionItemConfiguration();
        final TokenExchangeCalculator tokenExchangeCalculator = environment.getTokenExchangeCalculator();
        _defineEndpoint((apiPrePath + "/exchange-rate"), HttpMethod.GET, new TokenExchangeRateHandler(redemptionItemConfiguration, tokenExchangeCalculator));
    }
}
