package com.softwareverde.devtokens.database;

import com.softwareverde.bitcoin.server.database.Database;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.DatabaseConnectionFactory;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.DatabaseInitializer;
import com.softwareverde.database.mysql.MysqlDatabase;
import com.softwareverde.database.mysql.MysqlDatabaseConnection;
import com.softwareverde.database.mysql.MysqlDatabaseConnectionFactory;
import com.softwareverde.database.mysql.MysqlDatabaseInitializer;
import com.softwareverde.database.mysql.embedded.EmbeddedMysqlDatabase;
import com.softwareverde.database.mysql.embedded.properties.MutableEmbeddedDatabaseProperties;
import com.softwareverde.database.properties.DatabaseCredentials;
import com.softwareverde.devtokens.configuration.DatabaseProperties;
import com.softwareverde.logging.Logger;

import java.io.File;
import java.sql.Connection;

public class DevTokensDatabase implements Database {
    public static class InitFile {
        public final String sqlInitFile;
        public final Integer databaseVersion;

        public InitFile(final String sqlInitFile, final Integer databaseVersion) {
            this.sqlInitFile = sqlInitFile;
            this.databaseVersion = databaseVersion;
        }
    }

    public static final InitFile DEV_TOKENS = new InitFile("/sql/init.sql", 1);

    public static final DatabaseInitializer.DatabaseUpgradeHandler<Connection> DATABASE_UPGRADE_HANDLER = new DatabaseInitializer.DatabaseUpgradeHandler<Connection>() {
        @Override
        public Boolean onUpgrade(final com.softwareverde.database.DatabaseConnection<Connection> maintenanceDatabaseConnection, final Integer currentVersion, final Integer requiredVersion) {
            return false;
        }
    };

    public static DevTokensDatabase newInstance(final InitFile sqlInitFile, final DatabaseProperties databaseProperties) {
        final DatabaseInitializer<Connection> databaseInitializer = new MysqlDatabaseInitializer(sqlInitFile.sqlInitFile, sqlInitFile.databaseVersion, DevTokensDatabase.DATABASE_UPGRADE_HANDLER);

        final MutableEmbeddedDatabaseProperties embeddedDatabaseProperties = new MutableEmbeddedDatabaseProperties(databaseProperties);
        {
            // Copy configuration settings specific to BitcoinVerdeDatabaseProperties from the provided property set...
            final File installationDirectory = databaseProperties.getInstallationDirectory();
            final File dataDirectory = databaseProperties.getDataDirectory();

            embeddedDatabaseProperties.setInstallationDirectory(installationDirectory);
            embeddedDatabaseProperties.setDataDirectory(dataDirectory);

            // Disable ONLY_FULL_GROUP_BY, required since MariaDb v5.7.5 to allow under-constrained column values via group-by.
            embeddedDatabaseProperties.addArgument("--sql_mode='STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'");
        }

        try {
            if (databaseProperties.shouldUseEmbeddedDatabase()) {
                // Initialize the embedded database...

                Logger.info("[Initializing Database]");
                final EmbeddedMysqlDatabase embeddedMysqlDatabase = new EmbeddedMysqlDatabase(embeddedDatabaseProperties, databaseInitializer);
                embeddedMysqlDatabase.start();

                final DatabaseCredentials maintenanceCredentials = databaseInitializer.getMaintenanceCredentials(databaseProperties);
                final MysqlDatabaseConnectionFactory maintenanceDatabaseConnectionFactory = new MysqlDatabaseConnectionFactory(databaseProperties, maintenanceCredentials);
                return new DevTokensDatabase(embeddedMysqlDatabase, maintenanceDatabaseConnectionFactory);
            }
            else {
                // Connect to the remote database...
                final DatabaseCredentials credentials = databaseProperties.getCredentials();
                final DatabaseCredentials rootCredentials = databaseProperties.getRootCredentials();
                final DatabaseCredentials maintenanceCredentials = databaseInitializer.getMaintenanceCredentials(databaseProperties);

                final MysqlDatabaseConnectionFactory rootDatabaseConnectionFactory = new MysqlDatabaseConnectionFactory(databaseProperties.getHostname(), databaseProperties.getPort(), "", rootCredentials.username, rootCredentials.password);
                final MysqlDatabaseConnectionFactory maintenanceDatabaseConnectionFactory = new MysqlDatabaseConnectionFactory(databaseProperties, maintenanceCredentials);

                try (final MysqlDatabaseConnection maintenanceDatabaseConnection = maintenanceDatabaseConnectionFactory.newConnection()) {
                    final Integer databaseVersion = databaseInitializer.getDatabaseVersionNumber(maintenanceDatabaseConnection);
                    if (databaseVersion < 0) {
                        try (final MysqlDatabaseConnection rootDatabaseConnection = rootDatabaseConnectionFactory.newConnection()) {
                            databaseInitializer.initializeSchema(rootDatabaseConnection, databaseProperties);
                        }
                    }
                }
                catch (final DatabaseException exception) {
                    try (final MysqlDatabaseConnection rootDatabaseConnection = rootDatabaseConnectionFactory.newConnection()) {
                        databaseInitializer.initializeSchema(rootDatabaseConnection, databaseProperties);
                    }
                }

                try (final MysqlDatabaseConnection maintenanceDatabaseConnection = maintenanceDatabaseConnectionFactory.newConnection()) {
                    databaseInitializer.initializeDatabase(maintenanceDatabaseConnection);
                }

                return new DevTokensDatabase(new MysqlDatabase(databaseProperties, credentials), maintenanceDatabaseConnectionFactory);
            }
        }
        catch (final DatabaseException exception) {
            Logger.error(exception);
        }

        return null;
    }

    public static MysqlDatabaseConnectionFactory getMaintenanceDatabaseConnectionFactory(final DatabaseProperties databaseProperties) {
        final DatabaseInitializer<Connection> databaseInitializer = new MysqlDatabaseInitializer();
        final DatabaseCredentials maintenanceCredentials = databaseInitializer.getMaintenanceCredentials(databaseProperties);
        return new MysqlDatabaseConnectionFactory(databaseProperties, maintenanceCredentials);
    }

    protected final MysqlDatabase _core;
    protected final MysqlDatabaseConnectionFactory _maintenanceDatabaseConnectionFactory;

    protected DevTokensDatabase(final MysqlDatabase core, final MysqlDatabaseConnectionFactory maintenanceDatabaseConnectionFactory) {
        _core = core;
        _maintenanceDatabaseConnectionFactory = maintenanceDatabaseConnectionFactory;
    }

    @Override
    public DatabaseConnection newConnection() throws DatabaseException {
        return new MysqlDatabaseConnectionWrapper(_core.newConnection());
    }

    public DatabaseConnection getMaintenanceConnection() throws DatabaseException {
        if (_maintenanceDatabaseConnectionFactory == null) { return null; }
        return new MysqlDatabaseConnectionWrapper(_maintenanceDatabaseConnectionFactory.newConnection());
    }

    @Override
    public DatabaseConnectionFactory newConnectionFactory() {
        return new MysqlDatabaseConnectionFactoryWrapper(_core.newConnectionFactory());
    }

    @Override
    public Integer getMaxQueryBatchSize() {
        return 1024;
    }

    @Override
    public void close() throws DatabaseException { }
}
