package io.github.eealba.payper.orders.v2.model;


import io.github.eealba.jasoner.JasonerSingleVO;

/**
 * 
 */
@JasonerSingleVO
public class AuthenticationFlow {
    private Object value;
    public AuthenticationFlow(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Field value can`t be null");
        }
        this.value = value;
    }

    public Object value() {
        return value;
    }

    public void value(Object value) {
        this.value = value;
    }
}
