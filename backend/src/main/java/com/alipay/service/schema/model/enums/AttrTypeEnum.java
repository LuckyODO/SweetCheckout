package com.alipay.service.schema.model.enums;

import com.alipay.service.schema.model.attribute.Attribute;
import com.alipay.service.schema.model.attribute.ComplexAttribute;
import com.alipay.service.schema.model.attribute.MultiAttribute;
import com.alipay.service.schema.model.attribute.MultiComplexAttribute;
import com.alipay.service.schema.model.attribute.SingleAttribute;

public enum AttrTypeEnum {

    SINGLE("single"),
    MULTI("multi"),
    COMPLEX("complex"),
    MULTICOMPLEX("multiComplex");

    final private String type;

    AttrTypeEnum(final String type) {
        this.type = type;
    }

    public static Attribute createAttribute(AttrTypeEnum attributeType) {
        Attribute attribute = switch (attributeType) {
            case SINGLE -> new SingleAttribute();
            case MULTI -> new MultiAttribute();
            case COMPLEX -> new ComplexAttribute();
            case MULTICOMPLEX -> new MultiComplexAttribute();
        };
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
