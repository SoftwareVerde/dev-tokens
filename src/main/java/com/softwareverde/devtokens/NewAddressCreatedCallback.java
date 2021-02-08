package com.softwareverde.devtokens;

import com.softwareverde.bitcoin.address.Address;

public interface NewAddressCreatedCallback {
    void newAddressCreated(Address address);
}