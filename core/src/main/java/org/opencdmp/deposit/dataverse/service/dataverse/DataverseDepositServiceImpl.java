package org.opencdmp.deposit.dataverse.service.dataverse;


import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.client.HttpStatusCodeException;
import org.opencdmp.deposit.dataverse.validation.DataverseErrors;
import org.opencdmp.deposit.dataverse.validation.DataverseDatasetValidator;
import gr.cite.tools.exception.MyValidationException;
import gr.cite.tools.exception.MyApplicationException;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.models.FileEnvelopeModel;
import org.opencdmp.commonmodels.models.deposit.DepositPreviewModel;
import org.opencdmp.commonmodels.models.deposit.PreviewFieldOption;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.plugin.PluginUserFieldModel;
import org.opencdmp.deposit.dataverse.configuration.DataverseSemanticsManifest;
import org.opencdmp.deposit.dataverse.model.DataverseDataset;
import org.opencdmp.deposit.dataverse.model.DataverseFieldSchema;
import org.opencdmp.deposit.dataverse.model.builder.DataverseBuilder;
import org.opencdmp.deposit.dataverse.model.builder.DataversePreviewMapper;
import org.opencdmp.deposit.dataverse.service.storage.FileStorageService;
import org.opencdmp.depositbase.repository.DepositAccount;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.DepositPluginSettings;
import org.opencdmp.depositbase.repository.DepositPreviewMergeHelper;
import org.opencdmp.depositbase.repository.PlanDepositModel;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

