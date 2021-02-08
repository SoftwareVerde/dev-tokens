package com.softwareverde.devtokens.webserver.api.v1.get;

import com.softwareverde.devtokens.RedemptionItem;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.endpoint.ItemsApi;
import com.softwareverde.http.server.servlet.request.Request;
import com.softwareverde.http.server.servlet.response.JsonResponse;
import com.softwareverde.http.server.servlet.response.Response;
import com.softwareverde.http.server.servlet.routed.RequestHandler;

import java.util.Map;

public class RedemptionItemsHandler implements RequestHandler<Environment> {
    protected final RedemptionItemConfiguration _redemptionItemConfiguration;

    public RedemptionItemsHandler(final RedemptionItemConfiguration redemptionItemConfiguration) {
        _redemptionItemConfiguration = redemptionItemConfiguration;
    }

    @Override
    public Response handleRequest(final Request request, final Environment environment, final Map<String, String> parameters) throws Exception {


        final ItemsApi.GetRedemptionItemsResult redemptionItemsResult = new ItemsApi.GetRedemptionItemsResult();

        for (final RedemptionItem.ItemId itemId : _redemptionItemConfiguration.getRedemptionItemIds()) {
            final RedemptionItem redemptionItem = _redemptionItemConfiguration.getRedemptionItemById(itemId);
            redemptionItemsResult.addRedemptionItem(redemptionItem);
        }

        redemptionItemsResult.setWasSuccess(true);
        return new JsonResponse(Response.Codes.OK, redemptionItemsResult);
    }
}
