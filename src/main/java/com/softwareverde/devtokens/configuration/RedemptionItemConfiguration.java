package com.softwareverde.devtokens.configuration;

import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.immutable.ImmutableList;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.devtokens.RedemptionItem;
import com.softwareverde.json.Json;
import com.softwareverde.logging.Logger;

import java.util.HashMap;

public class RedemptionItemConfiguration {
    public static RedemptionItemConfiguration parse(final Json json) {
        if (! json.isArray()) { return null; }
        if (json.length() == 0) { return null; }

        final MutableList<RedemptionItem> redemptionItems = new MutableList<>();
        for (int i = 0; i < json.length(); ++i) {
            final Json redemptionItemJson = json.get(i);

            final Long itemIdLong = redemptionItemJson.getOrNull("id", Json.Types.LONG);
            final RedemptionItem.ItemId itemId = RedemptionItem.ItemId.fromLong(itemIdLong);
            if (itemId == null) {
                Logger.error("Invalid or missing property: id");
                return null;
            }

            final String itemName = redemptionItemJson.getOrNull("name", Json.Types.STRING);
            if (itemName == null) {
                Logger.error("Invalid or missing property: name");
                return null;
            }

            final Long tokenAmount = redemptionItemJson.getLong("tokenAmount");
            if (tokenAmount == null || tokenAmount < 1) {
                Logger.error("Invalid or missing property: tokenAmount");
                return null;
            }

            final MutableList<String> requiredFields = new MutableList<>();
            final Json requiredFieldsJson = redemptionItemJson.get("requiredFields");
            for (int j = 0; j < requiredFieldsJson.length(); ++j) {
                final String fieldName = requiredFieldsJson.getString(j);
                requiredFields.add(fieldName);
            }

            final MutableList<String> optionalFields = new MutableList<>();
            final Json optionalFieldsJson = redemptionItemJson.get("optionalFields");
            for (int j = 0; j < optionalFieldsJson.length(); ++j) {
                final String fieldName = optionalFieldsJson.getString(j);
                optionalFields.add(fieldName);
            }

            final String category = redemptionItemJson.getString("category");
            final String shortDescription = redemptionItemJson.getString("shortDescription");
            final String longDescription = redemptionItemJson.getString("longDescription");
            final String formDescription = redemptionItemJson.getString("formDescription");

            final RedemptionItem redemptionItem = new RedemptionItem(itemId, itemName, tokenAmount, requiredFields, optionalFields, category, shortDescription, longDescription, formDescription);
            redemptionItems.add(redemptionItem);
        }

        final RedemptionItemConfiguration redemptionItemConfiguration = new RedemptionItemConfiguration();

        for (final RedemptionItem redemptionItem : redemptionItems) {
            redemptionItemConfiguration._redemptionItemMap.put(redemptionItem.getId(), redemptionItem);
        }

        return redemptionItemConfiguration;
    }

    protected final HashMap<RedemptionItem.ItemId, RedemptionItem> _redemptionItemMap = new HashMap<>();

    protected RedemptionItemConfiguration() { }

    public RedemptionItem getRedemptionItemById(final RedemptionItem.ItemId itemId) {
        if (itemId == null) { return null; }
        return _redemptionItemMap.get(itemId);
    }

    public List<RedemptionItem.ItemId> getRedemptionItemIds() {
        return new ImmutableList<RedemptionItem.ItemId>(_redemptionItemMap.keySet());
    }
}
