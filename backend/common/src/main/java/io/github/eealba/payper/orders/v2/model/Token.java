package io.github.eealba.payper.orders.v2.model;



/**
 * The tokenized payment source to fund a payment.
 */
public class Token{
    private String id;
    private String type;
    public Token(String id, String type) {
        if (id == null) {
            throw new IllegalArgumentException("Field id can`t be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Field type can`t be null");
        }
        if (!id.matches("^[0-9a-zA-Z_-]+$")) {
            throw new IllegalArgumentException("The value: " + id + " does not match the required pattern");
        }
        if (!type.matches("^[0-9A-Z_-]+$")) {
            throw new IllegalArgumentException("The value: " + type + " does not match the required pattern");
        }
        this.id = id;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public void id(String id) {
        this.id = id;
    }

    public String type() {
        return type;
    }

    public void type(String type) {
        this.type = type;
    }
}
