package io.github.eealba.payper.orders.v2.model;


import io.github.eealba.jasoner.JasonerSingleVO;

/**
 * Describes the URL.
 */
@JasonerSingleVO
public class Url {
    private String value;
    public Url(String value) {
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
