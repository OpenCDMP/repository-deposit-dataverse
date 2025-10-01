package org.opencdmp.deposit.dataverse.service.storage;

public interface FileStorageService {
	String storeFile(byte[] data);

	byte[] readFile(String fileRef);
}
