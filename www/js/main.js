window.state = {
    tokenName: "Dev Tokens",
    donationAddress: null,
    redemptionAddresses: [],
    currentRedemptionItemId: null,
    donationAmount: 0,
    tokenExchangeRate: null,
    webSocket: {
        pendingReceivedMessages: []
    },
    initialized: false,

    initialize: function(addressJson, returnAddress, tokenExchangeRate) {
        if (addressJson) {
            window.state.donationAddress = addressJson;

            // Since returnAddress may be null and is an optional parameter when set with tokenExchangeRate,
            //  the returnAddress is only set when the addressJson is set.
            window.state.returnAddress = returnAddress;
        }
        if (tokenExchangeRate) {
            window.state.tokenExchangeRate = tokenExchangeRate;
        }

        $(".redemption-widget").each(function(i, item) {
            const container = $(item).parent();
            const itemId = window.parseInt($(".redemption-widget", container).data("item-id"));
            const tokenAmount = window.state.tokenExchangeRate.redemptionItems[itemId];

            if (tokenAmount) {
                $("h5.dev-tokens", container).text(Number(tokenAmount).toLocaleString() + " " + window.state.tokenName);
            }
        });

        const bchPerToken = (window.state.tokenExchangeRate.satoshisPerDevToken / 100000000).toFixed(8);
        $(".token-exchange-rate").each(function(i, item) {
            $(item).text(bchPerToken);
        });

        window.state.initialized = true;

        const pendingReceivedMessages = window.state.webSocket.pendingReceivedMessages;
        for (let i in pendingReceivedMessages) {
            const message = pendingReceivedMessages[i];
            webSocket.onmessage(message);
        }
        window.state.webSocket.pendingReceivedMessages.length = 0;
    },

    queueWebSocketMessage: function(event) {
        window.state.webSocket.pendingReceivedMessages.push(event);
    }
};

window.webSocket = null;

function showDialog(title, message, callback, alternateColor) {
    const dialog = $("#dialog");
    const body = $("body");
    const overlay = $("#dialog-overlay");

    dialog.toggleClass("alt", (alternateColor ? true : false));

    $("h2", dialog).text(title);
    $("p", dialog).html(message);

    body.toggleClass("no-scroll", true);
    dialog.toggleClass("visible", true);
    overlay.toggleClass("visible", true);

    window.closeDialog.callback = callback;
}

function closeDialog() {
    const dialog = $("#dialog");
    const body = $("body");
    const overlay = $("#dialog-overlay");

    const noScroll = ($("#redemption-card-overlay.visible").length > 0);

    body.toggleClass("no-scroll", noScroll);
    dialog.toggleClass("visible", false);
    overlay.toggleClass("visible", false);

    if ((typeof window.closeDialog.callback) == "function") {
        const callback = window.closeDialog.callback;
        window.closeDialog.callback = null;
        callback();
    }
}

function renderInput(inputField) {
    const container = $(inputField).parent();
    const label = $("label", container);
    const input = $("input", container);

    label.toggleClass("hidden", (input.val().length > 0));
};

function getTokenExchangeRate(callback) {
    $.get(
        "/api/v1/exchange-rate",
        {},
        function(data) {
            if (typeof callback == "function") {
                callback(data);
            }
        }
    ).fail(function(data) {
        if (typeof callback == "function") {
            callback(JSON.parse(data.responseText || "{}"));
        }
    });
}

function getRedemptionItems(callback) {
    $.get(
        "/api/v1/items",
        {},
        function(data) {
            if (typeof callback == "function") {
                callback(data);
            }
        }
    ).fail(function(data) {
        if (typeof callback == "function") {
            callback(JSON.parse(data.responseText || "{}"));
        }
    });
}

function createNewDonationAddress(returnAddress, callback) {
    $.post(
        "/api/v1/donate/new",
        {
            return_address: returnAddress
        },
        function(data) {
            if (typeof callback == "function") {
                callback(data);
            }
        }
    ).fail(function(data) {
        if (typeof callback == "function") {
            callback(JSON.parse(data.responseText || "{}"));
        }
    });
}

function validateSlpAddress(inputString, callback) {
    $.post(
        "/api/v1/address/slp/validate",
        {
            address: inputString
        },
        function(data) {
            if (typeof callback == "function") {
                callback(data);
            }
        }
    ).fail(function(data) {
        if (typeof callback == "function") {
            callback(JSON.parse(data.responseText || "{}"));
        }
    });
}

