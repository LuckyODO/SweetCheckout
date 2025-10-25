package io.github.eealba.payper.orders.v2.model;


import io.github.eealba.jasoner.JasonerSingleVO;

/**
 * The identifier of the instrument.
 */
@JasonerSingleVO
public class InstrumentId {
    private String value;
    public InstrumentId(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Field value can`t be null");
        }
        if (!value.matches("^[A-Za-z0-9-_.+=]+$")) {
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
