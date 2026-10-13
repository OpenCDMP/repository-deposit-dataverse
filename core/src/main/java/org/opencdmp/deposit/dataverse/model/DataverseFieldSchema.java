package org.opencdmp.deposit.dataverse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataverseFieldSchema {
    private final String typeClass;
    private final boolean multiple;
    private final boolean required;
    private Map<String, Map<String,Object>> childFields;
    private List<String> controlledVocabularyValues;

    public DataverseFieldSchema(String typeClass, boolean multiple, boolean required, Map<String, Map<String,Object>> childFields) {
        this.typeClass = typeClass;
        this.multiple = multiple;
        this.required = required;
        this.childFields = childFields;
    }
    public DataverseFieldSchema(String typeClass, boolean multiple, boolean required, Map<String, Map<String,Object>> childFields , List<String> controlledVocabularyValues ) {
        this(typeClass, multiple, required, childFields);
        this.controlledVocabularyValues = controlledVocabularyValues;
    }

    public String getTypeClass() {
        return typeClass;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isRequired() {
        return required;
    }

    public Map<String, Map<String,Object>> getChildFields() {
        return childFields;
    }

    public void setChildFields(Map<String, Map<String,Object>> childFields) {
        this.childFields = childFields;
    }

    public List<String> getControlledVocabularyValues() {
        return controlledVocabularyValues;
    }

    public void setControlledVocabularyValues(List<String> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
    }
}
