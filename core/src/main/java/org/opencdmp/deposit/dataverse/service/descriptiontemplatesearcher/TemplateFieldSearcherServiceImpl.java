package org.opencdmp.deposit.dataverse.service.descriptiontemplatesearcher;

import org.opencdmp.commonmodels.models.descriptiotemplate.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Component
public class TemplateFieldSearcherServiceImpl implements TemplateFieldSearcherService {

    @Override
    public FieldModel findFieldBySemantic(FieldSetModel fieldSetModel, String semantic){
        if (fieldSetModel == null || fieldSetModel.getFields() == null) return null;
        List<FieldModel> fieldModels = fieldSetModel.getAllField();
        if (fieldModels == null) return null;
        return fieldModels.stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(semantic)).findFirst().orElse(null);
    }
    @Override
    public List<FieldSetModel> searchFieldSetsBySemantics(DescriptionTemplateModel template, List<String> Semantics){
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

    private FieldSetModel findFieldSet(DescriptionTemplateModel template, FieldModel fieldModel){
        if (template == null || template.getDefinition() == null || template.getDefinition().getPages() == null) return null;
        for (PageModel pageModel : template.getDefinition().getPages().stream().sorted(Comparator.comparing(PageModel::getOrdinal)).toList()){
            FieldSetModel fieldSet = this.findFieldSet(pageModel, fieldModel);
            if (fieldSet != null) return fieldSet;
        }
        return null;
    }
    private FieldSetModel findFieldSet(PageModel pageModel, FieldModel fieldModel){
        if (pageModel == null || pageModel.getSections() == null) return null;
        for (SectionModel sectionModel : pageModel.getSections().stream().sorted(Comparator.comparing(SectionModel::getOrdinal)).toList()){
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
    public List<FieldModel> searchFieldsBySemantics(DescriptionTemplateModel template, String value) {
        if (template == null || template.getDefinition() == null) return new ArrayList<>();
        List<FieldModel> fieldModels = template.getDefinition().getAllField();
        if (fieldModels == null) return  new ArrayList<>();
        return fieldModels.stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(value)).toList();
        
    }
}
