package org.opencdmp.deposit.dataverse.service.descriptiontemplatesearcher;

import org.opencdmp.commonmodels.models.descriptiotemplate.DescriptionTemplateModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.FieldSetModel;

import java.util.List;

public interface TemplateFieldSearcherService {
	FieldModel findFieldBySemantic(FieldSetModel fieldSetModel, String semantic);

	List<FieldSetModel> searchFieldSetsBySemantics(DescriptionTemplateModel template, List<String> Semantics);

	List<FieldModel> searchFieldsBySemantics(DescriptionTemplateModel template, String value);
}
