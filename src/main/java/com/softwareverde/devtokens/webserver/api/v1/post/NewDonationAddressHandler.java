package com.softwareverde.devtokens.webserver.api.v1.post;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.constable.list.List;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.devtokens.NewAddressCreatedCallback;
import com.softwareverde.devtokens.configuration.ServerProperties;
import com.softwareverde.devtokens.database.DatabaseManager;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.endpoint.DonateApi;
import com.softwareverde.http.querystring.PostParameters;
import com.softwareverde.http.server.servlet.request.Request;
import com.softwareverde.http.server.servlet.response.JsonResponse;
import com.softwareverde.http.server.servlet.response.Response;
import com.softwareverde.http.server.servlet.routed.RequestHandler;
import com.softwareverde.logging.Logger;

import java.util.Map;

public class NewDonationAddressHandler implements RequestHandler<Environment> {
    protected final NewAddressCreatedCallback _newAddressCreatedCallback;

    protected Address _createNewAddress(final Environment environment, final Address returnAddress) throws DatabaseException {
        final MasterInflater masterInflater = environment.getMasterInflater();
        final AddressInflater addressInflater = masterInflater.getAddressInflater();

        final PrivateKey privateKey = PrivateKey.createNewKey();
        final Address address = addressInflater.fromPrivateKey(privateKey, true);

        try (final DatabaseManager databaseManager = environment.newDatabaseManager()) {
            databaseManager.storeDonationAddress(privateKey, address, returnAddress);
        }

        final Wallet spvWallet = environment.getSpvWallet();
        spvWallet.addPrivateKey(privateKey);

        if (_newAddressCreatedCallback != null) {
            _newAddressCreatedCallback.newAddressCreated(address);
        }

        return address;
    }

    public NewDonationAddressHandler(final NewAddressCreatedCallback newAddressCreatedCallback) {
        _newAddressCreatedCallback = newAddressCreatedCallback;
    }

    @Override
    public Response handleRequest(final Request request, final Environment environment, final Map<String, String> parameters) throws Exception {
        final PostParameters postParameters = request.getPostParameters();

        final Address returnAddress;
        if (postParameters.containsKey("return_address")) {
            final String returnAddressString = postParameters.get("return_address");
            returnAddress = ValidateSlpAddressHandler.inflateSlpAddress(environment, returnAddressString);
            if (returnAddress == null) {
                final DonateApi.NewDonationAddressResult newDonationAddressResult = new DonateApi.NewDonationAddressResult();
                newDonationAddressResult.setWasSuccess(false);
                newDonationAddressResult.setErrorMessage("Invalid return address: " + returnAddressString);
                return new JsonResponse(Response.Codes.BAD_REQUEST, newDonationAddressResult);
            }
        }
        else {
            returnAddress = null;
        }

        final Address address;
        try {
            final ServerProperties serverProperties = environment.getServerProperties();
            final Boolean useUniqueDonationAddresses = serverProperties.useUniqueDonationAddresses();

            if (! useUniqueDonationAddresses) {
                if (returnAddress != null) {
                    final DonateApi.NewDonationAddressResult newDonationAddressResult = new DonateApi.NewDonationAddressResult();
                    newDonationAddressResult.setWasSuccess(false);
                    newDonationAddressResult.setErrorMessage("Service cannot use return addresses without unique donation addresses.");
                    return new JsonResponse(Response.Codes.BAD_REQUEST, newDonationAddressResult);
                }

                try (final DatabaseManager databaseManager = environment.newDatabaseManager()) {
                    final List<Address> existingDonationAddresses = databaseManager.getDonationAddresses();
                    if (! existingDonationAddresses.isEmpty()) {
                        address = existingDonationAddresses.get(0);
                    }
                    else {
                        address = _createNewAddress(environment, null);
                    }
                }
            }
            else {
                address = _createNewAddress(environment, returnAddress);
            }
        }
        catch (final Exception exception) {
            Logger.debug(exception);

            final DonateApi.NewDonationAddressResult newDonationAddressResult = new DonateApi.NewDonationAddressResult();
            newDonationAddressResult.setWasSuccess(false);
            newDonationAddressResult.setErrorMessage("Unable to create donation address.");
            return new JsonResponse(Response.Codes.SERVER_ERROR, newDonationAddressResult);
        }

        final DonateApi.NewDonationAddressResult newDonationAddressResult = new DonateApi.NewDonationAddressResult();
        newDonationAddressResult.setWasSuccess(true);
        newDonationAddressResult.setAddress(address);
        newDonationAddressResult.setReturnAddress(returnAddress);
        return new JsonResponse(Response.Codes.OK, newDonationAddressResult);
    }
}
