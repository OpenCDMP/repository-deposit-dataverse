package org.opencdmp.deposit.dataverse.service.dataverse;

import gr.cite.tools.cache.CacheService;
import org.opencdmp.deposit.dataverse.model.DataverseFieldSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DataverseSchemaCacheService extends CacheService<DataverseSchemaCacheService.DataverseSchemaCacheValue> {

    public static class DataverseSchemaCacheValue {

        public DataverseSchemaCacheValue() {
        }

        public DataverseSchemaCacheValue(String repositoryUrl, Map<String, DataverseFieldSchema> schema) {
            this.repositoryUrl = repositoryUrl;
            this.schema = schema;
        }

        private String repositoryUrl;
        private Map<String, DataverseFieldSchema> schema;

        public String getRepositoryUrl() {
            return repositoryUrl;
        }

        public void setRepositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
        }

        public Map<String, DataverseFieldSchema> getSchema() {
            return schema;
        }

        public void setSchema(Map<String, DataverseFieldSchema> schema) {
            this.schema = schema;
        }
    }

    @Autowired
    public DataverseSchemaCacheService(DataverseSchemaCacheOptions options) {
        super(options);
    }

    @Override
    protected Class<DataverseSchemaCacheValue> valueClass() {
        return DataverseSchemaCacheValue.class;
    }

    @Override
    public String keyOf(DataverseSchemaCacheValue value) {
        return this.buildKey(value.getRepositoryUrl());
    }

    public String buildKey(String repositoryUrl) {
        HashMap<String, String> keyParts = new HashMap<>();
        keyParts.put("$repositoryUrl$", repositoryUrl);
        return this.generateKey(keyParts);
    }
}
