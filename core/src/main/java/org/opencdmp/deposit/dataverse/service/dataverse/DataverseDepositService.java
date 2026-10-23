package org.opencdmp.deposit.dataverse.service.dataverse;

import org.opencdmp.commonmodels.models.deposit.DepositPreviewModel;
import org.opencdmp.commonmodels.models.deposit.PreviewFieldOption;
import org.opencdmp.depositbase.repository.DepositAccount;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;

import java.util.List;

public interface DataverseDepositService {
	String deposit(PlanDepositModel planDepositModel);

	DepositConfiguration getConfiguration();

	DepositAccount account(PlanDepositModel planDepositModel);

	String authenticate(String code);

	String getLogo();

	DepositPreviewModel preview(PlanDepositModel PlanDepositModel);

	List<PreviewFieldOption> searchVocabulary(String fieldCode, String like);
}
