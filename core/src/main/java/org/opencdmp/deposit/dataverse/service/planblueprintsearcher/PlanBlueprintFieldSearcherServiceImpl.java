package org.opencdmp.deposit.dataverse.service.planblueprintsearcher;

import org.opencdmp.commonmodels.models.planblueprint.PlanBlueprintModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.FieldSetModel;
import org.opencdmp.commonmodels.models.planblueprintdescriptiontemplatefields.SectionModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Component
public class PlanBlueprintFieldSearcherServiceImpl implements PlanBlueprintFieldSearcherService {

    @Override
    public FieldModel findFieldBySemantic(FieldSetModel fieldSetModel, String semantic){
        if (fieldSetModel == null || fieldSetModel.getFields() == null) return null;
        List<FieldModel> fieldModels = fieldSetModel.getAllField();
        if (fieldModels == null) return null;
        return fieldModels.stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(semantic)).findFirst().orElse(null);
    }
    @Override
    public List<FieldSetModel> searchFieldSetsBySemantics(PlanBlueprintModel template, List<String> Semantics){
        if (template == null || template.getDefinition() == null) return new ArrayList<>();
        List<FieldModel> fieldModels = template.getDefinition().getAllField();
        if (fieldModels == null) return new ArrayList<>();
        fieldModels = fieldModels.stream().filter(x-> x.getSemantics() != null && x.getSemantics().stream().anyMatch(Semantics::contains)).toList();
        List<FieldSetModel> response = new ArrayList<>();
        HashSet<String> fieldSetIds = new HashSet<>();
        for (FieldModel fieldModel : fieldModels){
            FieldSetModel fieldSetModel = this.findFieldSet(template, fieldModel);
            if (fieldSetIds.contains(fieldSetModel.getId())) continue;
            fieldSetIds.add(fieldSetModel.getId());
            response.add(fieldSetModel);
        }
        return response;
    }

    private FieldSetModel findFieldSet(PlanBlueprintModel blueprintModel, FieldModel fieldModel){
        if (blueprintModel == null || blueprintModel.getDefinition() == null || blueprintModel.getDefinition().getSections() == null) return null;
        for (SectionModel sectionModel : blueprintModel.getDefinition().getSections().stream().sorted(Comparator.comparing(SectionModel::getOrdinal)).toList()){
            FieldSetModel fieldSet = this.findFieldSet(sectionModel, fieldModel);
            if (fieldSet != null) return fieldSet;
        }
        return null;
    }

    private FieldSetModel findFieldSet(SectionModel sectionModel, FieldModel fieldModel){
        if (sectionModel == null) return null;
        if (sectionModel.getSections() != null) {
            for (SectionModel innerSectionModel : sectionModel.getSections().stream().sorted(Comparator.comparing(SectionModel::getOrdinal)).toList()) {
                FieldSetModel fieldSet = this.findFieldSet(innerSectionModel, fieldModel);
                if (fieldSet != null) return fieldSet;
            }
        }
        if (sectionModel.getFieldSets() != null) {
            for (FieldSetModel fieldSetModel : sectionModel.getFieldSets().stream().sorted(Comparator.comparing(FieldSetModel::getOrdinal)).toList()) {
                FieldSetModel fieldSet = this.findFieldSet(fieldSetModel, fieldModel);
                if (fieldSet != null) return fieldSet;

            }
        }
        return null;
    }

    private FieldSetModel findFieldSet(FieldSetModel fieldSetModel, FieldModel fieldModel){
        if (fieldSetModel == null || fieldSetModel.getFields() == null) return null;
        for (FieldModel currentFieldModel : fieldSetModel.getFields().stream().sorted(Comparator.comparing(FieldModel::getOrdinal)).toList()){
            if (currentFieldModel.getId().equals(fieldModel.getId())) return fieldSetModel;
        }
        return null;
    }
    
    @Override
    public List<FieldModel> searchFieldsBySemantics(PlanBlueprintModel template, String value) {
        if (template == null || template.getDefinition() == null) return new ArrayList<>();
        List<FieldModel> fieldModels = template.getDefinition().getAllField();
        if (fieldModels == null) return  new ArrayList<>();
        return fieldModels.stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(value)).toList();
        
    }
}