function createNewRedemptionAddress(itemId, formData, callback) {
    $.post(
        "/api/v1/redeem/" + itemId + "/new",
        formData,
        function(data) {
            if (typeof callback == "function") {
                callback(data);
            }
        }
    ).fail(function(data) {
        if (typeof callback == "function") {
            callback(JSON.parse(data.responseText || "{}"));
        }
    });
}

function updateDonationAddressUi(resultJson) {
    const qrCodeContainer = $("#donation-address-image");
    const donationAddressLink = $("#donation-address-text");
    const returnAddressInput = $("#donation-container .input > input");

    if (! resultJson.wasSuccess) {
        qrCodeContainer.toggleClass("error", true);
        qrCodeContainer.empty();

        donationAddressLink.toggleClass("error", true);
        donationAddressLink.text(resultJson.errorMessage || "");
        donationAddressLink.attr("href", null);

        returnAddressInput.val("");
        window.renderInput(returnAddressInput);
    }
    else {
        const addressJson = resultJson.address;

        const addressString = (addressJson.base32CheckEncoded || "");
        const qrCodeElement = window.ninja.qrCode.createCanvas(addressString, 8);
        const returnAddress = resultJson.returnAddress;

        qrCodeContainer.toggleClass("error", false);
        qrCodeContainer.empty();
        qrCodeContainer.append(qrCodeElement);

        donationAddressLink.toggleClass("error", false);
        donationAddressLink.text(addressJson.base32CheckEncoded || "");
        donationAddressLink.attr("href", addressJson.base32CheckEncoded);
        returnAddressInput.val(returnAddress);
        window.renderInput(returnAddressInput);
    }
}

function inflateRedemptionItems(redemptionItemsJson) {
    const container = $("<div></div>");

    for (let category in redemptionItemsJson) {
        const categoryItems = redemptionItemsJson[category];

        const categoryContainer = $("#templates .redemption-category").clone();
        $("h3", categoryContainer).text(category);

        for (let i in categoryItems) {
            const redemptionItemJson = categoryItems[i];

            const cardContainer = $("#templates .redemption-card").clone();
            $(".redemption-widget", cardContainer).attr("data-item-id", redemptionItemJson.id);
            $("h4", cardContainer).text(redemptionItemJson.name);
            $(".short-description", cardContainer).text(redemptionItemJson.shortDescription);
            $(".long-description", cardContainer).text(redemptionItemJson.longDescription);
            $(".form-description", cardContainer).text(redemptionItemJson.formDescription);

            const requiredFields = redemptionItemJson.requiredFields;
            for (let j in requiredFields) {
                const requiredField = requiredFields[j];
                const inputContainer = $("<div></div>");
                inputContainer.toggleClass("input", true);
                inputContainer.toggleClass("required", true);

                const labelElement = $("<label></label");
                labelElement.text(requiredField.label);
                inputContainer.append(labelElement);

                const inputElement = $("<input></input");
                inputElement.text(requiredField.name);
                inputElement.attr("type", "text");
                inputContainer.append(inputElement);

                $("form .inputs", cardContainer).append(inputContainer);
            }

            const optionalFields = redemptionItemJson.optionalFields;
            for (let j in optionalFields) {
                const optionalField = optionalFields[j];
                const inputContainer = $("<div></div>");
                inputContainer.toggleClass("input", true);
                inputContainer.toggleClass("optional", true);

                const labelElement = $("<label></label");
                labelElement.text(optionalField.label);
                inputContainer.append(labelElement);

                const inputElement = $("<input></input");
                inputElement.text(optionalField.name);
                inputElement.attr("type", "text");
                inputContainer.append(inputElement);

                $("form .inputs", cardContainer).append(inputContainer);
            }

            categoryContainer.append(cardContainer);
        }

        container.append(categoryContainer);
    }
    
    return container;
}

function inflateRedemptionContainer(addressJson) {
    const container = $("#templates .redemption-container").clone();
    if (addressJson === false) {
        return container;
    }

    const addressString = (addressJson.slpBase32CheckEncoded || "");
    const qrCodeElement = window.ninja.qrCode.createCanvas(addressString, 8);

    const qrCodeContainer = $(".qr-code", container);
    qrCodeContainer.toggleClass("error", false);
    qrCodeContainer.empty();
    qrCodeContainer.append(qrCodeElement);

    const donationAddressLink = $(".address", container);
    donationAddressLink.toggleClass("error", false);
    donationAddressLink.text(addressJson.slpBase32CheckEncoded || "");
    donationAddressLink.attr("href", addressJson.slpBase32CheckEncoded);

    return container;
}

