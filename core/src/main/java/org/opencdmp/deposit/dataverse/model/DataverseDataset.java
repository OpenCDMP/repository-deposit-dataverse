package org.opencdmp.deposit.dataverse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataverseDataset {

    private String metadataLanguage;

    private String datasetType;

    private DatasetVersion datasetVersion;

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getMetadataLanguage() {
        return metadataLanguage;
    }

    public void setMetadataLanguage(String metadataLanguage) {
        this.metadataLanguage = metadataLanguage;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }
}
