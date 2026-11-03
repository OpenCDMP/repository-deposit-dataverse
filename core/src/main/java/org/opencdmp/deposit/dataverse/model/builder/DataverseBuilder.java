package org.opencdmp.deposit.dataverse.model.builder;

import gr.cite.tools.logging.LoggerService;
import jakarta.annotation.Nullable;
import org.opencdmp.commonmodels.enums.PlanAccessType;
import org.opencdmp.commonmodels.enums.PlanUserRole;
import org.opencdmp.commonmodels.models.PlanUserModel;
import org.opencdmp.commonmodels.models.description.*;
import org.opencdmp.commonmodels.models.descriptiotemplate.DefinitionModel;
import org.opencdmp.commonmodels.models.plan.PlanPropertiesModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldSetModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.RadioBoxDataModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.SelectDataModel;
import org.opencdmp.commonmodels.models.plan.PlanContactModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldSetModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.RadioBoxDataModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.ReferenceTypeDataModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.SelectDataModel;
import org.opencdmp.commonmodels.models.plandescriptionfields.FieldModel;
import org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel;
import org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetModel;
import org.opencdmp.commonmodels.models.planreference.PlanReferenceModel;
import org.opencdmp.commonmodels.models.reference.ReferenceFieldModel;
import org.opencdmp.commonmodels.models.reference.ReferenceModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeDefinitionModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeFieldModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeModel;
import org.opencdmp.deposit.dataverse.model.*;
import org.opencdmp.deposit.dataverse.service.dataverse.DataverseDepositServiceImpl;
import org.opencdmp.deposit.dataverse.service.dataverse.DataverseServiceProperties;
import org.opencdmp.deposit.dataverse.service.descriptiontemplatesearcher.TemplateFieldSearcherService;
import org.opencdmp.deposit.dataverse.service.planblueprintsearcher.PlanBlueprintFieldSearcherService;
import org.opencdmp.depositbase.repository.DepositPluginSettings;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DataverseBuilder {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(DataverseDepositServiceImpl.class));
    private final PlanBlueprintFieldSearcherService blueprintFieldSearcherService;
    private final TemplateFieldSearcherService templateFieldSearcherService;

    private static final String TERMS_OF_USE = "CC0 - Public Domain Dedication";
    private static final String CONTRIBUTOR_TYPE_RESEARCHER = "Researcher";
    private static final String CONTRIBUTOR_TYPE_FUNDER = "Funder";
    private static final String CONTRIBUTOR_TYPE_EDITOR = "Editor";
    private static final String CONTRIBUTOR_TYPE_MEMBER = "Project Member";
    private static final String FIELD_TYPE_CLASS_PRIMITIVE = "primitive";
    private static final String FIELD_TYPE_CLASS_COMPOUND = "compound";
    private static final String FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY = "controlledVocabulary";
    private static final String FIELD_CONTRIBUTOR_TYPE = "contributorType";


    private static final String SEMANTIC_DATAVERSE_IDENTIFIER = "dataverse.identifier";
    private static final String SEMANTIC_DATAVERSE_SUBJECT = "dataverse.dataset.subject";
    private static final String SEMANTIC_DATAVERSE_DESCRIPTION = "dataverse.dataset.description";
    private static final String SEMANTIC_DATAVERSE_ALTERNATIVE_TITLE = "dataverse.dataset.alternative_title";
    private static final String SEMANTIC_DATAVERSE_SUBTITLE = "dataverse.dataset.subtitle";
    private static final String SEMANTIC_DATAVERSE_ALTERNATIVE_URL = "dataverse.dataset.alternative_url";
    private static final String SEMANTIC_DATAVERSE_DATE_OF_DEPOSIT = "dataverse.dataset.date_of_deposit";
    private static final String SEMANTIC_DATAVERSE_NOTES = "dataverse.dataset.notes";
    private static final String SEMANTIC_DATAVERSE_DATASET_TYPE = "dataverse.dataset.type";

    /** The type Dataverse creates a dataset with when none is sent, and the only one a collection allows unless it lists others. */
    public static final String DEFAULT_DATASET_TYPE = "dataset";

    private static final String SEMANTIC_DATAVERSE_AUTHOR_NAME = "dataverse.author.name";
    private static final String SEMANTIC_DATAVERSE_AUTHOR_AFFILIATION = "dataverse.author.affiliation";

    private static final String SEMANTIC_DATAVERSE_CONTACT_NAME = "dataverse.point_of_contact.name";
    private static final String SEMANTIC_DATAVERSE_CONTACT_EMAIL = "dataverse.point_of_contact.email";
    private static final String SEMANTIC_DATAVERSE_CONTACT_AFFILIATION = "dataverse.point_of_contact.affiliation";

    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE = "dataverse.related_publication.identifier_type";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER = "dataverse.related_publication.identifier";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE = "dataverse.related_publication.relation_type";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION = "dataverse.related_publication.citation";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL = "dataverse.related_publication.url";
    private static final List<String> SEMANTIC_DATAVERSE_RELATED_PUBLICATION_KEYS = List.of(SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE,
            SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL);

    private static final String SEMANTIC_DATAVERSE_PUBLICATION_TYPE_PREFIX = "dataverse.publication.type.";
    private static final String SEMANTIC_DATAVERSE_PUBLICATION_RELATION_TYPE_PREFIX = "dataverse.publication.relation_type.";

    private static final String SEMANTIC_DATAVERSE_KEYWORD_TERM = "dataverse.keyword.term";
    private static final String SEMANTIC_DATAVERSE_KEYWORD_TERM_URI = "dataverse.keyword.term_uri";
    private static final String SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_NAME = "dataverse.keyword.controlled_vocabulary_name";
    private static final String SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_URL = "dataverse.keyword.controlled_vocabulary_url";

    private static final String SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_AGENCY = "dataverse.other_identifier.agency";
    private static final String SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_IDENTIFIER = "dataverse.other_identifier.identifier";

    private final DataverseServiceProperties dataverseServiceProperties;

    @Autowired
    public DataverseBuilder(PlanBlueprintFieldSearcherService blueprintFieldSearcherService, TemplateFieldSearcherService templateFieldSearcherService, DataverseServiceProperties dataverseServiceProperties){
        this.blueprintFieldSearcherService = blueprintFieldSearcherService;
        this.templateFieldSearcherService = templateFieldSearcherService;
        this.dataverseServiceProperties = dataverseServiceProperties;
    }

    /**
     * Turns a controlled-vocabulary label as returned by the Dataverse schema API (e.g. "Data Collector",
     * "IsCitedBy") into the lower_snake_case segment used by plan blueprint/description template semantic
     * codes (e.g. "dataverse.contributor.type.data_collector"). Vocab values come from the target instance's
     * own schema, not a static list, so this must derive the code generically rather than looking it up.
     */
    private static String toSemanticCodeSegment(String vocabularyLabel) {
        return vocabularyLabel.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /**
     * dataverseLicences: the installation's active licences, read only when the plan has a licence reference.
     * allowedDatasetTypes: the names of the dataset types the target collection allows, or null when they are unknown.
     */
    public DataverseDataset build(PlanModel planModel, Map<String, DataverseFieldSchema> schema, DepositPluginSettings settings, List<Map<String, Object>> dataverseLicences, Collection<String> allowedDatasetTypes) {
        DataverseDataset dataset = new DataverseDataset();

        if (planModel == null) return dataset;

        String metadataLanguage = settings.text(DataverseDepositServiceImpl.SETTING_METADATA_LANGUAGE, this.dataverseServiceProperties.getSpecifiedMetadataLanguage());
        if (metadataLanguage != null && !metadataLanguage.isBlank()) dataset.setMetadataLanguage(metadataLanguage);

        // a type the collection does not allow would fail the whole deposit, so it falls back to the installation's,
        // then is left out, so Dataverse creates a plain "dataset"
        List<String> planDatasetTypes = this.applyListValue(planModel, SEMANTIC_DATAVERSE_DATASET_TYPE);
        String datasetType = planDatasetTypes.stream().map(type -> allowedDatasetType(type, allowedDatasetTypes)).filter(Objects::nonNull).findFirst().orElse(null);
        if (datasetType == null && !planDatasetTypes.isEmpty()) logger.warn("Dataset types the collection does not allow: " + planDatasetTypes);
        if (datasetType == null) datasetType = allowedDatasetType(this.dataverseServiceProperties.getDatasetType(), allowedDatasetTypes);
        dataset.setDatasetType(datasetType);

        DatasetVersion version = new DatasetVersion();
        DataSetMetadataBlock metadataBlock = new DataSetMetadataBlock();
        Citation citation = new Citation();
        List<CitationField> fields = new ArrayList<>();

        fields.add(citationField(schema, "title", FIELD_TYPE_CLASS_PRIMITIVE, false, planModel.getLabel()));
        fields.add(citationField(schema, "dsDescription", FIELD_TYPE_CLASS_COMPOUND, true, this.buildDescriptionFields(planModel)));

        List<String> alternativeTitles = this.applyListValue(planModel, SEMANTIC_DATAVERSE_ALTERNATIVE_TITLE);
        if (!alternativeTitles.isEmpty()) {
            CitationField f = citationField(schema, "alternativeTitle", FIELD_TYPE_CLASS_PRIMITIVE, false, null);
            if (f.isMultiple()) f.setValue(alternativeTitles);
            else f.setValue(alternativeTitles.getFirst());
            fields.add(f);
        }

        List<String> subtitle = this.applyListValue(planModel, SEMANTIC_DATAVERSE_SUBTITLE);
        if (!subtitle.isEmpty()) fields.add(citationField(schema, "subtitle", FIELD_TYPE_CLASS_PRIMITIVE, false, subtitle.getFirst()));

        List<String> notes = this.applyListValue(planModel, SEMANTIC_DATAVERSE_NOTES);
        if (!notes.isEmpty()) fields.add(citationField(schema, "notesText", FIELD_TYPE_CLASS_PRIMITIVE, false, notes.getFirst()));

        List<String> subjectFields = this.applyListValue(planModel, SEMANTIC_DATAVERSE_SUBJECT);
        String defaultSubject = settings.text(DataverseDepositServiceImpl.SETTING_SUBJECT, dataverseServiceProperties.getDefaultSubject());
        if (subjectFields.isEmpty() && defaultSubject != null && !defaultSubject.isBlank()) {
            subjectFields.add(defaultSubject);
        }
        // "subject" is a controlled vocabulary; a plan value that does not exist in the target instance's
        // schema must be dropped, not sent, or Dataverse rejects the whole deposit.
        List<String> validSubjectFields = subjectFields.stream().filter(subject -> this.isValidVocabularyValue(schema, "subject", subject)).toList();
        if (!validSubjectFields.isEmpty()) fields.add(citationField(schema, "subject", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, true, validSubjectFields));

        List<String> alternativeUrl = this.applyListValue(planModel, SEMANTIC_DATAVERSE_ALTERNATIVE_URL);
        String url = null;
        if (alternativeUrl.isEmpty() && PlanAccessType.Public.equals(planModel.getAccessType()) && settings.linkPublicPlan()) {
            url = dataverseServiceProperties.getDomain() + "explore-plans/overview/public/" + planModel.getId().toString();
        } else if (!alternativeUrl.isEmpty()) {
            url = alternativeUrl.getFirst();
        }
        if (url != null) fields.add(citationField(schema, "alternativeURL", FIELD_TYPE_CLASS_PRIMITIVE, false, url));

        List<String> dateOfDeposit = this.applyListValue(planModel, SEMANTIC_DATAVERSE_DATE_OF_DEPOSIT);
        try {
            if (!dateOfDeposit.isEmpty()) {
                LocalDate.parse(dateOfDeposit.getFirst(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                fields.add(citationField(schema, "dateOfDeposit", FIELD_TYPE_CLASS_PRIMITIVE, false, dateOfDeposit.getFirst()));
            }
        } catch (Exception e) {
            if (!dateOfDeposit.isEmpty()) {
                logger.warn("Wrong date of deposit input:" + dateOfDeposit.getFirst());
            }
        }

        fields.add(citationField(schema, "author", FIELD_TYPE_CLASS_COMPOUND, true, this.buildAuthorsFields(planModel, settings)));
        fields.add(citationField(schema, "datasetContact", FIELD_TYPE_CLASS_COMPOUND, true, this.buildContactFields(planModel, settings)));
        fields.add(citationField(schema, "contributor", FIELD_TYPE_CLASS_COMPOUND, true, this.buildContributorsFields(planModel, schema, settings)));
        fields.add(citationField(schema, "grantNumber", FIELD_TYPE_CLASS_COMPOUND, true, this.buildFundingFields(planModel, settings)));
        fields.add(citationField(schema, "publication", FIELD_TYPE_CLASS_COMPOUND, true, this.buildRelatedPublicationFields(planModel,schema)));
        fields.add(citationField(schema, "keyword", FIELD_TYPE_CLASS_COMPOUND, true, this.buildKeywordsFields(planModel)));
        fields.add(citationField(schema, "otherId", FIELD_TYPE_CLASS_COMPOUND, true, this.buildOtherIdentifierFields(planModel)));

        citation.setFields(fields);
        metadataBlock.setCitation(citation);
        version.setMetadataBlocks(metadataBlock);
        this.applyLicence(version, planModel, settings, dataverseLicences);
        dataset.setDatasetVersion(version);

        return dataset;
    }

    /** The type as the collection names it, or null when it does not allow it; any non-blank type when what it allows is unknown. */
    private static String allowedDatasetType(String type, Collection<String> allowedDatasetTypes) {
        if (type == null || type.isBlank()) return null;
        if (allowedDatasetTypes == null) return type.trim();
        return allowedDatasetTypes.stream().filter(allowed -> allowed.equalsIgnoreCase(type.trim())).findFirst().orElse(null);
    }

    private static CitationField citationField(Map<String, DataverseFieldSchema> schema, String typeName, String fallbackTypeClass, boolean fallbackMultiple, Object value) {
        DataverseFieldSchema s = schema != null ? schema.get(typeName) : null;
        String typeClass = s != null ? s.getTypeClass() : fallbackTypeClass;
        boolean multiple = s != null ? s.isMultiple() : fallbackMultiple;
        return new CitationField(typeName, typeClass, multiple, value);
    }

    private List<Map<String, CitationField>> buildDescriptionFields(PlanModel planModel) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){
                List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(SEMANTIC_DATAVERSE_DESCRIPTION, descriptionModel.getDescriptionTemplate().getDefinition().getAllField());
                for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
                    if (field.getData() == null) continue;
                    List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  descriptionModel.getProperties().getFieldSets());

                    for (FieldModel valueField : valueFields) {
                        for (String value : this.answerValues(field, valueField, SEMANTIC_DATAVERSE_DESCRIPTION)) {
                            citationFields.add(Map.of("dsDescriptionValue", new CitationField("dsDescriptionValue", FIELD_TYPE_CLASS_PRIMITIVE, false, value)));
                        }
                    }
                }

            }
        }

        List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(SEMANTIC_DATAVERSE_DESCRIPTION, planModel.getPlanBlueprint().getDefinition().getAllField());
        for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
            if (field.getData() == null) continue;
            List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  planModel.getProperties().getFieldSets());

            for (FieldModel valueField : valueFields) {
                for (String value : this.answerValues(field, valueField, SEMANTIC_DATAVERSE_DESCRIPTION)) {
                    citationFields.add(Map.of("dsDescriptionValue", new CitationField("dsDescriptionValue", FIELD_TYPE_CLASS_PRIMITIVE, false, value)));
                }
            }
        }

        if (planModel.getDescription() != null && !planModel.getDescription().isBlank())
            citationFields.add(Map.of("dsDescriptionValue", new CitationField("dsDescriptionValue", FIELD_TYPE_CLASS_PRIMITIVE, false, planModel.getDescription())));

        return citationFields;
    }

    private List<Map<String, CitationField>> buildAuthorsFields(PlanModel planModel, DepositPluginSettings settings){
        List<Map<String, CitationField>> fields = new ArrayList<>();
        if (planModel == null || planModel.getUsers() == null) return fields;
        List<String> authorKeys = List.of(SEMANTIC_DATAVERSE_AUTHOR_NAME, SEMANTIC_DATAVERSE_AUTHOR_AFFILIATION);

        List<String> organizations = new ArrayList<>();
        List<ReferenceModel> planOrganizations = this.getReferenceModelOfType(planModel, settings.text(DataverseDepositServiceImpl.SETTING_REFERENCE_TYPE_ORGANISATIONS, dataverseServiceProperties.getOrganizationReferenceCode()));
        if (!planOrganizations.isEmpty()) organizations = planOrganizations.stream().map(ReferenceModel::getLabel).toList();

        for (PlanUserModel planUser: settings.authors(planModel.getUsers())) {
            Map<String, CitationField> map = new HashMap<>();
            map.put("authorName", new CitationField("authorName", FIELD_TYPE_CLASS_PRIMITIVE, false, planUser.getUser().getName()));

            if (!organizations.isEmpty()) {
                map.put("authorAffiliation", new CitationField("authorAffiliation", FIELD_TYPE_CLASS_PRIMITIVE, false, String.join(", ", organizations)));
            }
            fields.add(map);
        }

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), authorKeys)) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, descriptionModel.getProperties().getFieldSets(), authorKeys);
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_AUTHOR_NAME, "authorName", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_AUTHOR_AFFILIATION, "authorAffiliation", FIELD_TYPE_CLASS_PRIMITIVE);
                        if (!map.isEmpty()) fields.add(map);
                    }
                }

            }
        }

        for (FieldSetModel fieldSet : this.blueprintFieldSearcherService.searchFieldSetsBySemantics(planModel.getPlanBlueprint(), authorKeys)) {
            List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, planModel.getProperties().getFieldSets(), authorKeys);
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                Map<String, CitationField> map = new HashMap<>();

                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_AUTHOR_NAME, "authorName", FIELD_TYPE_CLASS_PRIMITIVE);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_AUTHOR_AFFILIATION, "authorAffiliation", FIELD_TYPE_CLASS_PRIMITIVE);
                if (!map.isEmpty()) fields.add(map);
            }
        }

        return fields;
    }


    private static @Nullable Object getSchemaVocabularyValues(Map<String, DataverseFieldSchema> schema, String rootName, String fieldName) {
        DataverseFieldSchema rootSchema = schema != null ? schema.get(rootName) : null;
        if (rootSchema == null || rootSchema.getChildFields() == null) return null;
        Map<String, Object> fieldSchema = rootSchema.getChildFields().get(fieldName) != null ? rootSchema.getChildFields().get(fieldName) : null;
        return fieldSchema != null && fieldSchema.get("controlledVocabularyValues") != null && fieldSchema.containsKey("isControlledVocabulary") && fieldSchema.get("isControlledVocabulary").equals(true) ? fieldSchema.get("controlledVocabularyValues") : null;
    }

    private @Nullable Object getSchemaVocabularyValues(Map<String, DataverseFieldSchema> schema, String rootName) {
        DataverseFieldSchema rootSchema = schema != null ? schema.get(rootName) : null;
        return rootSchema != null ? rootSchema.getControlledVocabularyValues() : null;
    }

    /**
     * True when value is one of the allowed controlled-vocabulary entries for a top-level field (e.g. "subject").
     * Dataverse rejects the whole deposit if a controlled-vocabulary field carries a value the target instance's
     * schema does not recognize, so any such value built from plan data must be checked before being sent —
     * a stale/free-text plan value that no longer matches the live vocabulary must be dropped, not submitted.
     */
    private boolean isValidVocabularyValue(Map<String, DataverseFieldSchema> schema, String rootName, String value) {
        if (value == null) return false;
        Object vocabularyValues = getSchemaVocabularyValues(schema, rootName);
        return vocabularyValues instanceof List<?> list && list.contains(value);
    }

    /** Same as {@link #isValidVocabularyValue(Map, String, String)}, for a controlled-vocabulary child field of a compound field (e.g. "publication.publicationRelationType"). */
    private boolean isValidVocabularyValue(Map<String, DataverseFieldSchema> schema, String rootName, String fieldName, String value) {
        if (value == null) return false;
        Object vocabularyValues = getSchemaVocabularyValues(schema, rootName, fieldName);
        return vocabularyValues instanceof List<?> list && list.contains(value);
    }

    /**
     * The relation type set on an input that carries a publication identifier type key: its
     * "dataverse.publication.relation_type.<type>" key, one per relation type of the instance's citation schema.
     * The legacy "dataverse.publication.type.<type>" key of a relation type is still accepted, but only when no
     * identifier type reduces to the same segment, since that key then names the identifier type.
     */
    private String findPublicationRelationType(org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field, Map<String, DataverseFieldSchema> schema, List<String> publicationIDTypes) {
        if (field.getSemantics() == null) return null;
        Object publicationRelationTypeValues = getSchemaVocabularyValues(schema,"publication","publicationRelationType");
        List<String> relationTypes =  publicationRelationTypeValues instanceof List ? (List<String>) publicationRelationTypeValues : List.of();
        for (String relationType: relationTypes) {
            if (field.getSemantics().contains(SEMANTIC_DATAVERSE_PUBLICATION_RELATION_TYPE_PREFIX + toSemanticCodeSegment(relationType))) return relationType;
        }
        Set<String> identifierTypeSegments = publicationIDTypes.stream().map(DataverseBuilder::toSemanticCodeSegment).collect(Collectors.toSet());
        for (String relationType: relationTypes) {
            String segment = toSemanticCodeSegment(relationType);
            if (!identifierTypeSegments.contains(segment) && field.getSemantics().contains(SEMANTIC_DATAVERSE_PUBLICATION_TYPE_PREFIX + segment)) return relationType;
        }
        return null;
    }

    private List<Map<String, CitationField>> buildContributorsFields(PlanModel planModel , Map<String, DataverseFieldSchema> schema, DepositPluginSettings settings){
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getUsers() != null) {
            for (PlanUserModel planUser: planModel.getUsers()) {
                String contributorType = planUser.getRole().equals(PlanUserRole.Owner) ? CONTRIBUTOR_TYPE_EDITOR : CONTRIBUTOR_TYPE_MEMBER;
                if (!this.isValidVocabularyValue(schema, "contributor", FIELD_CONTRIBUTOR_TYPE, contributorType)) continue;
                Map<String, CitationField> map = new HashMap<>();
                map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, planUser.getUser().getName()));
                map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, contributorType));
                citationFields.add(map);
            }
        }

        if (this.isValidVocabularyValue(schema, "contributor", FIELD_CONTRIBUTOR_TYPE, CONTRIBUTOR_TYPE_RESEARCHER)) {
            List<ReferenceModel> planResearchers = this.getReferenceModelOfType(planModel, settings.text(DataverseDepositServiceImpl.SETTING_REFERENCE_TYPE_RESEARCHERS, dataverseServiceProperties.getResearcherReferenceCode()));
            for (ReferenceModel researcher : planResearchers) {
                Map<String, CitationField> map = new HashMap<>();
                map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, researcher.getLabel()));
                map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, CONTRIBUTOR_TYPE_RESEARCHER));
                citationFields.add(map);
            }
        }

        if (this.isValidVocabularyValue(schema, "contributor", FIELD_CONTRIBUTOR_TYPE, CONTRIBUTOR_TYPE_FUNDER)) {
            List<ReferenceModel> planFunders = this.getReferenceModelOfType(planModel, settings.text(DataverseDepositServiceImpl.SETTING_REFERENCE_TYPE_FUNDERS, dataverseServiceProperties.getFunderReferenceCode()));
            for (ReferenceModel funder : planFunders) {
                Map<String, CitationField> map = new HashMap<>();
                map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, funder.getLabel()));
                map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, CONTRIBUTOR_TYPE_FUNDER));
                citationFields.add(map);
            }
        }

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){
                Object contributorTypeValues = getSchemaVocabularyValues(schema,"contributor",FIELD_CONTRIBUTOR_TYPE);
                List<String> contributorTypes =  contributorTypeValues instanceof List ? (List<String>) contributorTypeValues : List.of();
                for (String contributorType: contributorTypes){
                    String semanticCode = "dataverse.contributor.type." + toSemanticCodeSegment(contributorType);
                    List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(semanticCode, descriptionModel.getDescriptionTemplate().getDefinition().getAllField());
                    for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
                        if (field.getData() == null) continue;
                        List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  descriptionModel.getProperties().getFieldSets());

                        for (FieldModel valueField : valueFields) {
                            for (String value : this.answerValues(field, valueField, semanticCode)) {
                                Map<String, CitationField> map = new HashMap<>();
                                map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, contributorType));
                                map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, value));
                                citationFields.add(map);
                            }
                        }
                    }
                }
            }
        }
        Object contributorTypeValues = getSchemaVocabularyValues(schema,"contributor",FIELD_CONTRIBUTOR_TYPE);
        List<String> contributorTypes =  contributorTypeValues instanceof List ? (List<String>) contributorTypeValues : List.of();
        for (String contributorType: contributorTypes){
            String semanticCode = "dataverse.contributor.type." + toSemanticCodeSegment(contributorType);
            List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(semanticCode, planModel.getPlanBlueprint().getDefinition().getAllField());
            for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
                if (field.getData() == null) continue;
                List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  planModel.getProperties().getFieldSets());

                for (FieldModel valueField : valueFields) {
                    for (String value : this.answerValues(field, valueField, semanticCode)) {
                        Map<String, CitationField> map = new HashMap<>();
                        map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, contributorType));
                        map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, value));
                        citationFields.add(map);
                    }
                }

            }
        }

        return citationFields;
    }

    private List<Map<String, CitationField>> buildFundingFields(PlanModel planModel, DepositPluginSettings settings){
        List<Map<String, CitationField>> fields = new ArrayList<>();
        if (planModel == null) return fields;

        List<ReferenceModel> planFunders = this.getReferenceModelOfType(planModel, settings.text(DataverseDepositServiceImpl.SETTING_REFERENCE_TYPE_FUNDERS, dataverseServiceProperties.getFunderReferenceCode()));
        List<ReferenceModel> planGrants = this.getReferenceModelOfType(planModel, settings.text(DataverseDepositServiceImpl.SETTING_REFERENCE_TYPE_GRANTS, dataverseServiceProperties.getGrantReferenceCode()));

        // the plan does not link grants to funders: grants go with its only funder, otherwise funders and grants stand alone
        String agency = planFunders.size() == 1 ? planFunders.getFirst().getLabel() : null;
        for (ReferenceModel grant: planGrants) {
            Map<String, CitationField> map = new HashMap<>();
            if (agency != null) map.put("grantNumberAgency", new CitationField("grantNumberAgency", FIELD_TYPE_CLASS_PRIMITIVE, false, agency));
            map.put("grantNumberValue", new CitationField("grantNumberValue", FIELD_TYPE_CLASS_PRIMITIVE, false, grant.getReference()));
            fields.add(map);
        }

        if (planGrants.isEmpty() || planFunders.size() > 1) {
            for (ReferenceModel funder: planFunders) {
                Map<String, CitationField> map = new HashMap<>();
                map.put("grantNumberAgency", new CitationField("grantNumberAgency", FIELD_TYPE_CLASS_PRIMITIVE, false, funder.getLabel()));
                fields.add(map);
            }
        }

        return fields;
    }

    private List<Map<String, CitationField>> buildContactFields(PlanModel planModel, DepositPluginSettings settings){
        List<Map<String, CitationField>> fields = new ArrayList<>();
        if (planModel == null || planModel.getProperties() == null) return fields;
        List<String> contactKeys = List.of(SEMANTIC_DATAVERSE_CONTACT_NAME, SEMANTIC_DATAVERSE_CONTACT_EMAIL, SEMANTIC_DATAVERSE_CONTACT_AFFILIATION);

        if (planModel.getProperties().getContacts() != null) {
            for (PlanContactModel planContactModel: planModel.getProperties().getContacts()) {
                Map<String, CitationField> map = new HashMap<>();
                String name = Stream.of(planContactModel.getFirstName(), planContactModel.getLastName()).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(" "));
                if (!name.isEmpty()) map.put("datasetContactName", new CitationField("datasetContactName", FIELD_TYPE_CLASS_PRIMITIVE, false, name));
                map.put("datasetContactEmail", new CitationField("datasetContactEmail", FIELD_TYPE_CLASS_PRIMITIVE, false, planContactModel.getEmail()));
                fields.add(map);
            }
        }

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), contactKeys)) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, descriptionModel.getProperties().getFieldSets(), contactKeys);
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_NAME, "datasetContactName", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_EMAIL, "datasetContactEmail", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_AFFILIATION, "datasetContactAffiliation", FIELD_TYPE_CLASS_PRIMITIVE);
                        if (!map.isEmpty()) fields.add(map);
                    }
                }

            }
        }

        for (FieldSetModel fieldSet : this.blueprintFieldSearcherService.searchFieldSetsBySemantics(planModel.getPlanBlueprint(), contactKeys)) {
            List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, planModel.getProperties().getFieldSets(), contactKeys);
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                Map<String, CitationField> map = new HashMap<>();

                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_NAME, "datasetContactName", FIELD_TYPE_CLASS_PRIMITIVE);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_EMAIL, "datasetContactEmail", FIELD_TYPE_CLASS_PRIMITIVE);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_AFFILIATION, "datasetContactAffiliation", FIELD_TYPE_CLASS_PRIMITIVE);
                if (!map.isEmpty()) fields.add(map);
            }
        }

        // Dataverse requires a contact with an e-mail, so the organisation's applies when the plan names none
        String contactName = settings.text(DataverseDepositServiceImpl.SETTING_CONTACT_NAME);
        String contactEmail = settings.text(DataverseDepositServiceImpl.SETTING_CONTACT_EMAIL);
        if (fields.isEmpty() && contactEmail != null) {
            Map<String, CitationField> map = new HashMap<>();
            if (contactName != null) map.put("datasetContactName", new CitationField("datasetContactName", FIELD_TYPE_CLASS_PRIMITIVE, false, contactName));
            map.put("datasetContactEmail", new CitationField("datasetContactEmail", FIELD_TYPE_CLASS_PRIMITIVE, false, contactEmail));
            fields.add(map);
        }
        return fields;
    }

    private List<Map<String, CitationField>> buildRelatedPublicationFields(PlanModel planModel ,Map<String, DataverseFieldSchema> schema) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){
                Object publicationIDTypesList = getSchemaVocabularyValues(schema,"publication","publicationIDType");
                List<String> publicationIDTypes =  publicationIDTypesList instanceof List ? (List<String>) publicationIDTypesList : List.of();
                for (String identifierType: publicationIDTypes){
                    String semanticCode = SEMANTIC_DATAVERSE_PUBLICATION_TYPE_PREFIX + toSemanticCodeSegment(identifierType);
                    List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(semanticCode, descriptionModel.getDescriptionTemplate().getDefinition().getAllField());
                    for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
                        if (field.getData() == null) continue;
                        List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  descriptionModel.getProperties().getFieldSets());

                        for (FieldModel valueField : valueFields) {
                            for (String value : this.answerValues(field, valueField, semanticCode)) {
                                Map<String, CitationField> map = new HashMap<>();
                                map.put("publicationIDType", new CitationField("publicationIDType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, identifierType));
                                map.put("publicationIDNumber", new CitationField("publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false, value));

                                String relationType = this.findPublicationRelationType(field, schema, publicationIDTypes);
                                if (relationType != null) map.put("publicationRelationType", new CitationField("publicationRelationType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, relationType));
                                citationFields.add(map);
                            }
                        }
                    }
                }

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), List.of(
                        SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER,
                        SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION))) {
                    // own keys only: a reference type's publication keys are read per picked reference below
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, descriptionModel.getProperties().getFieldSets());
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildControlledVocabularyFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE, "publication", "publicationIDType", schema, false);
                        this.buildControlledVocabularyFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE, "publication", "publicationRelationType", schema, false);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION, "publicationCitation", FIELD_TYPE_CLASS_PRIMITIVE, false);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER, "publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL, "publicationURL", FIELD_TYPE_CLASS_PRIMITIVE, false);
                        if (!map.isEmpty()) citationFields.add(map);
                    }
                }

                PropertyDefinitionModel properties = descriptionModel.getProperties();
                if (properties == null) continue;

                Map<String, PropertyDefinitionFieldSetModel> fieldSets = properties.getFieldSets();
                if (fieldSets == null) continue;

                for (PropertyDefinitionFieldSetModel fieldSet : fieldSets.values()) {
                    List<PropertyDefinitionFieldSetItemModel> items = fieldSet.getItems();
                    if (items == null) continue;

                    for (PropertyDefinitionFieldSetItemModel item : items) {
                        Map<String, FieldModel> definitionFields = item.getFields();
                        if (definitionFields == null) continue;

                        for (FieldModel field : definitionFields.values()) {
                            List<ReferenceModel> references = field.getReferences();
                            if (references == null) continue;

                            for (ReferenceModel reference : references) {
                                Map<String, CitationField> map = this.buildPublicationFromReference(reference, schema);
                                if (!map.isEmpty()) citationFields.add(map);
                            }
                        }
                    }
                }


            }
        }
        Object publicationIDTypesList = getSchemaVocabularyValues(schema,"publication","publicationIDType");
        List<String> publicationIDTypes =  publicationIDTypesList instanceof List ? (List<String>) publicationIDTypesList : List.of();
        for (String identifierType: publicationIDTypes){
            String semanticCode = SEMANTIC_DATAVERSE_PUBLICATION_TYPE_PREFIX + toSemanticCodeSegment(identifierType);
            List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields = this.findSchematicValues(semanticCode, planModel.getPlanBlueprint().getDefinition().getAllField());
            for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
                if (field.getData() == null) continue;
                List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  planModel.getProperties().getFieldSets());

                for (FieldModel valueField : valueFields) {
                    for (String value : this.answerValues(field, valueField, semanticCode)) {
                        Map<String, CitationField> map = new HashMap<>();
                        map.put("publicationIDType", new CitationField("publicationIDType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, identifierType));
                        map.put("publicationIDNumber", new CitationField("publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false, value));

                        String relationType = this.findPublicationRelationType(field, schema, publicationIDTypes);
                        if (relationType != null) map.put("publicationRelationType", new CitationField("publicationRelationType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, relationType));
                        citationFields.add(map);
                    }
                }
            }
        }
        for (FieldSetModel fieldSet : this.blueprintFieldSearcherService.searchFieldSetsBySemantics(planModel.getPlanBlueprint(), List.of(
                SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER,
                SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION))) {
            // own keys only: a reference type's publication keys are read per picked reference below
            List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, planModel.getProperties().getFieldSets());
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                Map<String, CitationField> map = new HashMap<>();

                this.buildControlledVocabularyFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE, "publication", "publicationIDType", schema, false);
                this.buildControlledVocabularyFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE, "publication", "publicationRelationType", schema, false);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION, "publicationCitation", FIELD_TYPE_CLASS_PRIMITIVE, false);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER, "publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL, "publicationURL", FIELD_TYPE_CLASS_PRIMITIVE, false);
                if (!map.isEmpty()) citationFields.add(map);
            }
        }


        PlanPropertiesModel properties = planModel.getProperties();
        if (properties == null) return citationFields;

        Map<String, PropertyDefinitionFieldSetModel> fieldSets = properties.getFieldSets();
        if (fieldSets == null) return citationFields;

        for (PropertyDefinitionFieldSetModel fieldSet : fieldSets.values()) {
            List<PropertyDefinitionFieldSetItemModel> items = fieldSet.getItems();
            if (items == null) continue;

            for (PropertyDefinitionFieldSetItemModel item : items) {
                Map<String, FieldModel> definitionFields = item.getFields();
                if (definitionFields == null) continue;

                for (FieldModel field : definitionFields.values()) {
                    List<ReferenceModel> references = field.getReferences();
                    if (references == null) continue;

                    for (ReferenceModel reference : references) {
                        Map<String, CitationField> map = this.buildPublicationFromReference(reference, schema);
                        if (!map.isEmpty()) citationFields.add(map);
                    }
                }
            }
        }


        return citationFields;
    }

    private List<Map<String, CitationField>> buildKeywordsFields(PlanModel planModel) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;
        List<String> keywordKeys = List.of(SEMANTIC_DATAVERSE_KEYWORD_TERM, SEMANTIC_DATAVERSE_KEYWORD_TERM_URI, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_NAME, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_URL);

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), keywordKeys)) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, descriptionModel.getProperties().getFieldSets(), keywordKeys);
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_TERM, "keywordValue", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_TERM_URI, "keywordTermURI", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_NAME, "keywordVocabulary", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_URL, "keywordVocabularyURI", FIELD_TYPE_CLASS_PRIMITIVE);
                        if (!map.isEmpty()) citationFields.add(map);
                    }
                }

            }
        }

        for (FieldSetModel fieldSet : this.blueprintFieldSearcherService.searchFieldSetsBySemantics(planModel.getPlanBlueprint(), keywordKeys)) {
            List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, planModel.getProperties().getFieldSets(), keywordKeys);
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                Map<String, CitationField> map = new HashMap<>();

                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_TERM, "keywordValue", FIELD_TYPE_CLASS_PRIMITIVE);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_TERM_URI, "keywordTermURI", FIELD_TYPE_CLASS_PRIMITIVE);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_NAME, "keywordVocabulary", FIELD_TYPE_CLASS_PRIMITIVE);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_URL, "keywordVocabularyURI", FIELD_TYPE_CLASS_PRIMITIVE);
                if (!map.isEmpty()) citationFields.add(map);
            }
        }

        return citationFields;
    }

    private List<Map<String, CitationField>> buildOtherIdentifierFields(PlanModel planModel) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;
        List<String> otherIdentifierKeys = List.of(SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_AGENCY, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_IDENTIFIER);

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), otherIdentifierKeys)) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, descriptionModel.getProperties().getFieldSets(), otherIdentifierKeys);
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_AGENCY, "otherIdAgency", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel.getDescriptionTemplate().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_IDENTIFIER, "otherIdValue", FIELD_TYPE_CLASS_PRIMITIVE);
                        if (!map.isEmpty()) citationFields.add(map);
                    }
                }

            }
        }

        for (FieldSetModel fieldSet : this.blueprintFieldSearcherService.searchFieldSetsBySemantics(planModel.getPlanBlueprint(), otherIdentifierKeys)) {
            List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetRows(fieldSet, planModel.getProperties().getFieldSets(), otherIdentifierKeys);
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                Map<String, CitationField> map = new HashMap<>();

                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_AGENCY, "otherIdAgency", FIELD_TYPE_CLASS_PRIMITIVE);
                this.buildCitationFieldFromFieldSetSemantic(map, planModel.getPlanBlueprint().getDefinition().getAllField(), fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_IDENTIFIER, "otherIdValue", FIELD_TYPE_CLASS_PRIMITIVE);
                if (!map.isEmpty()) citationFields.add(map);
            }
        }

        return citationFields;
    }

    /**
     * The related publication a picked reference makes through its reference type: the publication keys the type gives
     * its fields, read from the reference's fields, then those it gives its id, name and description.
     */
    private Map<String, CitationField> buildPublicationFromReference(ReferenceModel reference, Map<String, DataverseFieldSchema> schema) {
        Map<String, CitationField> map = new HashMap<>();
        if (reference == null || reference.getType() == null || reference.getType().getDefinition() == null) return map;
        ReferenceTypeDefinitionModel definition = reference.getType().getDefinition();

        List<ReferenceTypeFieldModel> semanticReferences = new ArrayList<>();
        if (definition.getFields() != null) {
            for (ReferenceTypeFieldModel refField : definition.getFields()) {
                if (refField.getSemantics() != null && refField.getSemantics().stream().anyMatch(SEMANTIC_DATAVERSE_RELATED_PUBLICATION_KEYS::contains)) semanticReferences.add(refField);
            }
        }
        if (!semanticReferences.isEmpty() && reference.getDefinition() != null && reference.getDefinition().getFields() != null) {
            for (ReferenceFieldModel defField : reference.getDefinition().getFields()) {
                if (defField == null || defField.getCode() == null) continue;

                for (ReferenceTypeFieldModel semanticRef : semanticReferences) {
                    if (semanticRef.getCode() == null || !defField.getCode().equals(semanticRef.getCode())) continue;
                    for (String semantic : SEMANTIC_DATAVERSE_RELATED_PUBLICATION_KEYS) {
                        if (semanticRef.getSemantics().contains(semantic)) this.putPublicationValue(map, semantic, defField.getValue(), schema);
                    }
                }
            }
        }
        // the type's id, name and description carry keys too, so a reference without fields still makes one
        for (String semantic : SEMANTIC_DATAVERSE_RELATED_PUBLICATION_KEYS) {
            String value = ReferenceSemantics.systemValue(reference, definition, semantic);
            if (value != null) this.putPublicationValue(map, semantic, value, schema);
        }
        return map;
    }

    private void putPublicationValue(Map<String, CitationField> map, String semantic, String value, Map<String, DataverseFieldSchema> schema) {
        switch (semantic) {
            case SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER -> map.put("publicationIDNumber", new CitationField("publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false, value));
            case SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE -> {
                if (this.isValidVocabularyValue(schema, "publication", "publicationIDType", value)) map.put("publicationIDType", new CitationField("publicationIDType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, value));
            }
            case SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE -> {
                if (this.isValidVocabularyValue(schema, "publication", "publicationRelationType", value)) map.put("publicationRelationType", new CitationField("publicationRelationType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, value));
            }
            case SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION -> {
                if (value != null && !value.isBlank()) map.put("publicationCitation", new CitationField("publicationCitation", FIELD_TYPE_CLASS_PRIMITIVE, false, value));
            }
            case SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL -> {
                if (value != null && !value.isBlank()) map.put("publicationURL", new CitationField("publicationURL", FIELD_TYPE_CLASS_PRIMITIVE, false, value));
            }
            default -> {
            }
        }
    }

    private void buildCitationFieldFromFieldSetSemantic(Map<String, CitationField> map, List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fieldModels, FieldSetModel fieldSet,
                                                        PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel,
                                                        String semantic, String citationFieldName, String typeClass) {
        this.buildCitationFieldFromFieldSetSemantic(map, fieldModels, fieldSet, propertyDefinitionFieldSetItemModel, semantic, citationFieldName, typeClass, true);
    }

    /** throughType: false to read only the inputs carrying the key themselves, not those whose reference type gives it. */
    private void buildCitationFieldFromFieldSetSemantic(Map<String, CitationField> map, List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fieldModels, FieldSetModel fieldSet,
                                                        PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel,
                                                        String semantic, String citationFieldName, String typeClass, boolean throughType) {
        FieldModel fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, semantic, throughType);
        if (fieldValue != null) {
            org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field = fieldModels.stream().filter(x-> x.getId().equals(fieldValue.getId())).findFirst().orElse(null);
            String value = this.extractSchematicSingleValue(field, fieldValue, semantic);
            if (value != null) map.put(citationFieldName, new CitationField(citationFieldName, typeClass, false, value));
        }
    }

    /**
     * Same as {@link #buildCitationFieldFromFieldSetSemantic(Map, List, FieldSetModel, PropertyDefinitionFieldSetItemModel, String, String, String, boolean)},
     * for a controlled-vocabulary child field (rootName.citationFieldName): the plan's free-text value is
     * checked against the live schema vocabulary before being written, since Dataverse rejects the whole
     * deposit if a controlled-vocabulary field carries a value the target instance's schema does not
     * recognize (e.g. a stale value from before the description template's options changed).
     */
    private void buildControlledVocabularyFieldFromFieldSetSemantic(Map<String, CitationField> map, List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fieldModels, FieldSetModel fieldSet,
                                                        PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel,
                                                        String semantic, String rootName, String citationFieldName, Map<String, DataverseFieldSchema> schema, boolean throughType) {
        FieldModel fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, semantic, throughType);
        if (fieldValue == null) return;
        org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field = fieldModels.stream().filter(x-> x.getId().equals(fieldValue.getId())).findFirst().orElse(null);
        String value = this.extractSchematicSingleValue(field, fieldValue, semantic);
        if (value != null && this.isValidVocabularyValue(schema, rootName, citationFieldName, value)) {
            map.put(citationFieldName, new CitationField(citationFieldName, FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, value));
        }
    }

    public List<ReferenceModel> licenceReferences(PlanModel planModel, DepositPluginSettings settings) {
        if (planModel == null) return List.of();
        return this.getReferenceModelOfType(planModel, settings.text(DataverseDepositServiceImpl.SETTING_REFERENCE_TYPE_LICENCES, dataverseServiceProperties.getLicenseReferenceCode()));
    }

    /** The plan's licence when the installation offers it, else the organisation's licence, else the CC0 terms of use. */
    private void applyLicence(DatasetVersion version, PlanModel planModel, DepositPluginSettings settings, List<Map<String, Object>> dataverseLicences) {
        for (ReferenceModel reference : this.licenceReferences(planModel, settings)) {
            Map<String, Object> licence = this.findDataverseLicence(reference, dataverseLicences);
            if (licence == null) continue;
            Map<String, String> license = new LinkedHashMap<>();
            license.put("name", String.valueOf(licence.get("name")));
            if (licence.get("uri") != null) license.put("uri", String.valueOf(licence.get("uri")));
            version.setLicense(license);
            return;
        }
        String licence = settings.text(DataverseDepositServiceImpl.SETTING_LICENCE);
        if (licence != null) {
            version.setLicense(Map.of("name", licence));
            return;
        }
        version.setTermsOfUse(TERMS_OF_USE);
    }

    private Map<String, Object> findDataverseLicence(ReferenceModel reference, List<Map<String, Object>> dataverseLicences) {
        if (reference == null || dataverseLicences == null || dataverseLicences.isEmpty()) return null;
        Set<String> candidates = new HashSet<>();
        candidates.add(normalizeLicence(reference.getLabel()));
        candidates.add(normalizeLicence(reference.getReference()));
        if (reference.getDefinition() != null && reference.getDefinition().getFields() != null) {
            for (ReferenceFieldModel field : reference.getDefinition().getFields()) candidates.add(normalizeLicence(field.getValue()));
        }
        candidates.remove(null);
        return dataverseLicences.stream()
                .filter(x -> candidates.contains(normalizeLicence(x.get("uri") != null ? x.get("uri").toString() : null)) || candidates.contains(normalizeLicence(x.get("name") != null ? x.get("name").toString() : null)))
                .findFirst().orElse(null);
    }

    private static String normalizeLicence(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT).replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    public String buildDataverseIdentifier(PlanModel planModel, DepositPluginSettings settings){
        String collection = settings.text(DataverseDepositServiceImpl.SETTING_COLLECTION, dataverseServiceProperties.getAlias());
        if (planModel == null) return collection;

        String value = this.applyValue(planModel, SEMANTIC_DATAVERSE_IDENTIFIER);

        return value != null ? value : collection;
    }


    //region  plan blueprint
    private List<ReferenceModel> getReferenceModelOfType(PlanModel planModel, String code){
        List<ReferenceModel> response = new ArrayList<>();
        if (planModel.getReferences() == null) return response;
        for (PlanReferenceModel planReferenceModel : planModel.getReferences()){
            if (planReferenceModel.getReference() != null && planReferenceModel.getReference().getType() != null && planReferenceModel.getReference().getType().getCode() != null  && planReferenceModel.getReference().getType().getCode().equals(code)){
                response.add(planReferenceModel.getReference());
            }
        }
        return response;
    }

    //endregion

    //region description template

    private String applyValue(PlanModel planModel, String semanticCode) {
        List<String> value =this.applyListValue(planModel, semanticCode);
        return !value.isEmpty()? value.getFirst(): null;
    }

    private List<String> applyListValue(PlanModel planModel, String semanticCode) {
        List<String> fields = new ArrayList<>();

        //plan blueprint semantics
        List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fieldsWithSemantics = this.findSchematicValues(semanticCode, planModel.getPlanBlueprint().getDefinition().getAllField());
        Set<String> values = extractSchematicValues(fieldsWithSemantics, planModel.getProperties().getFieldSets(), semanticCode);

        if (!values.isEmpty()) {
            fields.addAll(values.stream().toList());
        }

        if (planModel.getDescriptions() != null) {
            //description template
            for (DescriptionModel descriptionModel: planModel.getDescriptions()) {
                fieldsWithSemantics = this.findSchematicValues(semanticCode, descriptionModel.getDescriptionTemplate().getDefinition().getAllField());
                values = extractSchematicValues(fieldsWithSemantics, descriptionModel.getProperties().getFieldSets(), semanticCode);
                if (!values.isEmpty()) {
                    fields.addAll(values.stream().toList());
                }
            }
        }

        return fields;
    }

    /** The inputs carrying the key, then the reference inputs whose type gives it, so an input's own key comes first. */
    private List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> findSchematicValues(String relatedId, List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields){
        return Stream.concat(fields.stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(relatedId)), fields.stream().filter(x -> ReferenceSemantics.throughType(x, relatedId))).toList();
    }

    private Set<String> extractSchematicValues(List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> fields, Map<String, PropertyDefinitionFieldSetModel> fieldSets, String semantic) {
        Set<String> values = new HashSet<>();
        for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : fields) {
            if (field.getData() == null) continue;
            List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(), fieldSets);
            for (FieldModel valueField : valueFields) {
                values.addAll(this.extractSchematicValue(field, valueField, semantic));
            }
        }
        return values;
    }

    private String extractSchematicSingleValue(org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field, FieldModel valueField, String semantic) {
        Set<String> value = this.extractSchematicValue(field, valueField, semantic);
        return !value.isEmpty() ? value.stream().findFirst().orElse(null): null;
    }

    /** The values an answer adds: its first, as before, or one per picked reference when the key comes through the reference type. */
    private List<String> answerValues(org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field, FieldModel valueField, String semantic) {
        if (ReferenceSemantics.throughType(field, semantic)) return this.referenceTypeValues(field, valueField, semantic);
        String value = this.extractSchematicSingleValue(field, valueField, semantic);
        return value != null ? List.of(value) : List.of();
    }

    /** The id, name, description or field value each picked reference gives the key through the input's reference type. */
    private List<String> referenceTypeValues(org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field, FieldModel valueField, String semantic) {
        List<String> values = new ArrayList<>();
        if (valueField == null || valueField.getReferences() == null || !(field.getData() instanceof ReferenceTypeDataModel data)) return values;
        for (ReferenceModel referenceModel : valueField.getReferences()) {
            String value = ReferenceSemantics.value(referenceModel, data.getReferenceType(), semantic);
            if (value != null && !values.contains(value)) values.add(value);
        }
        return values;
    }

    private Set<String> extractSchematicValue(org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field, FieldModel valueField, String semantic) {

        Set<String> values = new HashSet<>();

        if (field == null || field.getData() == null) return values;

        switch (field.getData().getFieldType()) {
            case FREE_TEXT, TEXT_AREA, RICH_TEXT_AREA -> {
                if (valueField.getTextValue() != null && !valueField.getTextValue().isBlank()) values.add(valueField.getTextValue());
            }
            case NUMBER -> {
                if (valueField.getNumberValue() != null) values.add(String.valueOf(valueField.getNumberValue()));
            }
            case BOOLEAN_DECISION, CHECK_BOX -> {
                if (valueField.getBooleanValue() != null) values.add(valueField.getBooleanValue().toString());
            }
            case DATE_PICKER -> {
                if (valueField.getDateValue() != null) values.add(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(valueField.getDateValue()));
            }
            case DATASET_IDENTIFIER, VALIDATION -> {
                if (valueField.getExternalIdentifier() != null && valueField.getExternalIdentifier().getIdentifier() != null && !valueField.getExternalIdentifier().getIdentifier().isBlank()) {
                    values.add(valueField.getExternalIdentifier().getIdentifier());
                }
            }
            case TAGS -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    values.addAll(valueField.getTextListValue());
                }
            }
            case SELECT -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    SelectDataModel selectDataModel = (SelectDataModel)field.getData();
                    if (selectDataModel != null && selectDataModel.getOptions() != null && !selectDataModel.getOptions().isEmpty()){
                        for (SelectDataModel.OptionModel option : selectDataModel.getOptions()){
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) values.add(option.getLabel());
                        }
                    }
                }
            }
            case RADIO_BOX -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    RadioBoxDataModel radioBoxModel = (RadioBoxDataModel)field.getData();
                    if (radioBoxModel != null && radioBoxModel.getOptions() != null && !radioBoxModel.getOptions().isEmpty()){
                        for (RadioBoxDataModel.RadioBoxOptionModel option : radioBoxModel.getOptions()){
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) values.add(option.getLabel());
                        }
                    }
                }
            }
            case REFERENCE_TYPES -> {
                // through the type no definition fields are needed: the id, name or description can carry the key
                if (ReferenceSemantics.throughType(field, semantic)) values.addAll(this.referenceTypeValues(field, valueField, semantic));
                else if (valueField.getReferences() != null && !valueField.getReferences().isEmpty()) {
                    for (ReferenceModel referenceModel : valueField.getReferences()) {
                        if (referenceModel == null
                                || referenceModel.getType() == null || referenceModel.getType().getCode() == null || referenceModel.getType().getCode().isBlank()
                                || referenceModel.getDefinition() == null || referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()) continue;
                        if (referenceModel.getReference() != null && !referenceModel.getReference().isBlank()) {
                            values.add(referenceModel.getReference());
                        }
                    }
                }
            }
        }

        return values;
    }

    private List<FieldModel> findValueFieldsByIds(String fieldId, Map<String, PropertyDefinitionFieldSetModel> fieldSets){
        List<FieldModel> models = new ArrayList<>();
        if (fieldSets == null || fieldSets.isEmpty()) return models;
        for (PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel : fieldSets.values()){
            if (propertyDefinitionFieldSetModel == null ||propertyDefinitionFieldSetModel.getItems() == null || propertyDefinitionFieldSetModel.getItems().isEmpty()) continue;
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetModel.getItems()){
                if (propertyDefinitionFieldSetItemModel == null ||propertyDefinitionFieldSetItemModel.getFields() == null || propertyDefinitionFieldSetItemModel.getFields().isEmpty()) continue;
                for (Map.Entry<String, FieldModel> entry : propertyDefinitionFieldSetItemModel.getFields().entrySet()){
                    if (entry == null || entry.getValue() == null) continue;
                    if (entry.getKey().equalsIgnoreCase(fieldId)) models.add(entry.getValue());
                }
            }
        }
        return models;
    }

    private org.opencdmp.commonmodels.models.plandescriptionfields.FieldModel findValueFieldBySemantic(FieldSetModel fieldSet, org.opencdmp.commonmodels.models.plandescriptionfields.PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel, String semantic, boolean throughType){
        org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field = this.templateFieldSearcherService.findFieldBySemantic(fieldSet, semantic);
        if (field == null || !ReferenceSemantics.throughType(field, semantic)) return field != null ? propertyDefinitionFieldSetItemModel.getFields().getOrDefault(field.getId(), null) : null;
        if (!throughType) return null;
        // reference inputs giving the same key are alternatives: the one this row picked a reference in
        for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel carrier : fieldSet.getAllField()) {
            if (!ReferenceSemantics.carries(carrier, semantic)) continue;
            FieldModel value = propertyDefinitionFieldSetItemModel.getFields().get(carrier.getId());
            if (value != null && value.getReferences() != null && !value.getReferences().isEmpty()) return value;
        }
        return propertyDefinitionFieldSetItemModel.getFields().getOrDefault(field.getId(), null);
    }

    /**
     * The answer rows of the field set, one per object their picked references make. A reference input giving keys only
     * through its type makes an object per pick: inputs giving the same keys are alternatives, each pick a row of its
     * own; inputs giving other keys complete it, one pick of each. Inputs carrying the keys themselves read as before.
     */
    private List<PropertyDefinitionFieldSetItemModel> findFieldSetRows(FieldSetModel fieldSetModel, Map<String, PropertyDefinitionFieldSetModel> fieldSets, List<String> semantics) {
        List<PropertyDefinitionFieldSetItemModel> rows = new ArrayList<>();
        for (PropertyDefinitionFieldSetItemModel item : this.findFieldSetValue(fieldSetModel, fieldSets)) rows.addAll(this.rowsPerPick(fieldSetModel, item, semantics));
        return rows;
    }

    private List<PropertyDefinitionFieldSetItemModel> rowsPerPick(FieldSetModel fieldSet, PropertyDefinitionFieldSetItemModel item, List<String> semantics) {
        List<org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel> templateFields = fieldSet.getAllField();
        if (item == null || item.getFields() == null || templateFields == null) return Collections.singletonList(item);
        // a key an input of the set carries itself is read from that input, not through a reference type
        Set<String> owned = templateFields.stream().filter(x -> x.getSemantics() != null).flatMap(x -> x.getSemantics().stream()).collect(Collectors.toSet());
        List<Set<String>> keys = new ArrayList<>();
        List<List<Map.Entry<String, ReferenceModel>>> alternatives = new ArrayList<>();
        boolean several = false;
        for (org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel field : templateFields) {
            if (!(field.getData() instanceof ReferenceTypeDataModel data)) continue;
            Set<String> fieldKeys = new HashSet<>(ReferenceSemantics.keys(data.getReferenceType()));
            fieldKeys.retainAll(semantics);
            fieldKeys.removeAll(owned);
            FieldModel stored = item.getFields().get(field.getId());
            if (fieldKeys.isEmpty() || stored == null || stored.getReferences() == null) continue;
            List<Map.Entry<String, ReferenceModel>> picks = stored.getReferences().stream().filter(Objects::nonNull).map(x -> Map.entry(field.getId(), x)).toList();
            if (picks.isEmpty()) continue;
            several |= picks.size() > 1;
            List<Map.Entry<String, ReferenceModel>> merged = new ArrayList<>(picks);
            for (int i = keys.size() - 1; i >= 0; i--) {
                if (Collections.disjoint(keys.get(i), fieldKeys)) continue;
                fieldKeys.addAll(keys.remove(i));
                merged.addAll(0, alternatives.remove(i));
            }
            keys.add(fieldKeys);
            alternatives.add(merged);
        }
        if (!several) return List.of(item);

        List<Map<String, ReferenceModel>> combinations = new ArrayList<>(List.of(Map.of()));
        for (List<Map.Entry<String, ReferenceModel>> alternative : alternatives) {
            List<Map<String, ReferenceModel>> next = new ArrayList<>();
            for (Map<String, ReferenceModel> combination : combinations) {
                for (Map.Entry<String, ReferenceModel> pick : alternative) {
                    Map<String, ReferenceModel> more = new LinkedHashMap<>(combination);
                    more.put(pick.getKey(), pick.getValue());
                    next.add(more);
                }
            }
            combinations = next;
        }
        Set<String> picking = new HashSet<>();
        alternatives.forEach(x -> x.forEach(y -> picking.add(y.getKey())));
        List<PropertyDefinitionFieldSetItemModel> rows = new ArrayList<>();
        for (Map<String, ReferenceModel> combination : combinations) {
            PropertyDefinitionFieldSetItemModel row = new PropertyDefinitionFieldSetItemModel();
            row.setOrdinal(item.getOrdinal());
            row.setFields(new HashMap<>(item.getFields()));
            for (String fieldId : picking) {
                FieldModel only = new FieldModel();
                only.setId(fieldId);
                only.setReferences(combination.containsKey(fieldId) ? List.of(combination.get(fieldId)) : List.of());
                row.getFields().put(fieldId, only);
            }
            rows.add(row);
        }
        return rows;
    }

    private List<PropertyDefinitionFieldSetItemModel> findFieldSetValue(FieldSetModel fieldSetModel, Map<String, PropertyDefinitionFieldSetModel> fieldSets){
        List<PropertyDefinitionFieldSetItemModel> items = new ArrayList<>();
        if (fieldSetModel == null || fieldSets == null || fieldSets.isEmpty()) return items;
        PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel =  fieldSets.getOrDefault(fieldSetModel.getId(), null);
        if (propertyDefinitionFieldSetModel != null && propertyDefinitionFieldSetModel.getItems() != null) return propertyDefinitionFieldSetModel.getItems();
        return items;
    }

    //endregion

}

