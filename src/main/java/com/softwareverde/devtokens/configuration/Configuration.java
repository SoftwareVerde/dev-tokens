package com.softwareverde.devtokens.configuration;

import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.json.Json;
import com.softwareverde.util.IoUtil;
import com.softwareverde.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
    protected final DatabaseProperties _databaseProperties;
    protected final ServerProperties _serverProperties;
    protected final BitcoinProperties _bitcoinProperties;
    protected final TokenGenesisConfiguration _tokenGenesisConfiguration;
    protected final RedemptionItemConfiguration _redemptionItemConfiguration;

    public Configuration(final File configurationFile, final File redemptionItemFile, final MasterInflater masterInflater) {
        final Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(configurationFile));
        }
        catch (final IOException exception) { }

        _databaseProperties = DatabasePropertiesLoader.loadDatabaseProperties(null, properties);

        final ServerPropertiesLoader serverPropertiesLoader = new ServerPropertiesLoader(masterInflater);
        _serverProperties = serverPropertiesLoader.loadServerProperties(properties);

        final BitcoinPropertiesLoader bitcoinPropertiesLoader = new BitcoinPropertiesLoader();
        _bitcoinProperties = bitcoinPropertiesLoader.loadBitcoinProperties(properties);

        final TokenGenesisConfigurationLoader tokenGenesisConfigurationLoader = new TokenGenesisConfigurationLoader(masterInflater);
        _tokenGenesisConfiguration = tokenGenesisConfigurationLoader.loadTokenGenesisConfiguration(properties);

        final Json redemptionItemsJson = Json.parse(StringUtil.bytesToString(IoUtil.getFileContents(redemptionItemFile)));
        _redemptionItemConfiguration = RedemptionItemConfiguration.parse(redemptionItemsJson);

        if (_redemptionItemConfiguration == null) {
            throw new RuntimeException("Unable to parse redemption items.");
        }
    }

    public DatabaseProperties getDatabaseProperties() { return _databaseProperties; }
    public ServerProperties getServerProperties() { return _serverProperties; }
    public BitcoinProperties getBitcoinProperties() { return _bitcoinProperties; }
    public TokenGenesisConfiguration getTokenGenesisConfiguration() { return _tokenGenesisConfiguration; }
    public RedemptionItemConfiguration getRedemptionItemConfiguration() { return _redemptionItemConfiguration; }
}
