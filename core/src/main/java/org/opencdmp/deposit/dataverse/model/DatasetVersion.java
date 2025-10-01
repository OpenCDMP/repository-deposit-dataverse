package org.opencdmp.deposit.dataverse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasetVersion {

    private DataSetMetadataBlock metadataBlocks;

    private String termsOfUse;

    public DataSetMetadataBlock getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(DataSetMetadataBlock metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }
}
