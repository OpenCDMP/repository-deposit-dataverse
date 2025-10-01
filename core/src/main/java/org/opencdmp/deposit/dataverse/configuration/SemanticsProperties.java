package org.opencdmp.deposit.dataverse.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "semantics")
public class SemanticsProperties {
    private List<String> relationType;

    private List<String> identifierType;

    private List<Contributor> contributorType;

    public List<String> getRelationType() {
        return relationType;
    }

    public void setRelationType(List<String> relationType) {
        this.relationType = relationType;
    }

    public List<String> getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(List<String> identifierType) {
        this.identifierType = identifierType;
    }

    public List<Contributor> getContributorType() {
        return contributorType;
    }

    public void setContributorType(List<Contributor> contributorType) {
        this.contributorType = contributorType;
    }

    public static class Contributor {
        private String code;
        private String value;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
