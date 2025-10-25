package io.github.eealba.payper.orders.v2.model;


import io.github.eealba.jasoner.JasonerSingleVO;

/**
 * The account identifier for a PayPal account.
 */
@JasonerSingleVO
public class AccountId {
    private String value;
    public AccountId(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Field value can`t be null");
        }
        if (!value.matches("^[2-9A-HJ-NP-Z]{13}$")) {
            throw new IllegalArgumentException("The value: " + value + " does not match the required pattern");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    public void value(String value) {
        this.value = value;
    }
}
