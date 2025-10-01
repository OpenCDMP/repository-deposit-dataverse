package org.opencdmp.deposit.dataverse.service.dataverse;

import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;

public interface DataverseDepositService {
	String deposit(PlanDepositModel planDepositModel) throws Exception;

	DepositConfiguration getConfiguration();

	String authenticate(String code);

	String getLogo();
}
