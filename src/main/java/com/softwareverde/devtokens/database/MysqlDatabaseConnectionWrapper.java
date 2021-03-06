package com.softwareverde.devtokens.database;

import com.softwareverde.bitcoin.server.database.DatabaseConnectionCore;
import com.softwareverde.database.mysql.MysqlDatabaseConnection;

public class MysqlDatabaseConnectionWrapper extends DatabaseConnectionCore {
    public MysqlDatabaseConnectionWrapper(final MysqlDatabaseConnection core) {
        super(core);
    }

    @Override
    public Integer getRowsAffectedCount() {
        return ((MysqlDatabaseConnection) _core).getRowsAffectedCount();
    }
}
