package com.ceams.service;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public interface SealEncryptionService {

    boolean encryptAndStore(File sealImageFile, File storagePath, String password, String sealId);

    boolean encryptAndStore(InputStream imageStream, File storagePath, String password, String sealId);

    byte[] decryptSealImage(File encryptedFile, String password) throws Exception;

    byte[] decryptSealImage(String sealId, String password) throws Exception;

    boolean verifyPassword(File encryptedFile, String password);

    boolean deleteEncryptedSeal(File encryptedFile, String password);

    File getEncryptedSealFile(File storagePath, String sealId);

    Map<String, Object> getSealMetadata(File encryptedFile);

    String generateSealId();
}
