package org.opencdmp.deposit.dataverse.model;

public class CitationField {
    private String typeName;
    private String typeClass;
    private boolean multiple;
    private Object value;

    public String getTypeName() {
        return this.typeName;
    }

    public String getTypeClass() {
        return this.typeClass;
    }

    public boolean isMultiple() {
        return this.multiple;
    }

    public Object getValue() {
        return this.value;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setTypeClass(String typeClass) {
        this.typeClass = typeClass;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public void setValue(Object value) {
        this.value = value;
    }


    public String toString() {
        return "CitationField(typeName=" + this.getTypeName() + ", typeClass=" + this.getTypeClass() + ", multiple=" + this.isMultiple() + ", value=" + this.getValue() + ")";
    }

    public CitationField(String typeName, String typeClass, boolean multiple, Object value) {
        this.typeName = typeName;
        this.typeClass = typeClass;
        this.multiple = multiple;
        this.value = value;
    }

    public CitationField() {
    }
}
