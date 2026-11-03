package org.opencdmp.deposit.dataverse.validation;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.opencdmp.deposit.dataverse.model.CitationField;
import org.opencdmp.deposit.dataverse.model.DataverseDataset;
import org.opencdmp.deposit.dataverse.model.DataverseFieldSchema;
import org.opencdmp.deposit.dataverse.model.builder.DataverseBuilder;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Checks a dataset against the installation's own citation schema before it is sent: each value by the type the
 * schema gives its field (URL, EMAIL, DATE, INT, FLOAT), controlled vocabulary values, and, when the dataset will be
 * published, the required fields. The type checks are Dataverse's own (DatasetFieldValueValidator, URLValidator,
 * EMailValidator), so a value passes here exactly when the installation accepts it.
 */
@Component
public class DataverseDatasetValidator {

    private static final String TYPE_URL = "URL";
    private static final String TYPE_EMAIL = "EMAIL";
    private static final String TYPE_DATE = "DATE";
    private static final String TYPE_INT = "INT";
    private static final String TYPE_FLOAT = "FLOAT";
    private static final String NA_VALUE = "N/A";
    private static final String DATASET_TYPE_LABEL = "Dataset type";
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https", "ftp"}, UrlValidator.ALLOW_2_SLASHES);

    /** The review's group labels of the compound fields it shows, so a problem names the group the user edits. */
    private static final Map<String, String> GROUP_LABELS = Map.of(
            "author", "Author",
            "dsDescription", "Description",
            "datasetContact", "Point of Contact",
            "contributor", "Contributors",
            "grantNumber", "Funding",
            "publication", "Related Publication",
            "keyword", "Keywords",
            "otherId", "Identifiers");

    /**
     * The problems by review label (e.g. "Point of Contact, row 2"), in dataset order; empty when the installation should accept it.
     * allowedDatasetTypes: the dataset types the target collection allows, or null to leave the type to Dataverse.
     */
    public Map<String, List<String>> validate(DataverseDataset dataset, Map<String, DataverseFieldSchema> schema, boolean checkRequired, Collection<String> allowedDatasetTypes) {
        Map<String, List<String>> problems = new LinkedHashMap<>();
        if (dataset != null && allowedDatasetTypes != null) {
            // Dataverse creates a dataset sent without a type as a "dataset", and checks that against the collection too
            String datasetType = dataset.getDatasetType() != null ? dataset.getDatasetType() : DataverseBuilder.DEFAULT_DATASET_TYPE;
            if (!allowedDatasetTypes.contains(datasetType)) {
                add(problems, DATASET_TYPE_LABEL, DATASET_TYPE_LABEL + " '" + datasetType + "' is not one the collection allows (" + String.join(", ", allowedDatasetTypes) + ").");
            }
        }
        List<CitationField> fields = dataset != null && dataset.getDatasetVersion() != null && dataset.getDatasetVersion().getMetadataBlocks() != null
                && dataset.getDatasetVersion().getMetadataBlocks().getCitation() != null ? dataset.getDatasetVersion().getMetadataBlocks().getCitation().getFields() : null;
        if (fields == null || schema == null) return problems;

        Map<String, CitationField> byName = new HashMap<>();
        for (CitationField field : fields) {
            if (field == null || field.getTypeName() == null) continue;
            byName.put(field.getTypeName(), field);
            DataverseFieldSchema fieldSchema = schema.get(field.getTypeName());
            if (fieldSchema == null) continue; // a field this installation does not describe is left to it
            String name = displayName(field.getTypeName(), fieldSchema.getDisplayName());
            if (fieldSchema.getChildFields() != null && !fieldSchema.getChildFields().isEmpty()) {
                this.validateCompound(field, fieldSchema, checkRequired, problems);
            } else {
                for (String value : texts(field.getValue())) {
                    this.validateValue(name, name, value, fieldSchema.getType(), fieldSchema.getControlledVocabularyValues(), problems);
                }
            }
        }

        if (checkRequired) {
            for (Map.Entry<String, DataverseFieldSchema> entry : schema.entrySet()) {
                if (!entry.getValue().isRequired()) continue;
                CitationField field = byName.get(entry.getKey());
                if (field == null || isEmpty(field.getValue())) {
                    String label = GROUP_LABELS.getOrDefault(entry.getKey(), displayName(entry.getKey(), entry.getValue().getDisplayName()));
                    add(problems, label, displayName(entry.getKey(), entry.getValue().getDisplayName()) + " is required to publish.");
                }
            }
        }
        return problems;
    }

