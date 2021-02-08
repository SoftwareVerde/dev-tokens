package com.softwareverde.devtokens.configuration;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.constable.list.List;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;

public class ServerProperties {
    protected String _rootDirectory;
    protected String _tlsCertificateFile;
    protected String _tlsKeyFile;
    protected Integer _port;
    protected Integer _tlsPort;
    protected Integer _socketPort;

    protected SlpTokenId _slpTokenId;
    protected PrivateKey _slpTokenPrivateKey;
    protected Address _destinationAddress;
    protected List<PrivateKey> _staticDonationPrivateKeys;
    protected List<Address> _staticDonationWatchedAddresses;
    protected Boolean _useUniqueDonationAddresses;
    protected Double _dollarsPerToken;

    protected String _emailUsername;
    protected String _emailPassword;
    protected List<String> _emailRecipients;

    public String getRootDirectory() { return _rootDirectory; }
    public String getTlsCertificateFile() { return _tlsCertificateFile; }
    public String getTlsKeyFile() { return _tlsKeyFile; }
    public Integer getPort() { return _port; }
    public Integer getTlsPort() { return _tlsPort; }
    public Integer getSocketPort() { return _socketPort; }

    public SlpTokenId getSlpTokenId() { return _slpTokenId; }
    public PrivateKey getSlpTokenPrivateKey() { return _slpTokenPrivateKey; }
    public Address getDestinationAddress() { return _destinationAddress; }
    public List<PrivateKey> getStaticDonationPrivateKeys() { return _staticDonationPrivateKeys; }
    public List<Address> getStaticDonationWatchedAddresses() { return _staticDonationWatchedAddresses; }
    public Boolean useUniqueDonationAddresses() { return _useUniqueDonationAddresses; }
    public Double getDollarsPerToken() { return _dollarsPerToken; }

    public String getEmailUsername() { return _emailUsername; }
    public String getEmailPassword() { return _emailPassword; }
    public List<String> getEmailRecipients() { return _emailRecipients; }
}

