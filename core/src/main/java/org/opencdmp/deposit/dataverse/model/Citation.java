package org.opencdmp.deposit.dataverse.model;

import java.util.List;

public class Citation {
    private String displayName = "Citation Metadata";
    private List<CitationField> fields;

    public Citation() {
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public List<CitationField> getFields() {
        return this.fields;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setFields(List<CitationField> fields) {
        this.fields = fields;
    }

    public String toString() {
        return "Citation(displayName=" + this.getDisplayName() + ", fields=" + this.getFields() + ")";
    }
}