    private void validateCompound(CitationField field, DataverseFieldSchema fieldSchema, boolean checkRequired, Map<String, List<String>> problems) {
        String group = GROUP_LABELS.getOrDefault(field.getTypeName(), displayName(field.getTypeName(), fieldSchema.getDisplayName()));
        List<Map<?, ?>> rows = new ArrayList<>();
        if (field.getValue() instanceof Map<?, ?> row) rows.add(row);
        else if (field.getValue() instanceof List<?> list) list.stream().filter(x -> x instanceof Map<?, ?>).forEach(x -> rows.add((Map<?, ?>) x));

        for (int i = 0; i < rows.size(); i++) {
            String label = rows.size() > 1 || fieldSchema.isMultiple() ? group + ", row " + (i + 1) : group;
            Map<?, ?> row = rows.get(i);
            for (Map.Entry<String, Map<String, Object>> child : fieldSchema.getChildFields().entrySet()) {
                Map<String, Object> childSchema = child.getValue();
                String childName = displayName(child.getKey(), childSchema != null ? (String) childSchema.get("displayName") : null);
                Object childValue = row.get(child.getKey()) instanceof CitationField citationField ? citationField.getValue() : null;
                if (childValue == null || isEmpty(childValue)) {
                    if (checkRequired && childSchema != null && Boolean.TRUE.equals(childSchema.get("isRequired"))) add(problems, label, childName + " is required.");
                    continue;
                }
                String type = childSchema != null ? (String) childSchema.get("type") : null;
                List<String> vocabulary = childSchema != null && Boolean.TRUE.equals(childSchema.get("isControlledVocabulary")) && childSchema.get("controlledVocabularyValues") instanceof List<?> values
                        ? values.stream().map(String::valueOf).toList() : null;
                for (String value : texts(childValue)) this.validateValue(label, childName, value, type, vocabulary, problems);
            }
        }
    }

    private void validateValue(String label, String name, String value, String type, List<String> vocabulary, Map<String, List<String>> problems) {
        if (value == null || value.isBlank() || NA_VALUE.equals(value)) return;
        if (vocabulary != null && !vocabulary.isEmpty() && !vocabulary.contains(value)) {
            add(problems, label, name + " '" + value + "' is not one of the values the repository allows.");
            return;
        }
        if (type == null) return;
        switch (type) {
            case TYPE_URL -> {
                if (!URL_VALIDATOR.isValid(value)) add(problems, label, name + " '" + value + "' is not a valid URL (http, https or ftp).");
            }
            case TYPE_EMAIL -> {
                if (!EmailValidator.getInstance().isValid(value)) add(problems, label, name + " '" + value + "' is not a valid e-mail address.");
            }
            case TYPE_DATE -> {
                if (!isValidDateValue(value)) add(problems, label, name + " '" + value + "' is not a valid date (YYYY-MM-DD, YYYY-MM or YYYY).");
            }
            case TYPE_INT -> {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    add(problems, label, name + " '" + value + "' is not a whole number.");
                }
            }
            case TYPE_FLOAT -> {
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    add(problems, label, name + " '" + value + "' is not a number.");
                }
            }
            default -> {
                // TEXT, TEXTBOX and NONE take any value
            }
        }
    }

    /** Dataverse's DATE rule: yyyy-MM-dd, yyyy-MM or yyyy, an AD/BC year, a bracketed "[yyyy?]" estimate, or a timestamp. */
    static boolean isValidDateValue(String value) {
        String testString = value;
        boolean valid = isValidDate(testString, "yyyy-MM-dd") || isValidDate(testString, "yyyy-MM");
        if (value.contains("AD")) testString = testString.substring(0, testString.indexOf("AD")).trim();
        if (!valid) valid = isValidDate(testString, "yyyy") && isNumeric(testString);
        if (!valid && value.contains("BC")) {
            testString = testString.substring(0, testString.indexOf("BC")).trim();
            if (isNumeric(testString)) valid = true;
        }
        if (!valid && value.startsWith("[") && value.endsWith("?]") && !value.startsWith("[-")) {
            testString = value.replace("[", " ").replace("?]", " ").replace("-", " ").replace("BC", " ").replace("AD", " ").trim();
            if (value.contains("BC") && isNumeric(testString)) valid = true;
            else valid = isValidDate(testString, "yyyy") && isNumeric(testString);
        }
        if (!valid) {
            valid = isValidDate(value, "yyyy-MM-dd'T'HH:mm:ss") || isValidDate(value, "yyyy-MM-dd'T'HH:mm:ss.SSS") || isValidDate(value, "yyyy-MM-dd HH:mm:ss");
        }
        return valid;
    }

    private static boolean isValidDate(String dateString, String pattern) {
        boolean valid = true;
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setLenient(false);
        String trimmed = dateString.trim();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(trimmed));
            if (calendar.get(Calendar.ERA) == GregorianCalendar.AD && calendar.get(Calendar.YEAR) > 9999) valid = false;
        } catch (ParseException e) {
            valid = false;
        }
        if (trimmed.length() > pattern.length()) valid = false;
        return valid;
    }

    private static boolean isNumeric(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }

    private static List<String> texts(Object value) {
        List<String> texts = new ArrayList<>();
        if (value instanceof String text) texts.add(text);
        else if (value instanceof List<?> list) list.stream().filter(x -> x instanceof String).forEach(x -> texts.add((String) x));
        return texts;
    }

    private static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String text) return text.isBlank();
        if (value instanceof Collection<?> collection) return collection.stream().allMatch(DataverseDatasetValidator::isEmpty);
        if (value instanceof Map<?, ?> map) return map.values().stream().allMatch(x -> x instanceof CitationField citationField ? isEmpty(citationField.getValue()) : isEmpty(x));
        return false;
    }

    private static String displayName(String name, String displayName) {
        return displayName != null && !displayName.isBlank() ? displayName : name;
    }

    private static void add(Map<String, List<String>> problems, String label, String message) {
        List<String> messages = problems.computeIfAbsent(label, k -> new ArrayList<>());
        if (!messages.contains(message)) messages.add(message);
    }
}