function renderTokenBalance(itemId) {
    const container = $(".redemption-widget[data-item-id=" + itemId + "]").closest(".redemption-card");
    if (! container) { return; }

    const tokenBalanceContainer = $(".token-balance", container);
    const redemptionAddress = window.state.redemptionAddresses[itemId];
    const tokenBalance = ((redemptionAddress ? redemptionAddress.tokenBalance : 0) || 0);
    if (tokenBalance > 0) {
        tokenBalanceContainer.text("Balance: " + tokenBalance);
    }
};

function init() {
    const localStorage = window.localStorage;

    let savedDonationAddress = null;
    try {
        savedDonationAddress = JSON.parse(localStorage.getItem("donationAddress"));
    }
    catch (exception) { }

    window.getTokenExchangeRate(function(tokenExchangeRate) {
        if (! tokenExchangeRate.wasSuccess) {
            console.log("Unable to load exchange range. " + tokenExchangeRate.errorMessage);
            return;
        }

        if ( (savedDonationAddress != null) && savedDonationAddress.wasSuccess ) {
            const addressJson = savedDonationAddress.address;
            const returnAddress = savedDonationAddress.returnAddress;
            window.state.initialize(addressJson, returnAddress, tokenExchangeRate);
            window.updateDonationAddressUi(savedDonationAddress);
        }
        else {
            window.createNewDonationAddress(null, function(resultJson) {
                if (resultJson.wasSuccess) {
                    const addressJson = resultJson.address;
                    const returnAddress = resultJson.returnAddress;
                    window.state.initialize(addressJson, returnAddress, tokenExchangeRate);
                }

                localStorage.setItem("donationAddress", JSON.stringify(resultJson));
                window.updateDonationAddressUi(resultJson);
            });
        }
    });

    if (window.location.protocol == "http:") {
        webSocket = new WebSocket("ws://" + window.location.host + "/api/v1/announcements");
    }
    else {
        webSocket = new WebSocket("wss://" + window.location.host + "/api/v1/announcements");
    }

    webSocket.onopen = function() { };

    webSocket.onmessage = function(event) {
        if (! window.state.initialized) {
            window.state.queueWebSocketMessage(event);
            return;
        }

        const message = JSON.parse(event.data);

        let donationAddress = null;
        if (window.state.donationAddress) {
            donationAddress = window.state.donationAddress.base58CheckEncoded;
        }

        if (message.outputs) {
            const outputs = message.outputs;
            let newTransactionSlpRedemptionAmount = 0;
            let donationAmount = 0;
            for (let i in outputs) {
                const output = outputs[i];
                if (output.address) {
                    const outputAddress = output.address;
                    const outputAmount = output.amount;
                    if (donationAddress == outputAddress) {
                        donationAmount += outputAmount;
                    }

                    if (output.slpAmount > 0) {
                        for (let i in window.state.redemptionAddresses) {
                            const redemptionAddress = window.state.redemptionAddresses[i].base58CheckEncoded;
                            if (redemptionAddress == outputAddress) {
                                newTransactionSlpRedemptionAmount += output.slpAmount;
                            }
                        }
                    }
                }
            }

            if (donationAmount > 0) {
                window.state.donationAmount += donationAmount;
                let hasAlreadyCelebrated = false;
                try {
                    let savedDonationAddress = JSON.parse(localStorage.getItem("donationAddress"));
                    hasAlreadyCelebrated = (window.state.donationAmount <= (savedDonationAddress.donationAmount || 0));
                    if (! hasAlreadyCelebrated) {
                        savedDonationAddress.donationAmount = window.state.donationAmount;
                        localStorage.setItem("donationAddress", JSON.stringify(savedDonationAddress));
                    }
                }
                catch (exception) { }

                if (! hasAlreadyCelebrated) {
                    $("#canvas").toggleClass("visible", true);
                    $("#donation-amount").text(donationAmount / 100000000);
                    window.particles.render();
                }
            }

            if (newTransactionSlpRedemptionAmount > 0) {
                const itemId = window.state.currentRedemptionItemId;
                const itemRedemptionAmount = window.state.tokenExchangeRate.redemptionItems[itemId];
                const redemptionAddress = window.state.redemptionAddresses[itemId];

                const previousTokenBalance = ((redemptionAddress ? redemptionAddress.tokenBalance : 0) || 0);
                const newTokenBalance = (previousTokenBalance + newTransactionSlpRedemptionAmount);

                if (newTokenBalance >= itemRedemptionAmount) {
                    window.showDialog(
                        window.state.tokenName + " Redeemed",
                        "Thank you for redeeming your " + window.state.tokenName + "!<br />We'll process your request and contact you.<br /><br />Thank you for supporting us.",
                        null,
                        true
                    );
                }
                else {
                    const remainingTokensRequired = (itemRedemptionAmount - newTokenBalance);
                    window.showDialog(
                        "Almost There!",
                        ("We received your " + newTransactionSlpRedemptionAmount + " " + window.state.tokenName + ".<br />Your current balance is " + newTokenBalance + ".  Send " + remainingTokensRequired + " more and you'll be all set!"),
                        null,
                        false
                    );
                }

                // Update the tokenBalance state for the item...
                if (redemptionAddress) {
                    redemptionAddress.tokenBalance += newTransactionSlpRedemptionAmount;
                }

                window.renderTokenBalance(itemId);
            }
        }
    };

    webSocket.onclose = function() {
        console.log("WebSocket closed...");
    };

    const dialog = $("#dialog");
    $(".button", dialog).on("click", function() {
        window.closeDialog();
    });

    window.setInterval(function() {
        window.getTokenExchangeRate(function(tokenExchangeRate) {
            if (! tokenExchangeRate.wasSuccess) {
                console.log("Unable to load exchange range. " + tokenExchangeRate.errorMessage);
                return;
            }

            window.state.initialize(null, null, tokenExchangeRate);
        });
    }, 150000); // The server caches the price for 5 minutes, so update every 2.5 minutes.
}

