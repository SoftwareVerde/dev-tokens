package com.softwareverde.devtokens.configuration;

import com.softwareverde.database.properties.MutableDatabaseProperties;

import java.io.File;

public class DatabaseProperties extends MutableDatabaseProperties {
    protected Boolean _shouldUseEmbeddedDatabase;
    protected File _dataDirectory;
    protected File _installationDirectory;

    public DatabaseProperties() { }

    public DatabaseProperties(final DatabaseProperties databaseProperties) {
        super(databaseProperties);
        _shouldUseEmbeddedDatabase = databaseProperties.shouldUseEmbeddedDatabase();
        _dataDirectory = databaseProperties.getDataDirectory();
        _installationDirectory = databaseProperties.getInstallationDirectory();
    }

    public Boolean shouldUseEmbeddedDatabase() { return _shouldUseEmbeddedDatabase; }
    public File getDataDirectory() { return _dataDirectory; }
    public File getInstallationDirectory() { return _installationDirectory; }

    public void setShouldUseEmbeddedDatabase(final Boolean shouldUseEmbeddedDatabase) {
        _shouldUseEmbeddedDatabase = shouldUseEmbeddedDatabase;
    }

    public void setDataDirectory(final File dataDirectory) {
        _dataDirectory = dataDirectory;
    }

    public void setInstallationDirectory(final File installationDirectory) {
        _installationDirectory = installationDirectory;
    }
}