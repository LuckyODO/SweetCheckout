package io.github.eealba.payper.orders.v2.model;

import io.github.eealba.jasoner.JasonerSingleVO;

import java.util.List;

/**
 * An array of JSON patch objects to apply partial updates to resources.
 */
@JasonerSingleVO
public class PatchRequest {
    private List<Patch> value;

    public PatchRequest(List<Patch> value) {
        if (value == null) {
            throw new IllegalArgumentException("Field value can`t be null");
        }
        this.value = value;
    }

    public List<Patch> value() {
        return value;
    }

    public void value(List<Patch> value) {
        this.value = value;
    }
}
