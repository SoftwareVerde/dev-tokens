package com.softwareverde.devtokens.webserver.api.endpoint;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.ApiResult;
import com.softwareverde.devtokens.webserver.api.v1.post.ValidateSlpAddressHandler;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.json.Json;

public class AddressApi extends ApiEndpoint {
    public static class ValidateSlpAddressResult extends ApiResult {
        private String _inputString;
        private Address _address;
        private Boolean _isSimpleLedgerAddress;

        public void setInputString(final String inputString) {
            _inputString = inputString;
        }

        public void setIsSimpleLedgerAddress(final Boolean isSimpleLedgerAddress) {
            _isSimpleLedgerAddress = isSimpleLedgerAddress;
        }

        public void setAddress(final Address address) {
            _address = address;
        }

        @Override
        public Json toJson() {
            final Json json = super.toJson();
            json.put("inputString", _inputString);
            json.put("isSlpAddress", (_isSimpleLedgerAddress ? 1 : 0));

            final Json addressJson;
            {
                addressJson = new Json(false);
                addressJson.put("base58CheckEncoded", (_address != null ? _address.toBase58CheckEncoded() : null));
                addressJson.put("base32CheckEncoded", (_address != null ? _address.toBase32CheckEncoded(true) : null));
                addressJson.put("slpBase32CheckEncoded", (_address != null ? _address.toBase32CheckEncoded(Address.BASE_32_SLP_LABEL, true) : null));
            }
            json.put("address", addressJson);

            return json;
        }
    }

    public AddressApi(final String apiPrePath, final Environment environment) {
        super(environment);

        _defineEndpoint((apiPrePath + "/address/slp/validate"), HttpMethod.POST, new ValidateSlpAddressHandler());
    }
}
