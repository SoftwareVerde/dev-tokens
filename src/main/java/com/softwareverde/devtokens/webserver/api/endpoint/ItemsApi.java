package com.softwareverde.devtokens.webserver.api.endpoint;

import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.devtokens.RedemptionItem;
import com.softwareverde.devtokens.configuration.RedemptionItemConfiguration;
import com.softwareverde.devtokens.webserver.Environment;
import com.softwareverde.devtokens.webserver.api.ApiResult;
import com.softwareverde.devtokens.webserver.api.v1.get.RedemptionItemsHandler;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.json.Json;
import com.softwareverde.util.Util;

import java.util.HashMap;

public class ItemsApi extends ApiEndpoint {
    public static class GetRedemptionItemsResult extends ApiResult {
        private final MutableList<String> _redemptionCategories = new MutableList<>();
        private final HashMap<String, MutableList<RedemptionItem>> _redemptionItemsByCategory = new HashMap<>();

        public void addRedemptionItem(final RedemptionItem redemptionItem) {
            final String category = redemptionItem.getCategory();
            if (! _redemptionCategories.contains(category)) {
                _redemptionCategories.add(category);
            }

            final MutableList<RedemptionItem> redemptionItemsForCategory = Util.coalesce(_redemptionItemsByCategory.get(category), new MutableList<>());
            redemptionItemsForCategory.add(redemptionItem);
            _redemptionItemsByCategory.put(category, redemptionItemsForCategory);
        }

        @Override
        public Json toJson() {
            final Json json = super.toJson();

            final Json redemptionItemsJson = new Json(false);
            for (final String category : _redemptionCategories) {
                final Json redemptionItemsForCategoryJson = new Json(true);
                for (final RedemptionItem redemptionItem : _redemptionItemsByCategory.get(category)) {
                    redemptionItemsForCategoryJson.add(redemptionItem);
                }
                redemptionItemsJson.put(category, redemptionItemsForCategoryJson);
            }

            json.put("redemptionItemsByCategory", redemptionItemsJson);

            return json;
        }
    }

    public ItemsApi(final String apiPrePath, final Environment environment) {
        super(environment);

        final RedemptionItemConfiguration redemptionItemConfiguration = environment.getRedemptionItemConfiguration();
        _defineEndpoint((apiPrePath + "/items"), HttpMethod.GET, new RedemptionItemsHandler(redemptionItemConfiguration));
    }
}
