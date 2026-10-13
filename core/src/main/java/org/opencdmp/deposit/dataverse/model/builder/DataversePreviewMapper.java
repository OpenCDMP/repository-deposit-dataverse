package org.opencdmp.deposit.dataverse.model.builder;

import org.jspecify.annotations.Nullable;
import org.opencdmp.commonmodels.enums.PreviewFieldSource;
import org.opencdmp.commonmodels.enums.PreviewFieldType;
import org.opencdmp.commonmodels.models.deposit.*;
import org.opencdmp.deposit.dataverse.model.Citation;
import org.opencdmp.deposit.dataverse.model.CitationField;
import org.opencdmp.deposit.dataverse.model.DataverseDataset;
import org.opencdmp.deposit.dataverse.model.DataverseFieldSchema;
import org.opencdmp.deposit.dataverse.service.dataverse.DataverseDepositServiceImpl;
import org.opencdmp.depositbase.repository.DepositPluginSettings;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DataversePreviewMapper {

    private static final String FIELD_TYPE_CLASS_PRIMITIVE = "primitive";
    private static final String FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY = "controlledVocabulary";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private static final String FIELD_TITLE = "title";
    private static final String FIELD_SUBTITLE = "subtitle";
    private static final String FIELD_NOTES = "notesText";
    private static final String FIELD_ALTERNATIVE_URL = "alternativeURL";
    private static final String FIELD_DATE_OF_DEPOSIT = "dateOfDeposit";
    private static final String FIELD_PUBLICATION_URL = "publicationURL";
    private static final String FIELD_AUTHOR_AFFILIATION = "authorAffiliation";
    private static final String FIELD_AUTHOR_NAME = "authorName";
    private static final String FIELD_DESCRIPTION_VALUE = "dsDescriptionValue";
    public static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_DATASET_CONTACT_EMAIL = "datasetContactEmail";
    private static final String FIELD_DATASET_CONTACT_AFFILIATION = "datasetContactAffiliation";
    private static final String FIELD_DATASET_CONTACT_NAME = "datasetContactName";
    public static final String FIELD_PUBLICATION_RELATION_TYPE = "publicationRelationType";
    private static final String FIELD_PUBLICATION_CITATION = "publicationCitation";
    private static final String FIELD_PUBLICATION_ID_NUMBER = "publicationIDNumber";
    private static final String FIELD_CONTRIBUTOR_TYPE = "contributorType";
    private static final String FIELD_CONTRIBUTOR_NAME = "contributorName";
    private static final String FIELD_FUNDING_AGENCY = "grantNumberAgency";
    private static final String FIELD_FUNDING_AGENCY_GRANT_NUMBER = "grantNumberValue";
    public static final String FIELD_PUBLICATION_ID_TYPE = "publicationIDType";
    private static final String FIELD_KEYWORD_TERM_URI = "keywordTermURI";
    private static final String FIELD_KEYWORD_VOCABULARY = "keywordVocabulary";
    private static final String FIELD_KEYWORD_VOCABULARY_URI = "keywordVocabularyURI";
    private static final String FIELD_KEYWORD_VALUE = "keywordValue";
    private static final String FIELD_OTHER_IDENTIFIER_VALUE = "otherIdValue";
    private static final String FIELD_OTHER_IDENTIFIER_AGENCY = "otherIdAgency";

    public static final String FIELD_FILES = "files";

    public static final String GROUP_FILES_CODE = "files";
    public static final String GROUP_BASIC_INFORMATION = "basic-information";
    public static final String GROUP_AUTHORS = "authors";
    public static final String GROUP_DESCRIPTIONS = "descriptions";
    public static final String GROUP_SUBJECT = "subjects";
    public static final String GROUP_CONTACTS = "contacts";
    public static final String GROUP_CONTRIBUTOR = "contributors";
    public static final String GROUP_FUNDING = "funding";
    public static final String GROUP_PUBLICATIONS = "publications";
    public static final String GROUP_KEYWORDS = "keywords";
    public static final String GROUP_IDENTIFIERS = "identifiers";

    private static final List<PreviewFieldOption> authorOptions = List.of(
            new PreviewFieldOption("Person", "personal"),
            new PreviewFieldOption("Organization", "organizational"));


    public DepositPreviewModel toPreview(DataverseDataset dataverseDataset ,  Map<String, DataverseFieldSchema> dataverseFieldSchemaMap, List<String> uploadFileNames, DepositPluginSettings settings){
        DepositPreviewModel model = new DepositPreviewModel();

        model.setGroups(new ArrayList<>());
        if (uploadFileNames != null && !uploadFileNames.isEmpty()) model.getGroups().add(this.buildFilesGroup(uploadFileNames));
        model.getGroups().add(buildBasicInformationGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildAuthorGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildDescriptionGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildSubjectGroup(dataverseDataset,dataverseFieldSchemaMap, settings.isLocked(DataverseDepositServiceImpl.SETTING_SUBJECT)));
        model.getGroups().add(buildDatasetContact(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildContributorGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildFundingGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildPublicationGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildKeywordGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().add(buildOtherIdentifierGroup(dataverseDataset,dataverseFieldSchemaMap));
        model.getGroups().removeIf(group -> !GROUP_FILES_CODE.equals(group.getCode()) && settings.isHidden(group.getCode()));
        return model;
    }

    private PreviewFieldValue resolvePreviewValue(List<CitationField> citationFields,String typeName)
    {
        CitationField citationField = citationFields != null && !citationFields.isEmpty() ? citationFields.stream().filter(x->x.getTypeName().equals(typeName)).findFirst().orElse(null) : null;
        return this.textValue(typeName, citationField != null ? (String) citationField.getValue(): null);
    };

    private PreviewFieldValue resolvePreviewDateValue(List<CitationField> citationFields,String typeName)
    {
        CitationField citationField = citationFields != null && !citationFields.isEmpty() ? citationFields.stream().filter(x->x.getTypeName().equals(typeName)).findFirst().orElse(null) : null;
        return this.dateValue(typeName, citationField != null ? (String) citationField.getValue(): null);
    };

    private PreviewFieldGroup buildBasicInformationGroup(DataverseDataset dataverseDataset, Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_BASIC_INFORMATION, "Basic information");
        List<PreviewField> fields = group.getFields();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        PreviewField title = this.buildField(FIELD_TITLE, "Title", PreviewFieldType.Text, PreviewFieldSource.Mapped, this.isFieldRequired(schema, FIELD_TITLE), () -> resolvePreviewValue(citationFields, FIELD_TITLE));
        fields.add(title);
        fields.add(this.buildField(FIELD_ALTERNATIVE_URL,"Alternative URL", PreviewFieldType.Text, PreviewFieldSource.Mapped,false,() -> resolvePreviewValue(citationFields,FIELD_ALTERNATIVE_URL)));
        fields.add(this.buildField(FIELD_DATE_OF_DEPOSIT,"Deposit Date", PreviewFieldType.Date, PreviewFieldSource.Mapped,false,() -> resolvePreviewDateValue(citationFields,FIELD_DATE_OF_DEPOSIT)));
        fields.add(this.buildField(FIELD_SUBTITLE,"Subtitle", PreviewFieldType.Text, PreviewFieldSource.Mapped,false,() -> resolvePreviewValue(citationFields,FIELD_SUBTITLE)));
        fields.add(this.buildField(FIELD_NOTES,"Notes", PreviewFieldType.TextArea, PreviewFieldSource.Mapped,false,() -> resolvePreviewValue(citationFields,FIELD_NOTES)));
        return group;
    }

    private PreviewFieldGroup buildAuthorGroup(DataverseDataset dataverseDataset,Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_AUTHORS, "Author");
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "author"));
        List<PreviewField> fields = group.getFields();
        PreviewField previewField = this.buildTemplateField(FIELD_AUTHOR_NAME, "Name", PreviewFieldType.Text);
        previewField.setRequired(this.isFieldRequired(schema, "author", FIELD_AUTHOR_NAME));
        fields.add(previewField);
        fields.add(this.buildTemplateField(FIELD_AUTHOR_AFFILIATION, "Affiliation", PreviewFieldType.Text)); // dropdown
       PreviewField authorIdentifierSchemeField = this.buildTemplateField("authorIdentifierScheme","Type",PreviewFieldType.Select);
        Object authorIdentifierSchemeValues = getSchemaVocabularyValues(schema,"author","authorIdentifierScheme");
        List<String> authorIdentifierValues =  authorIdentifierSchemeValues instanceof List ? (List<String>) authorIdentifierSchemeValues : List.of();
        authorIdentifierSchemeField.setOptions(toOptions(authorIdentifierValues));
        fields.add(authorIdentifierSchemeField);
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField author = citationFields.stream().filter(x->x.getTypeName().equals("author")).findFirst().orElse(null); //List<DataverseAuthors>
        if (author != null && author.getValue() != null){
            List<Map<String, CitationField>> citations = new ArrayList<>();
            if (author.getValue() instanceof List<?> rawList) {
                citations = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> citation: citations) {
                if (citation == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_AUTHOR_NAME, citation.get(FIELD_AUTHOR_NAME) != null ? (String) citation.get(FIELD_AUTHOR_NAME).getValue() : null));
                values.add(this.textValue(FIELD_AUTHOR_AFFILIATION, citation.get(FIELD_AUTHOR_AFFILIATION) != null ? (String) citation.get(FIELD_AUTHOR_AFFILIATION).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildDescriptionGroup(DataverseDataset dataverseDataset, Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_DESCRIPTIONS, "Description");
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "dsDescription"));
        List<PreviewField> fields = group.getFields();
        PreviewField previewField = this.buildTemplateField(FIELD_DESCRIPTION_VALUE, "Text", PreviewFieldType.TextArea);
        previewField.setRequired(this.isFieldRequired(schema, "dsDescription", FIELD_DESCRIPTION_VALUE));
        fields.add(previewField);
        // fields.add(this.buildTemplateField("","Date",PreviewFieldType.Date)); // TODO: Pending Implementation on Builder
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField description = citationFields.stream().filter(x->x.getTypeName().equals("dsDescription")).findFirst().orElse(null);
        if (description != null && description.getValue() != null){
            List<Map<String, CitationField>> citations = new ArrayList<>();
            if (description.getValue() instanceof List<?> rawList) {
                citations = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> citation: citations) {
                if (citation == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_DESCRIPTION_VALUE, citation.get(FIELD_DESCRIPTION_VALUE) != null ? (String) citation.get(FIELD_DESCRIPTION_VALUE).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    }

    private PreviewFieldGroup buildSubjectGroup(DataverseDataset dataverseDataset,Map<String, DataverseFieldSchema> schema, boolean locked) {
        PreviewFieldGroup group = this.buildGroup(GROUP_SUBJECT, "Subject");
        group.setRequired(this.isFieldRequired(schema, "subject"));
        List<PreviewField> fields = group.getFields();
        PreviewField subjectPreviewField = this.buildField(FIELD_SUBJECT, "Subject", PreviewFieldType.Tags, locked ? PreviewFieldSource.System : PreviewFieldSource.Mapped,true,()->{
            List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
            CitationField subjectField = citationFields.stream().filter(x->x.getTypeName().equals(FIELD_SUBJECT)).findFirst().orElse(null);
            if (subjectField != null && subjectField.getValue() != null) {
                List<String> stringValues = subjectField.getValue() instanceof List<?> ? (List<String>) subjectField.getValue() : null;
                return this.textListValue(FIELD_SUBJECT, stringValues);
            }
            return null;
        });
        Object subjectVocabularyObject = getSchemaVocabularyValues(schema,"subject");
        List<String> subjectVocabularyObjectValues =  subjectVocabularyObject instanceof List ? (List<String>) subjectVocabularyObject : List.of();
        subjectPreviewField.setVocabSearch(true);
        subjectPreviewField.setOptions(toOptions(subjectVocabularyObjectValues));
        subjectPreviewField.setRequired(this.isFieldRequired(schema, "subject"));
        fields.add(subjectPreviewField);
        return group;
    }


    private PreviewFieldGroup buildDatasetContact(DataverseDataset dataverseDataset,Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_CONTACTS, "Point of Contact"); // Point of Contact
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "datasetContact"));
        List<PreviewField> fields = group.getFields();
        PreviewField datasetContactEmail = this.buildTemplateField(FIELD_DATASET_CONTACT_EMAIL, "E-mail", PreviewFieldType.Text);
        datasetContactEmail.setRequired(this.isFieldRequired(schema, "datasetContact", FIELD_DATASET_CONTACT_EMAIL));
        fields.add(datasetContactEmail);
        fields.add(this.buildTemplateField(FIELD_DATASET_CONTACT_NAME, "Name", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(FIELD_DATASET_CONTACT_AFFILIATION, "Affiliation", PreviewFieldType.Text)); // dropdown
        // person or organization field
       /* PreviewField datasetContactField = this.buildTemplateField("datasetContactType","Type",PreviewFieldType.Select); // TODO: find dataverse codeName of this field ?? couldnt find only those fields are on schema
        datasetContactField.setOptions(authorOptions);
        fields.add(datasetContactField);*/
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField contactFields = citationFields.stream().filter(x->x.getTypeName().equals("datasetContact")).findFirst().orElse(null);
        if (contactFields != null && contactFields.getValue() != null){
            List<Map<String, CitationField>> remappedContacts = new ArrayList<>();
            if (contactFields.getValue() instanceof List<?> rawList) {
                remappedContacts = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> contact: remappedContacts) {
                if (contact == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_DATASET_CONTACT_EMAIL, contact.get(FIELD_DATASET_CONTACT_EMAIL) != null ? (String) contact.get(FIELD_DATASET_CONTACT_EMAIL).getValue() : null));
                values.add(this.textValue(FIELD_DATASET_CONTACT_NAME, contact.get(FIELD_DATASET_CONTACT_NAME) != null ? (String) contact.get(FIELD_DATASET_CONTACT_NAME).getValue() : null));
                values.add(this.textValue(FIELD_DATASET_CONTACT_AFFILIATION, contact.get(FIELD_DATASET_CONTACT_AFFILIATION) != null ? (String) contact.get(FIELD_DATASET_CONTACT_AFFILIATION).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    };

    private PreviewFieldGroup buildContributorGroup(DataverseDataset dataverseDataset,Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_CONTRIBUTOR, "Contributors");
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "contributor"));
        List<PreviewField> fields = group.getFields();
        PreviewField previewContributorTypeField = this.buildTemplateField(FIELD_CONTRIBUTOR_TYPE, "Type", PreviewFieldType.Select);
        Object contributorTypeValues = getSchemaVocabularyValues(schema,"contributor",FIELD_CONTRIBUTOR_TYPE);
        List<String> contributorTypes =  contributorTypeValues instanceof List ? (List<String>) contributorTypeValues : List.of();
        previewContributorTypeField.setOptions(toOptions(contributorTypes));
        fields.add(previewContributorTypeField);
        fields.add(this.buildTemplateField(FIELD_CONTRIBUTOR_NAME, "Name", PreviewFieldType.Text));
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField contactFields = citationFields.stream().filter(x->x.getTypeName().equals("contributor")).findFirst().orElse(null);
        if (contactFields != null && contactFields.getValue() != null){
            List<Map<String, CitationField>> remappedContacts = new ArrayList<>();
            if (contactFields.getValue() instanceof List<?> rawList) {
                remappedContacts = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> contact: remappedContacts) {
                if (contact == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_CONTRIBUTOR_TYPE, contact.get(FIELD_CONTRIBUTOR_TYPE) != null ? (String) contact.get(FIELD_CONTRIBUTOR_TYPE).getValue() : null));
                values.add(this.textValue(FIELD_CONTRIBUTOR_NAME, contact.get(FIELD_CONTRIBUTOR_NAME) != null ? (String) contact.get(FIELD_CONTRIBUTOR_NAME).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    };

    private PreviewFieldGroup buildFundingGroup(DataverseDataset dataverseDataset, Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_FUNDING, "Funding");
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "grantNumber"));
        List<PreviewField> fields = group.getFields();
        fields.add(this.buildTemplateField(FIELD_FUNDING_AGENCY, "Agency", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(FIELD_FUNDING_AGENCY_GRANT_NUMBER, "Agency Grant Number", PreviewFieldType.Text));
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField contactFields = citationFields.stream().filter(x->x.getTypeName().equals("grantNumber")).findFirst().orElse(null);
        if (contactFields != null && contactFields.getValue() != null){
            List<Map<String, CitationField>> remappedContacts = new ArrayList<>();
            if (contactFields.getValue() instanceof List<?> rawList) {
                remappedContacts = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> contact: remappedContacts) {
                if (contact == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_FUNDING_AGENCY, contact.get(FIELD_FUNDING_AGENCY) != null ? (String) contact.get(FIELD_FUNDING_AGENCY).getValue() : null));
                values.add(this.textValue(FIELD_FUNDING_AGENCY_GRANT_NUMBER, contact.get(FIELD_FUNDING_AGENCY_GRANT_NUMBER) != null ? (String) contact.get(FIELD_FUNDING_AGENCY_GRANT_NUMBER).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    };
// return doi -> null
    private PreviewFieldGroup buildPublicationGroup(DataverseDataset dataverseDataset,Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_PUBLICATIONS, "Related Publication");
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "publication"));
        List<PreviewField> fields = group.getFields();
        PreviewField relationTypeField = this.buildTemplateField(FIELD_PUBLICATION_RELATION_TYPE,"Relation Type",PreviewFieldType.Select);
        relationTypeField.setVocabSearch(true);
        Object publicationRelationTypeValues = getSchemaVocabularyValues(schema,"publication",FIELD_PUBLICATION_RELATION_TYPE);
        List<String> relationTypeValues =  publicationRelationTypeValues instanceof List ? (List<String>) publicationRelationTypeValues : List.of();
        relationTypeField.setOptions(toOptions(relationTypeValues));
        fields.add(relationTypeField);
        fields.add(this.buildTemplateField(FIELD_PUBLICATION_CITATION, "Citation", PreviewFieldType.TextArea));
        PreviewField relationIDType = this.buildTemplateField(FIELD_PUBLICATION_ID_TYPE,"Identifier Type",PreviewFieldType.Select);
        relationIDType.setVocabSearch(true);
        Object publicationIDTypeValues = getSchemaVocabularyValues(schema,"publication",FIELD_PUBLICATION_ID_TYPE);
        List<String> relationIDTypeValues =  publicationIDTypeValues instanceof List ? (List<String>) publicationIDTypeValues : List.of();
        relationIDType.setOptions(toOptions(relationIDTypeValues));
        fields.add(relationIDType);
        fields.add(this.buildTemplateField(FIELD_PUBLICATION_ID_NUMBER, "Identifier",PreviewFieldType.Text));
        fields.add(this.buildTemplateField(FIELD_PUBLICATION_URL,"URL",PreviewFieldType.Text));
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField contactFields = citationFields.stream().filter(x->x.getTypeName().equals("publication")).findFirst().orElse(null);
        if (contactFields != null && contactFields.getValue() != null){
            List<Map<String, CitationField>> remappedContacts = new ArrayList<>();
            if (contactFields.getValue() instanceof List<?> rawList) {
                remappedContacts = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> contact: remappedContacts) {
                if (contact == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_PUBLICATION_CITATION, contact.get(FIELD_PUBLICATION_CITATION) != null ? (String) contact.get(FIELD_PUBLICATION_CITATION).getValue() : null));
                values.add(this.textValue(FIELD_PUBLICATION_RELATION_TYPE, contact.get(FIELD_PUBLICATION_RELATION_TYPE) != null ? (String) contact.get(FIELD_PUBLICATION_RELATION_TYPE).getValue() : null));
                values.add(this.textValue(FIELD_PUBLICATION_ID_TYPE, contact.get(FIELD_PUBLICATION_ID_TYPE) != null ? (String) contact.get(FIELD_PUBLICATION_ID_TYPE).getValue() : null));
                values.add(this.textValue(FIELD_PUBLICATION_ID_NUMBER, contact.get(FIELD_PUBLICATION_ID_NUMBER) != null ? (String) contact.get(FIELD_PUBLICATION_ID_NUMBER).getValue() : null));
                values.add(this.textValue(FIELD_PUBLICATION_URL, contact.get(FIELD_PUBLICATION_URL) != null ? (String) contact.get(FIELD_PUBLICATION_URL).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
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
     * True when the target Dataverse instance's own citation schema marks a top-level field as required.
     * Required-ness is read live from the schema (schema.get(rootName).isRequired()) rather than hardcoded,
     * since it is instance-specific like the controlled-vocabulary values.
     */
    private boolean isFieldRequired(Map<String, DataverseFieldSchema> schema, String rootName) {
        DataverseFieldSchema rootSchema = schema != null ? schema.get(rootName) : null;
        return rootSchema != null && rootSchema.isRequired();
    }

    /** Same as {@link #isFieldRequired(Map, String)}, for a child field of a compound field (e.g. "author.authorName"). */
    private boolean isFieldRequired(Map<String, DataverseFieldSchema> schema, String rootName, String fieldName) {
        DataverseFieldSchema rootSchema = schema != null ? schema.get(rootName) : null;
        if (rootSchema == null || rootSchema.getChildFields() == null) return false;
        Map<String, Object> fieldSchema = rootSchema.getChildFields().get(fieldName);
        return fieldSchema != null && Boolean.TRUE.equals(fieldSchema.get("isRequired"));
    }

    /** Turns raw controlled-vocabulary labels from the Dataverse schema (e.g. "Data Collector") into preview options. */
    private static List<PreviewFieldOption> toOptions(List<String> vocabularyLabels) {
        if (vocabularyLabels == null) return List.of();
        return vocabularyLabels.stream()
                .filter(label -> label != null && !label.isBlank())
                .map(label -> new PreviewFieldOption(label, label))
                .toList();
    }

    /**
     * Serves autocomplete options for preview fields backed by the citation schema's controlled vocabularies
     * (subject, publicationRelationType, publicationIDType) — these are small, fully-enumerable lists already
     * present on the fetched schema, so no separate live search call to Dataverse is needed; unlike Zenodo's
     * license/language/subjects fields, which are proxied to dedicated search endpoints.
     */
    public List<PreviewFieldOption> searchVocabulary(Map<String, DataverseFieldSchema> schema, String fieldCode, String like) {
        Object values = switch (fieldCode) {
            case FIELD_SUBJECT -> getSchemaVocabularyValues(schema, "subject");
            case FIELD_PUBLICATION_RELATION_TYPE -> getSchemaVocabularyValues(schema, "publication", FIELD_PUBLICATION_RELATION_TYPE);
            case FIELD_PUBLICATION_ID_TYPE -> getSchemaVocabularyValues(schema, "publication", FIELD_PUBLICATION_ID_TYPE);
            default -> null;
        };
        List<String> vocabularyLabels = values instanceof List ? (List<String>) values : List.of();
        List<PreviewFieldOption> options = toOptions(vocabularyLabels);
        if (like == null || like.isBlank()) return options;

        String term = like.trim().toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.getLabel().toLowerCase(Locale.ROOT).contains(term)).toList();
    }


    private PreviewFieldGroup buildKeywordGroup(DataverseDataset dataverseDataset, Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_KEYWORDS, "Keywords");
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "keyword"));
        List<PreviewField> fields = group.getFields();
        fields.add(this.buildTemplateField(FIELD_KEYWORD_TERM_URI, "Term URI", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(FIELD_KEYWORD_VOCABULARY, "Controlled Vocabulary", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(FIELD_KEYWORD_VOCABULARY_URI, "Controlled Vocabulary URL", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(FIELD_KEYWORD_VALUE, "Term", PreviewFieldType.Text));
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField contactFields = citationFields.stream().filter(x->x.getTypeName().equals("keyword")).findFirst().orElse(null);
        if (contactFields != null && contactFields.getValue() != null){
            List<Map<String, CitationField>> remappedContacts = new ArrayList<>();
            if (contactFields.getValue() instanceof List<?> rawList) {
                remappedContacts = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> contact: remappedContacts) {
                if (contact == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_KEYWORD_TERM_URI, contact.get(FIELD_KEYWORD_TERM_URI) != null ? (String) contact.get(FIELD_KEYWORD_TERM_URI).getValue() : null));
                values.add(this.textValue(FIELD_KEYWORD_VOCABULARY, contact.get(FIELD_KEYWORD_VOCABULARY) != null ? (String) contact.get(FIELD_KEYWORD_VOCABULARY).getValue() : null));
                values.add(this.textValue(FIELD_KEYWORD_VOCABULARY_URI, contact.get(FIELD_KEYWORD_VOCABULARY_URI) != null ? (String) contact.get(FIELD_KEYWORD_VOCABULARY_URI).getValue() : null));
                values.add(this.textValue(FIELD_KEYWORD_VALUE, contact.get(FIELD_KEYWORD_VALUE) != null ? (String) contact.get(FIELD_KEYWORD_VALUE).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    };

    private PreviewFieldGroup buildOtherIdentifierGroup(DataverseDataset dataverseDataset, Map<String, DataverseFieldSchema> schema) {
        PreviewFieldGroup group = this.buildGroup(GROUP_IDENTIFIERS, "Identifiers");
        group.setRepeatable(true);
        group.setRequired(this.isFieldRequired(schema, "otherId"));
        List<PreviewField> fields = group.getFields();
        fields.add(this.buildTemplateField(FIELD_OTHER_IDENTIFIER_VALUE, "Identifier Value", PreviewFieldType.Text));
        fields.add(this.buildTemplateField(FIELD_OTHER_IDENTIFIER_AGENCY, "Identifier Agency", PreviewFieldType.Text));
        List<PreviewGroupItemModel> items = new ArrayList<>();
        List<CitationField> citationFields = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields();
        CitationField contactFields = citationFields.stream().filter(x->x.getTypeName().equals("otherId")).findFirst().orElse(null);
        if (contactFields != null && contactFields.getValue() != null){
            List<Map<String, CitationField>> remappedContacts = new ArrayList<>();
            if (contactFields.getValue() instanceof List<?> rawList) {
                remappedContacts = rawList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (Map<?, ?>) item)
                        .filter(map -> map.keySet().stream().allMatch(k -> k instanceof String) &&
                                map.values().stream().allMatch(v -> v instanceof CitationField))
                        .map(map -> (Map<String, CitationField>) map)
                        .toList();
            }
            for (Map<String, CitationField> contact: remappedContacts) {
                if (contact == null) continue;
                List<PreviewFieldValue> values = new ArrayList<>();
                values.add(this.textValue(FIELD_OTHER_IDENTIFIER_VALUE, contact.get(FIELD_OTHER_IDENTIFIER_VALUE) != null ? (String) contact.get(FIELD_OTHER_IDENTIFIER_VALUE).getValue() : null));
                values.add(this.textValue(FIELD_OTHER_IDENTIFIER_AGENCY, contact.get(FIELD_OTHER_IDENTIFIER_AGENCY) != null ? (String) contact.get(FIELD_OTHER_IDENTIFIER_AGENCY).getValue() : null));
                PreviewGroupItemModel item = new PreviewGroupItemModel();
                item.setValues(values);
                items.add(item);
            }
        }
        group.setItems(items);
        group.setBaseItems(items);
        return group;
    };

    private PreviewFieldGroup buildFilesGroup(List<String> uploadFileNames) {
        PreviewFieldGroup group = this.buildGroup(GROUP_FILES_CODE, "Files");
        group.getFields().add(this.buildField(FIELD_FILES, "Files to be uploaded", PreviewFieldType.ReadOnly, PreviewFieldSource.System, false,
                () -> this.textListValue(FIELD_FILES, uploadFileNames)));
        return group;
    }

    public CitationField grabFieldFromDataverseList(Citation citation,String typeName)
    {
        return  citation.getFields().stream().filter(x->x.getTypeName().equals(typeName)).findFirst().orElse(null);
    }

    public void applyOverrides(DataverseDataset dataverseDataset, Map<String, DataverseFieldSchema> schema, List<PreviewFieldValue> editedFields, List<PreviewGroupValuesModel> editedGroups, DepositPluginSettings settings) {
        if (dataverseDataset == null) return;
        if (editedFields != null) {
            for (PreviewFieldValue value : editedFields) {
                if (value == null || value.getCode() == null) continue;
                if (FIELD_SUBJECT.equals(value.getCode()) && settings.isLocked(DataverseDepositServiceImpl.SETTING_SUBJECT)) continue;
                this.applyFieldOverride(dataverseDataset, value);
            }
        }
        if (editedGroups != null) {
            for (PreviewGroupValuesModel group : editedGroups) {
                if (group == null || group.getGroupCode() == null) continue;
                String groupCode = group.getGroupCode();
                Citation dataverseList = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation();
                String citationTypeName = switch (groupCode) {
                    case GROUP_AUTHORS -> "author";
                    case GROUP_DESCRIPTIONS -> "dsDescription";
                    case GROUP_CONTACTS -> "datasetContact";
                    case GROUP_CONTRIBUTOR -> "contributor";
                    case GROUP_FUNDING -> "grantNumber";
                    case GROUP_PUBLICATIONS -> "publication";
                    case GROUP_KEYWORDS -> "keyword";
                    case GROUP_IDENTIFIERS -> "otherId";
                    // unknown group codes are ignored; they may come from a newer descriptor version
                    default -> null;
                };
                if (citationTypeName == null) continue;

                CitationField citationField = grabFieldFromDataverseList(dataverseList, citationTypeName);
                if (citationField == null) continue; // the field was not added to the built dataset; nothing to override

                List<Map<String, CitationField>> constructedFields = buildFieldsFromItems(group.getItems(), schema);
                citationField.setValue(!constructedFields.isEmpty() ? constructedFields : null);
            }
        }
    }

    private List<Map<String, CitationField>> buildFieldsFromItems(List<PreviewGroupItemModel> items, Map<String, DataverseFieldSchema> schema) {
       // ArrayList<CitationField> constructedList = new ArrayList<CitationField>();
        // could be modularized || or i can just add more cases
        ArrayList<Map<String,CitationField>> constructedItems = new ArrayList<>();
        for (PreviewGroupItemModel item : items) {
            ArrayList<CitationField> itemFields = new ArrayList<>();
            for(PreviewFieldValue value : item.getValues())
            {
                String typeName = value.getCode();
                switch (typeName)
                {
                    case FIELD_AUTHOR_NAME, FIELD_DATASET_CONTACT_AFFILIATION, FIELD_AUTHOR_AFFILIATION,
                         FIELD_DESCRIPTION_VALUE, FIELD_DATASET_CONTACT_EMAIL, FIELD_DATASET_CONTACT_NAME,
                         FIELD_CONTRIBUTOR_NAME, FIELD_FUNDING_AGENCY, FIELD_FUNDING_AGENCY_GRANT_NUMBER,
                         FIELD_PUBLICATION_CITATION, FIELD_PUBLICATION_ID_NUMBER, FIELD_PUBLICATION_URL,
                         FIELD_KEYWORD_TERM_URI, FIELD_KEYWORD_VOCABULARY, FIELD_KEYWORD_VOCABULARY_URI,
                         FIELD_KEYWORD_VALUE, FIELD_OTHER_IDENTIFIER_VALUE, FIELD_OTHER_IDENTIFIER_AGENCY -> {
                        DataverseFieldSchema s = schema != null ? schema.get(typeName) : null;
                        itemFields.add(new CitationField(typeName,s != null ? s.getTypeClass() : "primitive", s != null && s.isMultiple(), value.getTextValue()));
                    }
                    case FIELD_CONTRIBUTOR_TYPE -> {
                            DataverseFieldSchema parentSchema = schema != null ? schema.get("contributor") : null;
                            Map<String, Object> contributorTypeSchema = parentSchema != null && parentSchema.getChildFields() != null
                                    ? parentSchema.getChildFields().get(FIELD_CONTRIBUTOR_TYPE)
                                    : null;
                            String typeClass = contributorTypeSchema != null && contributorTypeSchema.get("typeClass") != null
                                    ? contributorTypeSchema.get("typeClass").toString()
                                    : "controlledVocabulary";
                            boolean isMultiple = contributorTypeSchema != null && Boolean.TRUE.equals(contributorTypeSchema.get("multiple"));
                            itemFields.add(new CitationField(typeName, typeClass, isMultiple, value.getTextValue()));
                    }
                    case FIELD_PUBLICATION_RELATION_TYPE, FIELD_PUBLICATION_ID_TYPE -> {
                        DataverseFieldSchema publicationSchema = schema != null ? schema.get("publication") : null;
                        Map<String, Object> childSchema = publicationSchema != null && publicationSchema.getChildFields() != null
                                ? publicationSchema.getChildFields().get(typeName)
                                : null;
                        String typeClass = childSchema != null && childSchema.get("typeClass") != null ? childSchema.get("typeClass").toString() : "controlledVocabulary";
                        boolean isMultiple = childSchema != null && Boolean.TRUE.equals(childSchema.get("multiple"));
                        itemFields.add(new CitationField(typeName, typeClass, isMultiple, value.getTextValue()));
                    }
                    default -> {
                        // idk ignore
                    }
                }
            }
            constructedItems.add(itemFields.stream().collect(Collectors.toMap(CitationField::getTypeName, x->x)));
        }
        return constructedItems.stream().toList();
    };

    private void applyFieldOverride(DataverseDataset dataverseDataset, PreviewFieldValue value) {
        Citation dataverseList = dataverseDataset.getDatasetVersion().getMetadataBlocks().getCitation();

        switch (value.getCode()) {
            case FIELD_TITLE -> this.grabOrCreateField(dataverseList, FIELD_TITLE, FIELD_TYPE_CLASS_PRIMITIVE, false).setValue(value.getTextValue());
            case FIELD_ALTERNATIVE_URL -> this.grabOrCreateField(dataverseList, FIELD_ALTERNATIVE_URL, FIELD_TYPE_CLASS_PRIMITIVE, false).setValue(value.getTextValue());
            case FIELD_DATE_OF_DEPOSIT -> this.grabOrCreateField(dataverseList, FIELD_DATE_OF_DEPOSIT, FIELD_TYPE_CLASS_PRIMITIVE, false).setValue(value.getDateValue() != null ? DATE_FORMAT.format(value.getDateValue()) : null);
            case FIELD_SUBTITLE -> this.grabOrCreateField(dataverseList, FIELD_SUBTITLE, FIELD_TYPE_CLASS_PRIMITIVE, false).setValue(value.getTextValue());
            case FIELD_NOTES -> this.grabOrCreateField(dataverseList, FIELD_NOTES, FIELD_TYPE_CLASS_PRIMITIVE, false).setValue(value.getTextValue());
            case FIELD_SUBJECT -> this.grabOrCreateField(dataverseList, FIELD_SUBJECT, FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, true).setValue(value.getTextListValue());
            default -> {
                // read-only and unknown codes are ignored; unknown codes may come from a newer descriptor version
            }
        }
    }

    /**
     * Finds the citation field by type name, or creates and appends it if it wasn't added at build time
     * (build() only adds optional primitive fields like alternativeURL/subtitle/notesText/subject when the
     * plan already produced a value for them) — otherwise an override on an unset optional field would be
     * silently lost instead of applied.
     */
    private CitationField grabOrCreateField(Citation citation, String typeName, String typeClass, boolean multiple) {
        CitationField field = this.grabFieldFromDataverseList(citation, typeName);
        if (field != null) return field;
        field = new CitationField(typeName, typeClass, multiple, null);
        citation.getFields().add(field);
        return field;
    }

    private PreviewFieldGroup buildGroup(String code, String label) {
        PreviewFieldGroup group = new PreviewFieldGroup();
        group.setCode(code);
        group.setLabel(label);
        group.setFields(new ArrayList<>());
        return group;
    }

    private PreviewField buildField(String code, String label, PreviewFieldType type, PreviewFieldSource source, boolean required, Supplier<PreviewFieldValue> valueSupplier) {
        PreviewField field = new PreviewField();
        field.setCode(code);
        field.setLabel(label);
        field.setType(type);
        field.setSource(source);
        field.setRequired(required);
        field.setValue(valueSupplier.get());
        field.setBaseValue(valueSupplier.get());
        return field;
    }

    private PreviewFieldValue textValue(String code, String text) {
        PreviewFieldValue value = new PreviewFieldValue();
        value.setCode(code);
        value.setTextValue(text);
        return value;
    }

    private PreviewFieldValue textListValue(String code, List<String> texts) {
        PreviewFieldValue value = new PreviewFieldValue();
        value.setCode(code);
        value.setTextListValue(texts != null && !texts.isEmpty() ? texts : null);
        return value;
    }

    private PreviewFieldValue dateValue(String code, String formattedDate) {
        PreviewFieldValue value = new PreviewFieldValue();
        value.setCode(code);
        if (formattedDate != null && !formattedDate.isBlank()) {
            try {
                value.setDateValue(LocalDate.parse(formattedDate).atStartOfDay(ZoneOffset.UTC).toInstant());
            } catch (Exception e) {
                // leave the value empty if the mapped date is not in yyyy-MM-dd form
            }
        }
        return value;
    }
    private PreviewField buildTemplateField(String code, String label, PreviewFieldType type) {
        PreviewField field = new PreviewField();
        field.setCode(code);
        field.setLabel(label);
        field.setType(type);
        field.setSource(PreviewFieldSource.Mapped);
        return field;
    }
}
