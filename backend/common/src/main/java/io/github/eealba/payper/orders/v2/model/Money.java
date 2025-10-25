package io.github.eealba.payper.orders.v2.model;


import io.github.eealba.jasoner.JasonerProperty;

/**
 * The currency and amount for a financial transaction, such as a balance or payment due.
 */
public class Money {
    @JasonerProperty("currency_code")
    private CurrencyCode currencyCode;
    private String value;

    public Money(CurrencyCode currencyCode, String value) {
        if (currencyCode == null) {
            throw new IllegalArgumentException("Field currencyCode can`t be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Field value can`t be null");
        }
        if (!value.matches("^((-?[0-9]+)|(-?([0-9]+)?[.][0-9]+))$")) {
            throw new IllegalArgumentException("The value: " + value + " does not match the required pattern");
        }
        this.currencyCode = currencyCode;
        this.value = value;
    }

    public CurrencyCode currencyCode() {
        return currencyCode;
    }

    public void currencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String value() {
        return value;
    }

    public void value(String value) {
        this.value = value;
    }
}
