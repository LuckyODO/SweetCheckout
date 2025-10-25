package io.github.eealba.payper.orders.v2.model;


import io.github.eealba.jasoner.JasonerSingleVO;

/**
 * The full name representation like Mr J Smith.
 */
@JasonerSingleVO
public class FullName {
    private String value;
    public FullName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Field value can`t be null");
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
