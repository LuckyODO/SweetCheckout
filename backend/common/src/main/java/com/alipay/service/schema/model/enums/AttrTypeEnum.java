package com.alipay.service.schema.model.enums;

import com.alipay.service.schema.model.attribute.*;

import java.util.function.Supplier;

public enum AttrTypeEnum {

    SINGLE("single", SingleAttribute::new),
    MULTI("multi", MultiAttribute::new),
    COMPLEX("complex", ComplexAttribute::new),
    MULTICOMPLEX("multiComplex", MultiComplexAttribute::new);

    final private String type;
    final private Supplier<Attribute> constructor;

    AttrTypeEnum(final String type, Supplier<Attribute> constructor) {
        this.type = type;
        this.constructor = constructor;
    }

    public static Attribute createAttribute(AttrTypeEnum attributeType) {
        Attribute attribute = attributeType.constructor.get();
        attribute.setType(attributeType);
        return attribute;
    }

    public static AttrTypeEnum getType(String type) {
        AttrTypeEnum[] values = AttrTypeEnum.values();
        for (AttrTypeEnum value : values) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

    public String toString() {
        return type;
    }

    /**
     * Getter method for property <tt>type</tt>.
     *
     * @return property value of type
     */
    public String getType() {
        return type;
    }

}
