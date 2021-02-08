package com.softwareverde.devtokens.configuration;

import com.softwareverde.util.Util;

import java.io.File;
import java.util.Properties;

public class DatabasePropertiesLoader {
    public static DatabaseProperties loadDatabaseProperties(final String prefix, final Properties properties) {
        final String propertyPrefix = (prefix == null ? "" : (prefix + "."));
        final String rootPassword = properties.getProperty(propertyPrefix + "database.rootPassword", "6137cde4893c59f76f005a8123d8e8e6");
        final String hostname = properties.getProperty(propertyPrefix + "database.hostname", "localhost");
        final String username = properties.getProperty(propertyPrefix + "database.username", "devtokens");
        final String password = properties.getProperty(propertyPrefix + "database.password", "64f3d85b9553648f1057ad28abbc4970");
        final String schema = (properties.getProperty(propertyPrefix + "database.schema", "devtokens")).replaceAll("[^A-Za-z0-9_]", "");
        final Integer port = Util.parseInt(properties.getProperty(propertyPrefix + "database.port", "8806"));
        final String dataDirectory = properties.getProperty(propertyPrefix + "database.dataDirectory", "data");
        final String mysqlInstallationDirectory = properties.getProperty(propertyPrefix + "database.installationDirectory", "mysql");
        final Boolean useEmbeddedDatabase = Util.parseBool(properties.getProperty(propertyPrefix + "database.useEmbeddedDatabase", "1"));

        final File dataDirectoryFile = new File(dataDirectory);

        final DatabaseProperties databaseProperties = new DatabaseProperties();
        databaseProperties.setRootPassword(rootPassword);
        databaseProperties.setHostname(hostname);
        databaseProperties.setUsername(username);
        databaseProperties.setPassword(password);
        databaseProperties.setSchema(schema);
        databaseProperties.setPort(port);
        databaseProperties.setDataDirectory(dataDirectoryFile);
        databaseProperties._shouldUseEmbeddedDatabase = useEmbeddedDatabase;

        final File installationDirectory = new File(mysqlInstallationDirectory);
        databaseProperties.setInstallationDirectory(installationDirectory);

        return databaseProperties;
    }

    protected DatabasePropertiesLoader() { }
}
