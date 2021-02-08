package com.softwareverde.devtokens.webserver.api.v1.post;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.endpoint.AddressApi;
import com.softwareverde.http.querystring.PostParameters;
import com.softwareverde.http.server.servlet.request.Request;
import com.softwareverde.http.server.servlet.response.JsonResponse;
import com.softwareverde.http.server.servlet.response.Response;
import com.softwareverde.http.server.servlet.routed.RequestHandler;
import com.softwareverde.util.Util;

import java.util.Map;

public class ValidateSlpAddressHandler implements RequestHandler<Environment> {
    /**
     * Returns an Address object IFF the inputString is a simpleLedger address-type.
     *  inputString does not need the "simpleledger" prefix in order to be valid, but it must always use the SLP checksum.
     */
    public static Address inflateSlpAddress(final Environment environment, final String inputString) {
        final MasterInflater masterInflater = environment.getMasterInflater();
        final AddressInflater addressInflater = masterInflater.getAddressInflater();

        final String prefix = (Address.BASE_32_SLP_LABEL + ":");

        final String inflationString;
        {
            final int colonIndex = inputString.indexOf(":");
            if (colonIndex < 0) {
                inflationString = (prefix + inputString);
            }
            else {
                inflationString = (prefix + inputString.substring(colonIndex + 1));
            }
        }

        return addressInflater.fromBase32Check(inflationString);
    }

    public ValidateSlpAddressHandler() { }

    @Override
    public Response handleRequest(final Request request, final Environment environment, final Map<String, String> parameters) throws Exception {
        final PostParameters postParameters = request.getPostParameters();
        final String inputString = postParameters.get("address");

        final MasterInflater masterInflater = environment.getMasterInflater();
        final AddressInflater addressInflater = masterInflater.getAddressInflater();

        final Address address = Util.coalesce(addressInflater.fromBase58Check(inputString), addressInflater.fromBase32Check(inputString));
        final Address slpAddress = ValidateSlpAddressHandler.inflateSlpAddress(environment, inputString);

        final AddressApi.ValidateSlpAddressResult validateSlpAddressResult = new AddressApi.ValidateSlpAddressResult();
        validateSlpAddressResult.setWasSuccess(true);
        validateSlpAddressResult.setInputString(inputString);
        validateSlpAddressResult.setIsSimpleLedgerAddress((slpAddress != null));
        validateSlpAddressResult.setAddress(address);
        return new JsonResponse(Response.Codes.OK, validateSlpAddressResult);
    }
}