function bindUi() {
    $(window).on("keyup", function(event) {
        if (event.keyCode === 27) {
            const body = $("body");
            const container = $(".redemption-card.expanded");
            if (container.length == 0) { return; }

            const overlay = $("#redemption-card-overlay");
            const closeButton = $(".close-btn", container);

            // Close Card...
            container.toggleClass("expanded", false);
            body.toggleClass("no-scroll", false);
            overlay.toggleClass("visible", false);
            closeButton.toggleClass("visible", false);
        }
    });

    $(".redemption-card").click(function() {
        const body = $("body");
        const overlay = $("#redemption-card-overlay");
        const container = $(this);
        const closeButton = $(".close-btn", container);

        const itemId = parseInt($(".redemption-widget", container).data("item-id")); // May be null if there is no form...

        if (! container.hasClass("expanded")) {
            // Open Card...
            container.toggleClass("expanded", true);
            closeButton.toggleClass("visible", true);
            overlay.toggleClass("visible", true);
            body.toggleClass("no-scroll", true);

            const redemptionUiWidget = window.inflateRedemptionContainer(false);
            const renderContainer = $(".redemption-widget", container);
            const redemptionWidget = $(".redemption-widget", container);

            renderContainer.empty();
            renderContainer.append(redemptionUiWidget);

            window.renderTokenBalance(itemId);

            $("form", container).show();
            redemptionWidget.toggleClass("empty", true);

            $("form .button", container).on("click", function() {
                let formIsComplete = true;
                $("form .input.required input", container).each(function() {
                    if ($(this).val().length == 0) {
                        $(this).toggleClass("error", true);
                        formIsComplete = false;
                    }
                });
                if (! formIsComplete) {
                    const errorContainer = $(".error-message", container);
                    errorContainer.text("Please fill out the required fields.");
                    errorContainer.toggleClass("visible", true);
                    return;
                }

                const formData = {};
                $("form .input.required input", container).each(function() {
                    $(this).toggleClass("error", false);
                    $(this).prop("readonly", true);

                    const key = $(this).prop("name");
                    const value = $(this).val();
                    formData[key] = value;
                });

                const errorContainer = $(".error-message", container);
                errorContainer.toggleClass("visible", false);

                window.createNewRedemptionAddress(itemId, formData, function(resultJson) {
                    if (! resultJson.wasSuccess) {
                        errorContainer.text("Server Error: " + (resultJson.errorMessage || ""));
                        errorContainer.toggleClass("visible", true);
                        return;
                    }

                    $("form", container).hide();
                    redemptionWidget.toggleClass("empty", false);

                    const addressJson = resultJson.address;
                    addressJson.tokenBalance = (resultJson.tokenBalance || 0);
                    // window.state.redemptionAddresses.push(addressJson);
                    window.state.redemptionAddresses[itemId] = addressJson;

                    const redemptionUiWidget = window.inflateRedemptionContainer(addressJson);
                    const renderContainer = $(".redemption-widget", container);
                    renderContainer.empty();
                    renderContainer.append(redemptionUiWidget);

                    window.renderTokenBalance(itemId);

                    window.state.currentRedemptionItemId = itemId;
                });
            });
        }
    });

    $(".redemption-card .close-btn").click(function() {
        // Close Card...
        const body = $("body");
        const overlay = $("#redemption-card-overlay");
        const container = $(this).closest(".redemption-card");
        const closeButton = $(".close-btn", container);

        closeButton.toggleClass("visible", false);
        container.toggleClass("expanded", false);
        body.toggleClass("no-scroll", false);
        overlay.toggleClass("visible", false);
        return false;
    });
    $("form").each(function(i, item) {
        $(item).attr("action", "javascript:void(0);");
    });
    $("form .input input").on("focus", function() {
        const container = $(this).parent();
        const label = $("label", container);
        const input = $("input", container);

        if (input.prop("readonly")) {
            input.blur();
            return;
        }

        container.toggleClass("focused", true);
    });
    $("form .input input").on("keyup", function() {
        window.renderInput(this);
    });
    $("form .input input").on("blur", function() {
        const container = $(this).parent();
        const label = $("label", container);
        const input = $("input", container);

        container.toggleClass("focused", false);
    });

    $("#donation-container input").on("keyup", function() {
        const input = $(this);
        const container = $("#donation-container");
        const qrCodeContainer = $(".qr-code", container);
        const qrCode = $("canvas", qrCodeContainer);
        const addressLink = $("a", qrCodeContainer);

        const inputString = input.val();
        const previousValue = input.data("previous-value");

        input.data("previous-value", inputString);

        const hasChanged = (inputString != previousValue);
        if (! hasChanged) { return; }

        const refreshDonationAddress = function(returnAddress) {
            const hasSameReturnAddress = (window.state.returnAddress == returnAddress);
            if (! hasSameReturnAddress) {
                window.createNewDonationAddress(returnAddress, function(newDonationAddressResponse) {
                    if (newDonationAddressResponse.wasSuccess) {
                        const addressJson = newDonationAddressResponse.address;
                        window.state.initialize(addressJson, returnAddress, null);
                    }

                    localStorage.setItem("donationAddress", JSON.stringify(newDonationAddressResponse));
                    window.updateDonationAddressUi(newDonationAddressResponse);

                    addressLink.toggleClass("disabled", false);
                    input.toggleClass("error", false);
                });
            }
            else {
                qrCode.toggle(true);
                addressLink.toggleClass("disabled", false);
                input.toggleClass("error", false);
                $("div", qrCodeContainer).remove();
            }
        };

        if (inputString.length > 0) {
            qrCode.toggle(false);
            addressLink.toggleClass("disabled", true);
            input.toggleClass("error", true);
            if ($("div", qrCodeContainer).length == 0) {
                qrCodeContainer.append("<div class=\"placeholder-qr\"></div>");
            }

            validateSlpAddress(inputString, function(validationResponse) {
                const addressJson = validationResponse.address;
                if (validationResponse.wasSuccess && validationResponse.isSlpAddress) {
                    const returnAddress = addressJson.slpBase32CheckEncoded;
                    refreshDonationAddress(returnAddress);
                }
                else if (validationResponse.wasSuccess && addressJson.base58CheckEncoded) {
                    window.showDialog(
                        "Invalid Return Address",
                        "The address you provided was a BCH address, not an SLP address.<br />SLP addresses usually start with the \"simpleledger:\" prefix.",
                        null,
                        false
                    );
                }
            });
        }
        else {
            qrCode.toggle(true);
            addressLink.toggleClass("disabled", false);
            input.toggleClass("error", false);
            $("div", qrCodeContainer).remove();

            if (window.state.returnAddress) {
                const returnAddress = null;
                refreshDonationAddress(returnAddress);
            }
        }
    });
}

$(window).on("load", function() {
    window.getRedemptionItems(function(response) {
        const redemptionItems = response.redemptionItemsByCategory;
        const container = window.inflateRedemptionItems(redemptionItems);

        const redeemContainer = $("section.redeem");
        redeemContainer.append(container);

        window.bindUi();
        window.init();
    });
});

