package com.softwareverde.devtokens.configuration;

import com.softwareverde.bitcoin.server.configuration.NodeProperties;
import com.softwareverde.constable.list.List;

public class BitcoinProperties {
    protected String _bitcoinRpcUrl;
    protected Integer _bitcoinRpcPort;
    protected List<NodeProperties> _seedNodeProperties;

    public String getBitcoinRpcUrl() { return _bitcoinRpcUrl; }
    public Integer getBitcoinRpcPort() { return _bitcoinRpcPort; }
    public List<NodeProperties> getSeedNodeProperties() { return _seedNodeProperties; }
}
