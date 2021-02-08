package com.softwareverde.devtokens.configuration;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.secp256k1.privatekey.PrivateKeyInflater;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.constable.list.immutable.ImmutableList;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.Util;

import java.util.Properties;

public class ServerPropertiesLoader {
    protected final MasterInflater _masterInflater;

    public ServerPropertiesLoader(final MasterInflater masterInflater) {
        _masterInflater = masterInflater;
    }

    public ServerProperties loadServerProperties(final Properties properties) {
        final PrivateKeyInflater privateKeyInflater = new PrivateKeyInflater();
        final AddressInflater addressInflater = _masterInflater.getAddressInflater();

        final ServerProperties serverProperties = new ServerProperties();

        serverProperties._rootDirectory = properties.getProperty("server.rootDirectory", "");
        serverProperties._port = Util.parseInt(properties.getProperty("server.httpPort", "80"));
        serverProperties._tlsPort = Util.parseInt(properties.getProperty("server.tlsPort", "443"));
        serverProperties._socketPort = Util.parseInt(properties.getProperty("server.socketPort", "444"));
        serverProperties._tlsCertificateFile = properties.getProperty("server.tlsCertificateFile", "");
        serverProperties._tlsKeyFile = properties.getProperty("server.tlsKeyFile", "");

        serverProperties._slpTokenId = SlpTokenId.fromHexString(properties.getProperty("server.slpTokenId", ""));

        {
            final String privateKeyString = properties.getProperty("server.slpTokenPrivateKey", "");
            serverProperties._slpTokenPrivateKey = Util.coalesce(PrivateKey.fromHexString(privateKeyString), privateKeyInflater.fromWalletImportFormat(privateKeyString));
        }

        final String destinationAddressString = properties.getProperty("server.destinationAddress", "");
        serverProperties._destinationAddress = Util.coalesce(addressInflater.fromBase58Check(destinationAddressString), addressInflater.fromBase32Check(destinationAddressString));

        final MutableList<PrivateKey> staticDonationPrivateKeys = new MutableList<PrivateKey>();
        final String[] parsedStaticDonationPrivateKeyStrings = PropertiesUtil.parseStringArrayProperty("server.staticDonationPrivateKeys", "[]", properties);
        for (final String privateKeyString : parsedStaticDonationPrivateKeyStrings) {
            final PrivateKey privateKey = Util.coalesce(PrivateKey.fromHexString(privateKeyString), privateKeyInflater.fromWalletImportFormat(privateKeyString));
            if (privateKey != null) {
                staticDonationPrivateKeys.add(privateKey);
            }
        }
        serverProperties._staticDonationPrivateKeys = staticDonationPrivateKeys;

        final MutableList<Address> staticDonationWatchedAddresses = new MutableList<Address>();
        final String[] parsedStaticDonationWatchedAddressStrings = PropertiesUtil.parseStringArrayProperty("server.staticDonationWatchedAddresses", "[]", properties);
        for (final String addressString : parsedStaticDonationWatchedAddressStrings) {
            final Address address = Util.coalesce(addressInflater.fromBase58Check(addressString), addressInflater.fromBase32Check(addressString));
            if (address != null) {
                staticDonationWatchedAddresses.add(address);
                Logger.info("Watching: " + address.toBase58CheckEncoded());
            }
        }
        serverProperties._staticDonationWatchedAddresses = staticDonationWatchedAddresses;

        serverProperties._useUniqueDonationAddresses = Util.parseBool(properties.getProperty("server.useUniqueDonationAddresses", "1"));

        serverProperties._dollarsPerToken = Util.parseDouble(properties.getProperty("server.dollarsPerToken", "50"));

        serverProperties._emailUsername = properties.getProperty("server.emailUsername", null);
        serverProperties._emailPassword = properties.getProperty("server.emailPassword", null);
        serverProperties._emailRecipients = new ImmutableList<String>(PropertiesUtil.parseStringArrayProperty("server.emailRecipients", "[]", properties));

        return serverProperties;
    }
}
