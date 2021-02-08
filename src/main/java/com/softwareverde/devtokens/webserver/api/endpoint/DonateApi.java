package com.softwareverde.devtokens.webserver.api.endpoint;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.devtokens.NewAddressCreatedCallback;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.ApiResult;
import com.softwareverde.devtokens.webserver.api.v1.post.NewDonationAddressHandler;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.json.Json;

public class DonateApi extends ApiEndpoint {
    public static class NewDonationAddressResult extends ApiResult {
        private Address _address;
        private Address _returnAddress;

        public void setAddress(final Address address) {
            _address = address;
        }

        public void setReturnAddress(final Address returnAddress) {
            _returnAddress = returnAddress;
        }

        @Override
        public Json toJson() {
            final Json json = super.toJson();

            final Json addressJson;
            {
                addressJson = new Json(false);
                addressJson.put("base58CheckEncoded", (_address != null ? _address.toBase58CheckEncoded() : null));
                addressJson.put("base32CheckEncoded", (_address != null ? _address.toBase32CheckEncoded(true) : null));
                addressJson.put("slpBase32CheckEncoded", (_address != null ? _address.toBase32CheckEncoded(Address.BASE_32_SLP_LABEL, true) : null));
            }
            json.put("address", addressJson);
            json.put("returnAddress", (_returnAddress != null ? _returnAddress.toBase32CheckEncoded(Address.BASE_32_SLP_LABEL, true) : null));

            return json;
        }
    }

    protected NewAddressCreatedCallback _newAddressCreatedCallback;

    public DonateApi(final String apiPrePath, final Environment environment) {
        super(environment);

        _defineEndpoint((apiPrePath + "/donate/new"), HttpMethod.POST, new NewDonationAddressHandler(new NewAddressCreatedCallback() {
            @Override
            public void newAddressCreated(final Address address) {
                final NewAddressCreatedCallback newAddressCreatedCallback = _newAddressCreatedCallback;
                if (newAddressCreatedCallback != null) {
                    newAddressCreatedCallback.newAddressCreated(address);
                }
            }
        }));
    }

    public void setNewAddressCreatedCallback(final NewAddressCreatedCallback newAddressCreatedCallback) {
        _newAddressCreatedCallback = newAddressCreatedCallback;
    }
}
