package com.softwareverde.devtokens.webserver.api.v1.get;

import com.softwareverde.devtokens.RedemptionItem;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.TokenExchangeCalculator;
import com.softwareverde.devtokens.webserver.api.endpoint.TokenExchangeApi;
import com.softwareverde.http.server.servlet.request.Request;
import com.softwareverde.http.server.servlet.response.JsonResponse;
import com.softwareverde.http.server.servlet.response.Response;
import com.softwareverde.http.server.servlet.routed.RequestHandler;

import java.util.Map;

public class TokenExchangeRateHandler implements RequestHandler<Environment> {
    protected final RedemptionItemConfiguration _redemptionItemConfiguration;
    protected final TokenExchangeCalculator _tokenExchangeCalculator;

    public TokenExchangeRateHandler(final RedemptionItemConfiguration redemptionItemConfiguration, final TokenExchangeCalculator tokenExchangeCalculator) {
        _redemptionItemConfiguration = redemptionItemConfiguration;
        _tokenExchangeCalculator = tokenExchangeCalculator;
    }

    @Override
    public Response handleRequest(final Request request, final Environment environment, final Map<String, String> parameters) throws Exception {
        final TokenExchangeCalculator tokenExchangeCalculator = environment.getTokenExchangeCalculator();

        final TokenExchangeApi.GetDevTokenExchangeRateResult devTokenExchangeRateResult = new TokenExchangeApi.GetDevTokenExchangeRateResult();

        final Double dollarsPerBitcoin = tokenExchangeCalculator.getDollarsPerBitcoin();
        devTokenExchangeRateResult.setDollarsPerBitcoin(dollarsPerBitcoin);

        final Long satoshisPerDevToken = tokenExchangeCalculator.getSatoshisPerToken();
        devTokenExchangeRateResult.setSatoshisPerDevToken(satoshisPerDevToken);

        for (final RedemptionItem.ItemId itemId : _redemptionItemConfiguration.getRedemptionItemIds()) {
            final Long tokenAmount = _tokenExchangeCalculator.getTokenRedemptionCost(itemId);
            devTokenExchangeRateResult.setDevTokensPerRedemptionItem(itemId, tokenAmount);
        }

        devTokenExchangeRateResult.setWasSuccess(true);
        return new JsonResponse(Response.Codes.OK, devTokenExchangeRateResult);
    }
}
