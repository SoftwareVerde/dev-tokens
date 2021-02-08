package com.softwareverde.devtokens.database;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.inflater.MasterInflater;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.query.Query;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.hash.Hash;
import com.softwareverde.cryptography.hash.sha256.Sha256Hash;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.cryptography.util.HashUtil;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.row.Row;
import com.softwareverde.devtokens.RedemptionItem;
import com.softwareverde.json.Json;
import com.softwareverde.util.StringUtil;

public class DatabaseManager implements AutoCloseable {
    protected final MasterInflater _masterInflater;
    protected final DatabaseConnection _databaseConnection;

    /**
     * Returns a canonical Hash of the Json object, assuming the Json object is one-dimensional.
     */
    protected Hash _hashJson(final Json json) {
        if (json.isArray()) {
            final String jsonString = json.toString();
            final ByteArray jsonStringBytes = ByteArray.wrap(StringUtil.stringToBytes(jsonString));
            return HashUtil.sha256(jsonStringBytes);
        }

        final MutableList<String> sortedKeys = new MutableList<String>();
        sortedKeys.addAll(json.getKeys());
        sortedKeys.sort(String::compareTo);

        final StringBuilder stringBuilder = new StringBuilder();
        for (final String key : sortedKeys) {
            final String value = json.getString(key);

            stringBuilder.append(key);
            stringBuilder.append(":");
            stringBuilder.append(value);
        }
        final String canonicalString = stringBuilder.toString();

        final ByteArray jsonStringBytes = ByteArray.wrap(StringUtil.stringToBytes(canonicalString));
        return HashUtil.sha256(jsonStringBytes);
    }

    public DatabaseManager(final DatabaseConnection databaseConnection, final MasterInflater masterInflater) {
        _masterInflater = masterInflater;
        _databaseConnection = databaseConnection;
    }

    public void storeDonationAddress(final PrivateKey privateKey, final Address address, final Address returnAddress) throws DatabaseException {
        final String privateKeyString = (privateKey != null ? privateKey.toString() : null);
        final String addressString = address.toBase58CheckEncoded();
        final String returnAddressString = (returnAddress != null ? returnAddress.toBase58CheckEncoded() : null);

        _databaseConnection.executeSql(
            new Query("INSERT IGNORE INTO donation_addresses (private_key, address, return_address) VALUES (?, ?, ?)")
                .setParameter(privateKeyString)
                .setParameter(addressString)
                .setParameter(returnAddressString)
        );
    }

    public Address getReturnAddress(final Address address) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, return_address FROM donation_addresses WHERE address = ?")
                .setParameter(address.toBase58CheckEncoded())
        );
        if (rows.isEmpty()) { return null; }
        final Row row = rows.get(0);

        final String returnAddress = row.getString("return_address");
        if (returnAddress == null) { return null; }

        final AddressInflater addressInflater = _masterInflater.getAddressInflater();
        return addressInflater.fromBase58Check(returnAddress);
    }

    public RedemptionItem.ItemId getRedemptionItem(final Address address) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, item_id FROM redemption_addresses WHERE address = ?")
                .setParameter(address.toBase58CheckEncoded())
        );

        if (rows.isEmpty()) { return null; }
        final Row row = rows.get(0);

        final Long itemId = row.getLong("item_id");
        return RedemptionItem.ItemId.fromLong(itemId);
    }

    public Address getRedemptionAddress(final Json formData) throws DatabaseException {
        final Hash formDataHash = _hashJson(formData);

        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, address FROM redemption_addresses WHERE hash = ?")
                .setParameter(formDataHash)
        );

        if (rows.isEmpty()) { return null; }
        final Row row = rows.get(0);

        final AddressInflater addressInflater = _masterInflater.getAddressInflater();
        final String addressString = row.getString("address");
        return addressInflater.fromBase58Check(addressString);
    }

    public void storeRedemptionAddress(final RedemptionItem.ItemId itemId, final PrivateKey privateKey, final Address address, final Json formData) throws DatabaseException {
        final Hash formDataHash = _hashJson(formData);

        _databaseConnection.executeSql(
            new Query("INSERT IGNORE INTO redemption_addresses (item_id, hash, private_key, address, form_data) VALUES (?, ?, ?, ?, ?)")
                .setParameter(itemId)
                .setParameter(formDataHash)
                .setParameter(privateKey.toString())
                .setParameter(address.toBase58CheckEncoded())
                .setParameter(formData.toString())
        );
    }

    public List<Address> getDonationAddresses() throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, address FROM donation_addresses")
        );

        final MutableList<Address> addresses = new MutableList<Address>(rows.size());
        final AddressInflater addressInflater = _masterInflater.getAddressInflater();
        for (final Row row : rows) {
            final String addressString = row.getString("address");
            final Address address = addressInflater.fromBase58Check(addressString);
            if (address != null) {
                addresses.add(address);
            }
        }
        return addresses;
    }

    public List<Address> getRedemptionAddresses() throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, address FROM redemption_addresses")
        );

        final MutableList<Address> addresses = new MutableList<Address>(rows.size());
        final AddressInflater addressInflater = _masterInflater.getAddressInflater();
        for (final Row row : rows) {
            final String addressString = row.getString("address");
            final Address address = addressInflater.fromBase58Check(addressString);
            if (address != null) {
                addresses.add(address);
            }
        }
        return addresses;
    }

    public Json getRedemptionFormData(final Address redemptionAddress) throws DatabaseException {
        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, form_data FROM redemption_addresses WHERE address = ?")
                .setParameter(redemptionAddress.toBase58CheckEncoded())
        );
        if (rows.isEmpty()) { return null; }

        final Row row = rows.get(0);
        final String formDataString = row.getString("form_data");
        return Json.parse(formDataString);
    }

    public PrivateKey getPrivateKey(final Address address) throws DatabaseException {
        if (address == null) { return null; }

        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id, private_key FROM donation_addresses WHERE address = ?")
                .setParameter(address.toBase58CheckEncoded())
        );

        rows.addAll(_databaseConnection.query(
            new Query("SELECT id, private_key FROM redemption_addresses WHERE address = ?")
                .setParameter(address.toBase58CheckEncoded())
        ));

        for (final Row row : rows) {
            final String privateKeyString = row.getString("private_key");
            if (privateKeyString != null) {
                return PrivateKey.fromHexString(privateKeyString);
            }
        }
        return null;
    }

    public Boolean wasTransactionProcessed(final Sha256Hash transactionHash) throws DatabaseException {
        if (transactionHash == null) { return false; }

        final java.util.List<Row> rows = _databaseConnection.query(
            new Query("SELECT id FROM processed_transactions WHERE hash = ?")
                .setParameter(transactionHash.toString())
        );

        return (rows.size() > 0);
    }

    public void markTransactionAsProcessed(final Sha256Hash transactionHash) throws DatabaseException {
        if (transactionHash == null) { return; }

        _databaseConnection.executeSql(
            new Query("INSERT INTO processed_transactions (hash) VALUES (?)")
                .setParameter(transactionHash.toString())
        );
    }

    public DatabaseConnection getDatabaseConnection() {
        return _databaseConnection;
    }

    @Override
    public void close() throws DatabaseException {
        _databaseConnection.close();
    }
}
