package org.opencdmp.deposit.dataverse.model.builder;

import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.fielddata.ReferenceTypeDataModel;
import org.opencdmp.commonmodels.models.reference.ReferenceFieldModel;
import org.opencdmp.commonmodels.models.reference.ReferenceModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeDefinitionModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeFieldModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The keys a reference type gives the references picked in an input: their id, name and description (the type's
 * system fields) and the type's own fields can each carry keys, so an input without keys of its own still answers them.
 */
public final class ReferenceSemantics {

    private ReferenceSemantics() {
    }

    /** Does the input's reference type give the key to a system field or to one of its own fields? */
    public static boolean carries(FieldModel field, String key) {
        return field != null && field.getData() instanceof ReferenceTypeDataModel data && keys(data.getReferenceType()).contains(key);
    }

    /** Does the input answer the key only through its reference type? Its own key wins and reads as before. */
    public static boolean throughType(FieldModel field, String key) {
        return field != null && (field.getSemantics() == null || !field.getSemantics().contains(key)) && carries(field, key);
    }

    /** The keys the reference type gives its id, name, description and fields. */
    public static Set<String> keys(ReferenceTypeModel type) {
        Set<String> keys = new HashSet<>();
        if (type == null || type.getDefinition() == null) return keys;
        ReferenceTypeDefinitionModel definition = type.getDefinition();
        if (definition.getReferenceIdSemantics() != null) keys.addAll(definition.getReferenceIdSemantics());
        if (definition.getLabelSemantics() != null) keys.addAll(definition.getLabelSemantics());
        if (definition.getDescriptionSemantics() != null) keys.addAll(definition.getDescriptionSemantics());
        if (definition.getFields() != null) definition.getFields().stream().filter(x -> x.getSemantics() != null).forEach(x -> keys.addAll(x.getSemantics()));
        return keys;
    }

    /**
     * The value a picked reference gives the key through its type: its id, name or description when the type gives
     * that system field the key, else the value of the reference's own field carrying it. Null when none does.
     */
    public static String value(ReferenceModel reference, ReferenceTypeModel type, String key) {
        if (reference == null || type == null || type.getDefinition() == null || key == null) return null;
        ReferenceTypeDefinitionModel definition = type.getDefinition();
        if (system(definition, key)) return systemValue(reference, definition, key);
        String value = null;
        if (definition.getFields() != null && reference.getDefinition() != null && reference.getDefinition().getFields() != null) {
            ReferenceTypeFieldModel typeField = definition.getFields().stream().filter(x -> x.getCode() != null && contains(x.getSemantics(), key)).findFirst().orElse(null);
            if (typeField != null) value = reference.getDefinition().getFields().stream().filter(x -> typeField.getCode().equals(x.getCode())).map(ReferenceFieldModel::getValue).findFirst().orElse(null);
        }
        return value != null && !value.isBlank() ? value : null;
    }

    /** The picked reference's id, name or description when the type gives that system field the key; null otherwise or when blank. */
    public static String systemValue(ReferenceModel reference, ReferenceTypeDefinitionModel definition, String key) {
        if (reference == null || definition == null || key == null) return null;
        String value = null;
        if (contains(definition.getReferenceIdSemantics(), key)) value = reference.getReference();
        else if (contains(definition.getLabelSemantics(), key)) value = reference.getLabel();
        else if (contains(definition.getDescriptionSemantics(), key)) value = reference.getDescription();
        return value != null && !value.isBlank() ? value : null;
    }

    private static boolean system(ReferenceTypeDefinitionModel definition, String key) {
        return contains(definition.getReferenceIdSemantics(), key) || contains(definition.getLabelSemantics(), key) || contains(definition.getDescriptionSemantics(), key);
    }

    private static boolean contains(List<String> semantics, String key) {
        return semantics != null && semantics.contains(key);
    }
}
