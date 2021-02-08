package com.softwareverde.devtokens;

import com.softwareverde.constable.list.List;
import com.softwareverde.json.Json;
import com.softwareverde.json.Jsonable;
import com.softwareverde.util.type.identifier.Identifier;

public class RedemptionItem implements Jsonable {
    public static class ItemId extends Identifier {
        public static ItemId fromLong(final Long id) {
            if ( (id == null) || (id < 0) ) { return null; }

            return new ItemId(id);
        }

        protected ItemId(final Long value) {
            super(value);
        }
    }

    protected final ItemId _id;
    protected final String _name;
    protected final Long _tokenAmount;
    protected final List<String> _requiredFields;
    protected final List<String> _optionalFields;
    protected final String _category;
    protected final String _shortDescription;
    protected final String _longDescription;
    protected final String _formDescription;

    protected static Json formatField(final String fieldName) {
        final Json json = new Json(false);

        final String displayName;
        {
            final StringBuilder stringBuilder = new StringBuilder();
            final String[] displayNameWords = fieldName.split("_");
            int index = 0;
            for (final String displayNameWord : displayNameWords) {
                final boolean isLastWord = (index == (displayNameWords.length - 1));
                if (! displayNameWord.isEmpty()) {
                    stringBuilder.append(Character.toUpperCase(displayNameWord.charAt(0)));
                    stringBuilder.append(displayNameWord.substring(1));
                }
                if (! isLastWord) {
                    stringBuilder.append(" ");
                }
                index += 1;
            }
            displayName = stringBuilder.toString();
        }

        json.put("name", fieldName);
        json.put("label", displayName);

        return json;
    }

    public RedemptionItem(final ItemId itemId, final String name, final Long tokenAmount, final List<String> requiredFields, final List<String> optionalFields, final String category, final String shortDescription, final String longDescription, final String formDescription) {
        _id = itemId;
        _name = name;
        _tokenAmount = tokenAmount;
        _requiredFields = requiredFields.asConst();
        _optionalFields = optionalFields;

        _category = category;
        _shortDescription = shortDescription;
        _longDescription = longDescription;
        _formDescription = formDescription;
    }

    public ItemId getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public List<String> getRequiredFields() {
        return _requiredFields;
    }

    public List<String> getOptionalFields() {
        return _optionalFields;
    }

    public Long getTokenAmount() {
        return _tokenAmount;
    }

    public String getCategory() {
        return _category;
    }

    public String getShortDescription() {
        return _shortDescription;
    }

    public String getLongDescription() {
        return _longDescription;
    }

    public String getFormDescription() {
        return _formDescription;
    }

    @Override
    public Json toJson() {
        final Json json = new Json(false);

        json.put("id", _id);
        json.put("name", _name);
        json.put("tokenAmount", _tokenAmount);

        final Json requiredFieldsJson = new Json(true);
        for (final String requiredField : _requiredFields) {
            requiredFieldsJson.add(RedemptionItem.formatField(requiredField));
        }
        json.put("requiredFields", requiredFieldsJson);

        final Json optionalFieldsJson = new Json(true);
        for (final String optionalField : _optionalFields) {
            optionalFieldsJson.add(RedemptionItem.formatField(optionalField));
        }
        json.put("optionalFields", optionalFieldsJson);

        json.put("category", _category);
        json.put("shortDescription", _shortDescription);
        json.put("longDescription", _longDescription);
        json.put("formDescription", _formDescription);

        return json;
    }
}