@Component
public class DataverseDepositServiceImpl implements DataverseDepositService {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(DataverseDepositServiceImpl.class));

    public static final String CONFIGURATION_FIELD_ACCESS_TOKEN = "dataverse-access-token";
    public static final String SETTING_ORGANISATION_ACCESS_TOKEN = "organisation-access-token";
    public static final String SETTING_COLLECTION = "collection";
    public static final String SETTING_SUBJECT = "subject";
    public static final String SETTING_METADATA_LANGUAGE = "metadata-language";
    public static final String SETTING_LICENCE = "licence";
    public static final String SETTING_PUBLISH_MODE = "publish-mode";
    public static final String PUBLISH_MODE_DRAFT = "draft";
    public static final String SETTING_RESTRICT_FILES = "restrict-files";
    public static final String SETTING_CONTACT_EMAIL = "contact-email";
    public static final String SETTING_CONTACT_NAME = "contact-name";
    public static final String SETTING_REFERENCE_TYPE_ORGANISATIONS = "reference-type-organisations";
    public static final String SETTING_REFERENCE_TYPE_FUNDERS = "reference-type-funders";
    public static final String SETTING_REFERENCE_TYPE_GRANTS = "reference-type-grants";
    public static final String SETTING_REFERENCE_TYPE_RESEARCHERS = "reference-type-researchers";
    public static final String SETTING_REFERENCE_TYPE_LICENCES = "reference-type-licences";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DataverseServiceProperties dataverseServiceProperties;
    private final DataverseBuilder dataverseBuilder;
    private final FileStorageService storageService;
    private final ResourceLoader resourceLoader;
    private final DataversePreviewMapper dataversePreviewMapper;
    private final DataverseSchemaCacheService schemaCacheService;
    private final DataverseDatasetValidator datasetValidator;

    private byte[] logo;;

    @Autowired
    public DataverseDepositServiceImpl(DataverseServiceProperties dataverseServiceProperties, DataverseBuilder mapper, FileStorageService storageService, ResourceLoader resourceLoader, DataversePreviewMapper dataversePreviewMapper, DataverseSchemaCacheService schemaCacheService, DataverseDatasetValidator datasetValidator){
        this.dataverseServiceProperties = dataverseServiceProperties;
        this.dataverseBuilder = mapper;
	    this.storageService = storageService;
        this.resourceLoader = resourceLoader;
        this.dataversePreviewMapper = dataversePreviewMapper;
        this.schemaCacheService = schemaCacheService;
        this.datasetValidator = datasetValidator;
        this.dataverseServiceProperties.getDepositConfiguration().setSemantics(DataverseSemanticsManifest.get());
        this.logo = null;
    }

    private String grabAuthenticationToken(PlanDepositModel planDepositModel) {
        DepositConfiguration  depositConfiguration = this.getConfiguration();
        String token = null; // authentication token grab
        if (planDepositModel.getAuthInfo() != null && depositConfiguration != null) {
            if (planDepositModel.getAuthInfo().getAuthToken() != null && !planDepositModel.getAuthInfo().getAuthToken().isBlank()) token = planDepositModel.getAuthInfo().getAuthToken();
            else if (planDepositModel.getAuthInfo().getAuthFields() != null && !planDepositModel.getAuthInfo().getAuthFields().isEmpty() && depositConfiguration.getUserConfigurationFields() != null) {
                PluginUserFieldModel userFieldModel = planDepositModel.getAuthInfo().getAuthFields().stream().filter(x -> x.getCode().equals(CONFIGURATION_FIELD_ACCESS_TOKEN)).findFirst().orElse(null);
                if (userFieldModel != null && userFieldModel.getTextValue() != null && !userFieldModel.getTextValue().isBlank()) token = userFieldModel.getTextValue();
            }
        }
        if (token == null || token.isBlank()) token = DepositPluginSettings.of(planDepositModel, depositConfiguration).text(SETTING_ORGANISATION_ACCESS_TOKEN, this.dataverseServiceProperties.getDepositConfiguration().getAccessToken());
        return token;
    }


    @Override
    public DepositPreviewModel preview(PlanDepositModel planDepositModel) {
        if (planDepositModel == null || planDepositModel.getPlanModel() == null) return null;
        String token = grabAuthenticationToken(planDepositModel);
        DepositPluginSettings settings = DepositPluginSettings.of(planDepositModel, this.getConfiguration());
        Map<String, DataverseFieldSchema> schema = this.fetchCitationSchema(token);
        PlanModel planModel = planDepositModel.getPlanModel();
        // a dataset's type is set only when it is created, so a new version's review leaves it out
        Map<String, String> datasetTypes = planModel.getPreviousDOI() == null ? this.fetchAllowedDatasetTypes(this.dataverseBuilder.buildDataverseIdentifier(planModel, settings), token) : null;
        DataverseDataset dataset = this.dataverseBuilder.build(planModel, schema, settings, this.fetchLicences(planModel, settings, token), datasetTypes != null ? datasetTypes.keySet() : null);
        List<String> uploadFileNames = new ArrayList<>();
        if (planModel.getPdfFile() != null && planModel.getPdfFile().getFilename() != null) uploadFileNames.add(planModel.getPdfFile().getFilename());
        if (planModel.getRdaJsonFile() != null && planModel.getRdaJsonFile().getFilename() != null) uploadFileNames.add(planModel.getRdaJsonFile().getFilename());
        if (planModel.getSupportingFilesZip() != null && planModel.getSupportingFilesZip().getFilename() != null) uploadFileNames.add(planModel.getSupportingFilesZip().getFilename());
        DepositPreviewModel preview = this.dataversePreviewMapper.toPreview(dataset, schema, uploadFileNames, settings, datasetTypes);
        DepositPreviewMergeHelper.merge(preview, planDepositModel.getStoredFields(), planDepositModel.getStoredGroups());
        return preview;
    }

    @Override
    public List<PreviewFieldOption> searchVocabulary(String fieldCode, String like) {
        // no per-request auth is available on this endpoint; the schema is a read-only, mostly-static
        // resource so the system-configured token is sufficient (same fallback grabAuthenticationToken
        // uses when a caller supplies none).
        String token = this.dataverseServiceProperties.getDepositConfiguration().getAccessToken();
        Map<String, DataverseFieldSchema> schema = this.fetchCitationSchema(token);
        return this.dataversePreviewMapper.searchVocabulary(schema, fieldCode, like);
    }

    @Override
    public String deposit(PlanDepositModel planDepositModel) {

        if(this.getConfiguration() != null && planDepositModel != null && planDepositModel.getPlanModel() != null) {

            String token = grabAuthenticationToken(planDepositModel);
            String previousDOI = planDepositModel.getPlanModel().getPreviousDOI();

            try {

                if (previousDOI == null) {
                    return depositFirst(planDepositModel, token);
                } else {
                    return depositNewVersion(planDepositModel, previousDOI, token);
                }

            } catch (WebClientResponseException ex) {
                logger.error(ex.getMessage(), ex);
                throw this.toException(ex.getStatusCode().is4xxClientError(), ex.getResponseBodyAsString(), ex.getStatusText());
            } catch (HttpStatusCodeException ex) {
                logger.error(ex.getMessage(), ex);
                throw this.toException(ex.getStatusCode().is4xxClientError(), ex.getResponseBodyAsString(), ex.getStatusText());
            }

        }

        return null;

    }


    private String depositFirst(PlanDepositModel planDepositModel, String token) {
        DepositPluginSettings settings = DepositPluginSettings.of(planDepositModel, this.getConfiguration());
        Map<String, DataverseFieldSchema> schema = this.fetchCitationSchema(token);
        String collection = this.dataverseBuilder.buildDataverseIdentifier(planDepositModel.getPlanModel(), settings);
        Map<String, String> datasetTypes = this.fetchAllowedDatasetTypes(collection, token);
        DataverseDataset dataset = this.dataverseBuilder.build(planDepositModel.getPlanModel(), schema, settings, this.fetchLicences(planDepositModel.getPlanModel(), settings, token), datasetTypes != null ? datasetTypes.keySet() : null);
        this.dataversePreviewMapper.applyOverrides(dataset,schema,planDepositModel.getEditedFields(), planDepositModel.getEditedGroups(), settings);
        boolean publish = !PUBLISH_MODE_DRAFT.equals(settings.text(SETTING_PUBLISH_MODE));
        this.failOnInvalidValues(dataset, schema, publish, datasetTypes != null ? datasetTypes.keySet() : null);

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "dataverses/" + collection + "/datasets?doNotValidate=true";

        Map<String, Object> response = this.getWebClient().post().uri(url).headers(httpHeaders -> {
                    httpHeaders.set("X-Dataverse-key", token);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(dataset).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();

        response = (Map<String, Object>) response.get("data");
        String doi = String.valueOf(response.get("persistentId"));

        try {
            this.uploadFiles(planDepositModel.getPlanModel(), doi, token, settings);
            if (publish) this.publish(doi, token);
        } catch (RuntimeException ex) {
            // a never-published dataset goes with its draft, so nothing is left behind in the dataverse
            this.deleteDraft(doi, token);
            throw ex;
        }
        return doi;
    }

    private void failOnInvalidValues(DataverseDataset dataset, Map<String, DataverseFieldSchema> schema, boolean checkRequired, Collection<String> allowedDatasetTypes) {
        Map<String, List<String>> problems = this.datasetValidator.validate(dataset, schema, checkRequired, allowedDatasetTypes);
        if (!problems.isEmpty()) throw DataverseErrors.exception(DataverseErrors.INVALID_VALUES, problems);
    }

    private void deleteDraft(String doi, String token) {
        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "datasets/:persistentId/versions/:draft?persistentId=" + doi;
        try {
            this.getWebClient().delete().uri(url).headers(httpHeaders -> httpHeaders.set("X-Dataverse-key", token)).retrieve().toBodilessEntity().block();
        } catch (Exception e) {
            logger.warn(new MapLogEntry("Could not remove the Dataverse draft").And("persistentId", doi).And("error", e.getMessage()));
        }
    }

    /** Dataverse's answer as the user should see it: a validation error when it refused the values, otherwise its message. */
    private RuntimeException toException(boolean clientError, String body, String statusText) {
        String message = null;
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.path("message").isString()) message = json.path("message").asString();
        } catch (Exception ignored) {
            // not a JSON answer
        }
        if (message == null || message.isBlank()) message = statusText;
        String described = DataverseErrors.describe(message);
        if (!clientError) return new MyApplicationException("The repository could not take the deposit: " + described);
        return new MyValidationException(described.startsWith(DataverseErrors.INVALID_VALUES) ? described : "The repository refused the deposit: " + described);
    }

    private void uploadFiles(PlanModel planModel, String doi, String token, DepositPluginSettings settings) {
        boolean restrict = settings.bool(SETTING_RESTRICT_FILES, false);
        if (planModel.getPdfFile() != null) this.uploadFile(planModel.getPdfFile(), doi, token, restrict);
        if (planModel.getRdaJsonFile() != null) this.uploadFile(planModel.getRdaJsonFile(), doi, token, restrict);
        if (planModel.getSupportingFilesZip() != null) this.uploadFile(planModel.getSupportingFilesZip(), doi, token, restrict);
    }

    private void uploadFile(FileEnvelopeModel fileEnvelopeModel, String doi, String token, boolean restrict) {

        if (fileEnvelopeModel == null) return;

        byte[] fileBytes = null;
        if (this.getConfiguration().isUseSharedStorage() && fileEnvelopeModel.getFileRef() != null && !fileEnvelopeModel.getFileRef().isBlank()) {
            fileBytes = this.storageService.readFile(fileEnvelopeModel.getFileRef());
        }
        if (fileBytes == null || fileBytes.length == 0){
            fileBytes = fileEnvelopeModel.getFile();
        }


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Dataverse-key", token);
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename(fileEnvelopeModel.getFilename())
                .build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(fileBytes, fileMap);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);
        body.add("jsonData", "{\"restrict\":\"" + restrict + "\", \"tabIngest\":\"false\"}");
        HttpEntity<MultiValueMap<String, Object>> requestEntity
                = new HttpEntity<>(body, headers);

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "datasets/:persistentId/add?persistentId=" + doi;

        // postForEntity throws HttpClientErrorException/HttpServerErrorException on a non-2xx response,
        // which propagates up to deposit()'s catch block same as the WebClient calls elsewhere in this class.
        new RestTemplate().postForEntity(url, requestEntity, Object.class);
    }

    private void deleteFile(int fileId, String token){
        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "files/" + fileId;

        this.getWebClient().delete().uri(url).headers(httpHeaders -> {
                    httpHeaders.set("X-Dataverse-key", token);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                }).exchangeToMono(mono ->
                mono.statusCode().isError() ?
                        mono.createException().flatMap(Mono::error) :
                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })).block();
    }

    private void publish(String doi, String token) {

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "datasets/:persistentId/actions/:publish?persistentId=" + doi + "&type=major";

        Map<String, Object> publishResponse = this.getWebClient().post().uri(url).headers(httpHeaders -> {
            httpHeaders.set("X-Dataverse-key", token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        }).exchangeToMono(mono ->
                mono.statusCode().isError() ?
                        mono.createException().flatMap(Mono::error) :
                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })).block();

        if (publishResponse == null) throw new UnsupportedOperationException("Failed to publish to Dataverse");
    }

    private String depositNewVersion(PlanDepositModel planDepositModel, String previousDOI, String token) {
        Map<String, DataverseFieldSchema> schema = this.fetchCitationSchema(token);

        DepositPluginSettings settings = DepositPluginSettings.of(planDepositModel, this.getConfiguration());
        // only the version is sent, so the dataset keeps the type it was created with
        DataverseDataset dataset = this.dataverseBuilder.build(planDepositModel.getPlanModel(), schema, settings, this.fetchLicences(planDepositModel.getPlanModel(), settings, token), null);
        this.dataversePreviewMapper.applyOverrides(dataset,schema, planDepositModel.getEditedFields(), planDepositModel.getEditedGroups(), settings);
        this.failOnInvalidValues(dataset, schema, true, null);

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "datasets/:persistentId/versions/:draft?persistentId=" + previousDOI;

        Map<String, Object> response = this.getWebClient().put().uri(url).headers(httpHeaders -> {
                    httpHeaders.set("X-Dataverse-key", token);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(dataset.getDatasetVersion()).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();

        if (response == null) throw new MyApplicationException("cannot create new draft version");

        try {
            JsonNode jsonNode = objectMapper.convertValue(response, JsonNode.class);
            JsonNode files = jsonNode.path("data").path("files");

            if (files.isArray()) {
                for (JsonNode file : files) {
                    int fileId = file.path("dataFile").path("id").asInt();
                    this.deleteFile(fileId, token);
                }
            }

            this.uploadFiles(planDepositModel.getPlanModel(), previousDOI, token, settings);
            if (!PUBLISH_MODE_DRAFT.equals(settings.text(SETTING_PUBLISH_MODE))) this.publish(previousDOI, token);
        } catch (RuntimeException ex) {
            // removing the new draft version leaves the dataset as it was published
            this.deleteDraft(previousDOI, token);
            throw ex;
        }
        return previousDOI;
    }


    /** The installation's active licences, read only when the plan has a licence reference to match against them. */
    private List<Map<String, Object>> fetchLicences(PlanModel planModel, DepositPluginSettings settings, String token) {
        if (this.dataverseBuilder.licenceReferences(planModel, settings).isEmpty()) return List.of();
        try {
            Map<String, Object> response = this.getWebClient().get().uri(this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "licenses")
                    .headers(h -> h.set("X-Dataverse-key", token))
                    .exchangeToMono(mono -> mono.statusCode().isError()
                            ? mono.createException().flatMap(Mono::error)
                            : mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}))
                    .block();
            if (response == null || !(response.get("data") instanceof List<?> licences)) return List.of();
            return licences.stream().filter(x -> x instanceof Map<?, ?>).map(x -> (Map<String, Object>) x).filter(x -> !Boolean.FALSE.equals(x.get("active"))).toList();
        } catch (Exception e) {
            // installations before Dataverse 5.10 have no licences endpoint; the plan's licence is then not sent
            logger.warn("Could not read the Dataverse licences: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * The dataset types the collection allows, as name to display name: its allowedDatasetTypes, or only "dataset"
     * when it lists none, which is what Dataverse checks a new dataset against. Null when the collection could not be
     * read, so the plan's type is sent unchecked.
     */
    private Map<String, String> fetchAllowedDatasetTypes(String collection, String token) {
        if (collection == null || collection.isBlank()) return null;
        try {
            Map<String, Object> response = this.getWebClient().get().uri(this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "dataverses/" + collection)
                    .headers(h -> h.set("X-Dataverse-key", token))
                    .exchangeToMono(mono -> mono.statusCode().isError()
                            ? mono.createException().flatMap(Mono::error)
                            : mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}))
                    .block();
            if (response == null || !(response.get("data") instanceof Map<?, ?> data)) return null;
            Map<String, String> types = new LinkedHashMap<>();
            if (data.get("allowedDatasetTypes") instanceof List<?> allowed) {
                for (Object item : allowed) {
                    if (!(item instanceof Map<?, ?> type) || !(type.get("name") instanceof String name) || name.isBlank()) continue;
                    types.put(name, type.get("displayName") instanceof String displayName && !displayName.isBlank() ? displayName : name);
                }
            }
            if (types.isEmpty()) types.put(DataverseBuilder.DEFAULT_DATASET_TYPE, DataverseBuilder.DEFAULT_DATASET_TYPE);
            return types;
        } catch (Exception e) {
            logger.warn("Could not read the dataset types of the Dataverse collection " + collection + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public DepositConfiguration getConfiguration() {
        return this.dataverseServiceProperties.getDepositConfiguration();
    }
    
    @Override
    public DepositAccount account(PlanDepositModel planDepositModel) {
        DepositConfiguration depositConfiguration = this.getConfiguration();
        if (depositConfiguration == null || planDepositModel == null) return null;

        String token = this.grabAuthenticationToken(planDepositModel);
        if (token == null || token.isBlank()) return null;

        try {
            // the token goes in the header, not the URL, because the request logger prints URLs
            Map<String, Object> response = this.getWebClient().get().uri(depositConfiguration.getRepositoryUrl() + "users/:me").headers(httpHeaders -> {
                httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                httpHeaders.set("X-Dataverse-key", token);
            }).exchangeToMono(mono -> mono.statusCode().isError() ? mono.createException().flatMap(Mono::error) : mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();
            if (response == null || !(response.get("data") instanceof Map<?, ?> me)) return null;

            Object id = me.get("id") != null ? me.get("id") : me.get("identifier");
            if (id == null) return null;

            String displayName = me.get("displayName") instanceof String value && !value.isBlank() ? value : null;
            if (displayName == null) {
                String fullName = ((me.get("firstName") instanceof String first ? first : "") + " " + (me.get("lastName") instanceof String last ? last : "")).trim();
                if (!fullName.isBlank()) displayName = fullName;
            }
            String username = me.get("identifier") instanceof String value && !value.isBlank() ? value : null;
            String email = me.get("email") instanceof String value && !value.isBlank() ? value : null;
            String name = displayName != null ? displayName : username != null ? username : email;
            if (name != null && email != null && !name.equals(email)) name = name + " (" + email + ")";

            DepositAccount account = new DepositAccount();
            account.setId(id.toString());
            account.setName(name);
            return account;
        } catch (Exception ex) {
            logger.error("could not read the Dataverse account of the deposit", ex);
            return null;
        }
    }

    @Override
    public String authenticate(String code){
        return null;
    }

    @Override
    public String getLogo() {
        DepositConfiguration dataverseConfig = this.dataverseServiceProperties.getDepositConfiguration();
        if(dataverseConfig != null && dataverseConfig.isHasLogo() && this.dataverseServiceProperties.getLogo() != null && !this.dataverseServiceProperties.getLogo().isBlank()) {
            if (this.logo == null) {
                try {
                    Resource resource = resourceLoader.getResource(this.dataverseServiceProperties.getLogo());
                    if(!resource.isReadable()) return null;
                    try(InputStream inputStream = resource.getInputStream()) {
                        this.logo = inputStream.readAllBytes();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            return (this.logo != null && this.logo.length != 0) ? Base64.getEncoder().encodeToString(this.logo) : null;
        }
        return null;
    }
    
    /**
     * The citation metadata block schema is effectively static per Dataverse instance, so it is cached
     * (see cache.yml, "dataverseCitationSchema") instead of being fetched over the network on every single
     * preview/deposit call.
     */
    private Map<String, DataverseFieldSchema> fetchCitationSchema(String token) {
        String repositoryUrl = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl();
        String cacheKey = this.schemaCacheService.buildKey(repositoryUrl);
        DataverseSchemaCacheService.DataverseSchemaCacheValue cached = this.schemaCacheService.lookup(cacheKey);
        if (cached != null && cached.getSchema() != null) return cached.getSchema();

        Map<String, DataverseFieldSchema> schema = this.fetchCitationSchemaUncached(token, repositoryUrl);
        this.schemaCacheService.put(new DataverseSchemaCacheService.DataverseSchemaCacheValue(repositoryUrl, schema));
        return schema;
    }

    private Map<String, DataverseFieldSchema> fetchCitationSchemaUncached(String token, String repositoryUrl) {
        String url = repositoryUrl + "metadatablocks/citation";
        Map<String, Object> response = this.getWebClient().get().uri(url)
                .headers(h -> h.set("X-Dataverse-key", token))
                .exchangeToMono(mono -> mono.statusCode().isError()
                        ? mono.createException().flatMap(Mono::error)
                        : mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}))
                .block();

        Map<String, DataverseFieldSchema> schema = new HashMap<>();
        if (response == null) return schema;

        JsonNode root = objectMapper.convertValue(response, JsonNode.class);
        JsonNode fields = root.path("data").path("fields");
        if (fields.isMissingNode()) return schema;

        fields.properties().forEach(entry -> {
            JsonNode field = entry.getValue();
            String typeClass = field.path("typeClass").asText("primitive");
            boolean multiple = field.path("multiple").asBoolean(false);
            boolean required = field.path("isRequired").asBoolean(false);
            Map<String, Map<String,Object>> childFields = objectMapper.convertValue(
                    field.path("childFields"),
                    new TypeReference<Map<String,Map<String,Object>>>() {}
            );
            // controlledVocabularyValues sits one level up from "childFields", directly on the field node
            List<String> controlledVocabularyValues  = objectMapper.convertValue(field.path("controlledVocabularyValues"),new TypeReference<List<String>>() {} );
            DataverseFieldSchema fieldSchema = new DataverseFieldSchema(typeClass, multiple, required, childFields, controlledVocabularyValues);
            fieldSchema.setType(field.path("type").asText(null));
            fieldSchema.setDisplayName(field.path("displayName").asText(null));
            schema.put(entry.getKey(), fieldSchema);
        });

        return schema;
    }

    private WebClient getWebClient(){
        return WebClient.builder().filters(exchangeFilterFunctions -> {
            exchangeFilterFunctions.add(logRequest());
            exchangeFilterFunctions.add(logResponse());
        }).codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(this.dataverseServiceProperties.getMaxInMemorySizeInBytes())
        ).build();
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug(new MapLogEntry("Request").And("method", clientRequest.method().toString()).And("url", clientRequest.url().toString()));
            return Mono.just(clientRequest);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                // reading the body consumes it, so the response passed on gets it back for the caller to read Dataverse's message
                return response.bodyToMono(String.class).defaultIfEmpty("")
                    .map(body -> {
                        logger.error(new MapLogEntry("Response").And("method", response.request().getMethod().toString()).And("url", response.request().getURI()).And("status", response.statusCode().toString()).And("body", body));
                        return response.mutate().body(body).build();
                    });
            }
            return Mono.just(response);
            
        });
    }
}
