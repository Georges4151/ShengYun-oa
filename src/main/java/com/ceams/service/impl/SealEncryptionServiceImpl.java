package com.ceams.service.impl;

import com.ceams.service.SealEncryptionService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SealEncryptionServiceImpl implements SealEncryptionService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final String FILE_EXTENSION = ".seal";
    private static final String METADATA_SEPARATOR = "|||";
    private static final String FILE_HEADER = "SEAL_ENCRYPTED_V1";

    @Override
    public boolean encryptAndStore(File sealImageFile, File storagePath, String password, String sealId) {
        try (FileInputStream fis = new FileInputStream(sealImageFile)) {
            return encryptAndStore(fis, storagePath, password, sealId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean encryptAndStore(InputStream imageStream, File storagePath, String password, String sealId) {
        try {
            if (!storagePath.exists()) {
                storagePath.mkdirs();
            }

            File encryptedFile = getEncryptedSealFile(storagePath, sealId);

            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            SecretKey key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = imageStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] imageData = baos.toByteArray();
            byte[] encryptedData = cipher.doFinal(imageData);

            try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
                String header = FILE_HEADER + METADATA_SEPARATOR + sealId + METADATA_SEPARATOR + "\n";
                fos.write(header.getBytes(StandardCharsets.UTF_8));
                fos.write(salt);
                fos.write(iv);
                fos.write(encryptedData);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public byte[] decryptSealImage(File encryptedFile, String password) throws Exception {
        byte[] fileContent = java.nio.file.Files.readAllBytes(encryptedFile.toPath());

        String contentStr = new String(fileContent, StandardCharsets.UTF_8);
        int newlineIndex = contentStr.indexOf('\n');

        if (newlineIndex < 0 || !contentStr.startsWith(FILE_HEADER)) {
            throw new Exception("无效的电子章文件格式");
        }

        String headerLine = contentStr.substring(0, newlineIndex);
        int headerEndIndex = newlineIndex + 1;

        int saltStart = headerEndIndex;
        int ivStart = saltStart + SALT_LENGTH;
        int dataStart = ivStart + IV_LENGTH;

        if (fileContent.length < dataStart) {
            throw new Exception("文件数据不完整");
        }

        byte[] salt = new byte[SALT_LENGTH];
        System.arraycopy(fileContent, saltStart, salt, 0, SALT_LENGTH);

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(fileContent, ivStart, iv, 0, IV_LENGTH);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        int encryptedDataLength = fileContent.length - dataStart;
        byte[] encryptedData = new byte[encryptedDataLength];
        System.arraycopy(fileContent, dataStart, encryptedData, 0, encryptedDataLength);

        byte[] decryptedData = cipher.doFinal(encryptedData);

        return decryptedData;
    }

    @Override
    public byte[] decryptSealImage(String sealId, String password) throws Exception {
        return new byte[0];
    }

    @Override
    public boolean verifyPassword(File encryptedFile, String password) {
        try {
            decryptSealImage(encryptedFile, password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deleteEncryptedSeal(File encryptedFile, String password) {
        if (!verifyPassword(encryptedFile, password)) {
            return false;
        }
        return encryptedFile.delete();
    }

    @Override
    public File getEncryptedSealFile(File storagePath, String sealId) {
        return new File(storagePath, sealId + FILE_EXTENSION);
    }

    @Override
    public Map<String, Object> getSealMetadata(File encryptedFile) {
        Map<String, Object> metadata = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(encryptedFile)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            String headerLine = reader.readLine();

            if (headerLine != null && headerLine.startsWith(FILE_HEADER)) {
                String[] parts = headerLine.split(METADATA_SEPARATOR);
                if (parts.length >= 2) {
                    metadata.put("format", parts[0]);
                    metadata.put("sealId", parts[1]);
                }
            }

            metadata.put("fileSize", encryptedFile.length());
            metadata.put("lastModified", encryptedFile.lastModified());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }

    @Override
    public String generateSealId() {
        return "SEAL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }
}
