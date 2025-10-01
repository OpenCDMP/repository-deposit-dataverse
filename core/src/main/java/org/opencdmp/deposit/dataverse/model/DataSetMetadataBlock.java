package org.opencdmp.deposit.dataverse.model;

public class DataSetMetadataBlock {
    private Citation citation;

    public DataSetMetadataBlock() {
    }

    public Citation getCitation() {
        return this.citation;
    }

    public void setCitation(Citation citation) {
        this.citation = citation;
    }

    public String toString() {
        return "DataSetMetadataBlock(citation=" + this.getCitation() + ")";
    }
}
