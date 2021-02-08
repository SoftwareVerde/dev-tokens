package com.softwareverde.devtokens.webserver.api.v1.post;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.constable.list.List;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.devtokens.NewAddressCreatedCallback;
import com.softwareverde.devtokens.RedemptionItem;
import com.softwareverde.devtokens.TokenService;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.database.DatabaseManager;
import com.softwareverde.devtokens.nodeapi.NodeConnection;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.endpoint.RedeemApi;
import com.softwareverde.http.querystring.PostParameters;
import com.softwareverde.http.server.servlet.request.Request;
import com.softwareverde.http.server.servlet.response.JsonResponse;
import com.softwareverde.http.server.servlet.response.Response;
import com.softwareverde.http.server.servlet.routed.RequestHandler;
import com.softwareverde.json.Json;
import com.softwareverde.util.Util;

import java.util.Map;

public class NewRedemptionAddressHandler implements RequestHandler<Environment> {
    protected final RedemptionItemConfiguration _redemptionItemConfiguration;
    protected final NewAddressCreatedCallback _newAddressCreatedCallback;

    public NewRedemptionAddressHandler(final RedemptionItemConfiguration redemptionItemConfiguration, final NewAddressCreatedCallback newAddressCreatedCallback) {
        _redemptionItemConfiguration = redemptionItemConfiguration;
        _newAddressCreatedCallback = newAddressCreatedCallback;
    }

    @Override
    public Response handleRequest(final Request request, final Environment environment, final Map<String, String> parameters) throws Exception {
        final SlpTokenId slpTokenId = environment.getSlpTokenId();
        final MasterInflater masterInflater = environment.getMasterInflater();
        final AddressInflater addressInflater = masterInflater.getAddressInflater();

        final Long itemIdLong = Util.parseLong(parameters.get("itemId"));
        final RedemptionItem.ItemId itemId = RedemptionItem.ItemId.fromLong(itemIdLong);
        if (itemId == null) {
            final RedeemApi.NewRedemptionAddressResult newRedemptionAddressResult = new RedeemApi.NewRedemptionAddressResult();
            newRedemptionAddressResult.setWasSuccess(false);
            newRedemptionAddressResult.setErrorMessage("Missing parameter: itemId");
            return new JsonResponse(Response.Codes.BAD_REQUEST, newRedemptionAddressResult);
        }

        final Json formData = new Json(false);
        {
            final RedemptionItem redemptionItem = _redemptionItemConfiguration.getRedemptionItemById(itemId);
            if (redemptionItem == null) {
                final RedeemApi.NewRedemptionAddressResult newRedemptionAddressResult = new RedeemApi.NewRedemptionAddressResult();
                newRedemptionAddressResult.setWasSuccess(false);
                newRedemptionAddressResult.setErrorMessage("Invalid Item Id: " + itemId);
                return new JsonResponse(Response.Codes.BAD_REQUEST, newRedemptionAddressResult);
            }

            final List<String> requiredFields = redemptionItem.getRequiredFields();
            final PostParameters postParameters = request.getPostParameters();
            for (final String fieldName : requiredFields) {
                final String fieldValue = postParameters.get(fieldName);
                if (Util.isBlank(fieldValue)) {
                    final RedeemApi.NewRedemptionAddressResult newRedemptionAddressResult = new RedeemApi.NewRedemptionAddressResult();
                    newRedemptionAddressResult.setWasSuccess(false);
                    newRedemptionAddressResult.setErrorMessage("Missing Field: " + fieldName);
                    return new JsonResponse(Response.Codes.BAD_REQUEST, newRedemptionAddressResult);
                }

                formData.put(fieldName, fieldValue);
            }

            final List<String> optionalFields = redemptionItem.getOptionalFields();
            for (final String fieldName : optionalFields) {
                final String fieldValue = parameters.get(fieldName);
                formData.put(fieldName, fieldValue);
            }

            formData.put("itemId", itemId);
        }

        final Long unusedTokenBalance;
        final Address address;
        try (final DatabaseManager databaseManager = environment.newDatabaseManager()) {
            final Address existingAddress = databaseManager.getRedemptionAddress(formData);
            if (existingAddress != null) {
                address = existingAddress;

                final Wallet wallet = new Wallet();
                final PrivateKey privateKey = databaseManager.getPrivateKey(address);
                wallet.addPrivateKey(privateKey);

                try (final NodeConnection nodeConnection = environment.newNodeConnection()) {
                    TokenService.loadWalletWithUnspentTransactionOutputs(wallet, address, databaseManager, nodeConnection);
                }

                unusedTokenBalance = wallet.getSlpTokenBalance(slpTokenId);
            }
            else {
                final PrivateKey privateKey = PrivateKey.createNewKey();
                address = addressInflater.fromPrivateKey(privateKey, true);

                databaseManager.storeRedemptionAddress(itemId, privateKey, address, formData);

                final Wallet spvWallet = environment.getSpvWallet();
                spvWallet.addPrivateKey(privateKey);

                if (_newAddressCreatedCallback != null) {
                    _newAddressCreatedCallback.newAddressCreated(address);
                }

                unusedTokenBalance = 0L;
            }
        }

        final RedeemApi.NewRedemptionAddressResult newRedemptionAddressResult = new RedeemApi.NewRedemptionAddressResult();
        newRedemptionAddressResult.setWasSuccess(true);
        newRedemptionAddressResult.setAddress(address);
        newRedemptionAddressResult.setUnusedTokenBalance(unusedTokenBalance);
        return new JsonResponse(Response.Codes.OK, newRedemptionAddressResult);
    }
}
