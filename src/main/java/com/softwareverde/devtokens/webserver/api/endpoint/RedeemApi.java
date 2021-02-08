package com.softwareverde.devtokens.webserver.api.endpoint;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.devtokens.NewAddressCreatedCallback;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.ApiResult;
import com.softwareverde.devtokens.webserver.api.v1.post.NewRedemptionAddressHandler;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.json.Json;

public class RedeemApi extends ApiEndpoint {
    public static class NewRedemptionAddressResult extends ApiResult {
        private Address _address;
        private Long _unusedTokenBalance;

        public void setAddress(final Address address) {
            _address = address;
        }

        public void setUnusedTokenBalance(final Long unusedTokenBalance) {
            _unusedTokenBalance = unusedTokenBalance;
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

            json.put("tokenBalance", _unusedTokenBalance);

            return json;
        }
    }

    protected NewAddressCreatedCallback _newAddressCreatedCallback;

    public RedeemApi(final String apiPrePath, final Environment environment) {
        super(environment);

        final RedemptionItemConfiguration redemptionItemConfiguration = environment.getRedemptionItemConfiguration();
        _defineEndpoint((apiPrePath + "/redeem/<itemId>/new"), HttpMethod.POST, new NewRedemptionAddressHandler(redemptionItemConfiguration, new NewAddressCreatedCallback() {
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
