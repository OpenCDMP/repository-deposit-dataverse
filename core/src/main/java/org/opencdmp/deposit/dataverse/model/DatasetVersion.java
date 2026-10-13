package org.opencdmp.deposit.dataverse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasetVersion {

    private DataSetMetadataBlock metadataBlocks;

    private String termsOfUse;

    /** The licence as Dataverse 5.10+ takes it: {name, uri}. */
    private Map<String, String> license;

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

    public Map<String, String> getLicense() {
        return license;
    }

    public void setLicense(Map<String, String> license) {
        this.license = license;
    }
}
