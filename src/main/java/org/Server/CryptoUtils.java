package org.Server;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utility class for hybrid encryption (AES + RSA)
 * Provides methods for key generation, encryption, and decryption
 */
public class CryptoUtils {
    private static final Logger logger = Logger.getLogger(CryptoUtils.class.getName());
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES";
    private static final int AES_KEY_SIZE = 256; // 256 bit AES key
    private static final int RSA_KEY_SIZE = 2048; // 2048 bit RSA key

    // Generate RSA key pair
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        logger.info("RSA key pair generated");
        return keyPair;
    }

    // Generate AES key
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE);
        SecretKey key = keyGenerator.generateKey();
        logger.info("AES key generated");
        return key;
    }

    // Encrypt data using AES
    public static byte[] encryptWithAES(byte[] data, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(data);
        logger.info("Data encrypted with AES: " + data.length + " bytes -> " + encryptedData.length + " bytes");
        return encryptedData;
    }

    // Decrypt data using AES
    public static byte[] decryptWithAES(byte[] encryptedData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedData = cipher.doFinal(encryptedData);
        logger.info("Data decrypted with AES: " + encryptedData.length + " bytes -> " + decryptedData.length + " bytes");
        return decryptedData;
    }

    // Encrypt AES key using RSA public key
    public static byte[] encryptAESKey(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        logger.info("AES key encrypted with RSA");
        return encryptedKey;
    }

    // Decrypt AES key using RSA private key
    public static SecretKey decryptAESKey(byte[] encryptedKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        SecretKey originalKey = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, AES_ALGORITHM);
        logger.info("AES key decrypted with RSA");
        return originalKey;
    }

    // Convert keys to String for transmission
    public static String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Convert public key from string
    public static PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    // Convert private key from string
    public static PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    // Hybrid encryption container to hold both encrypted AES key and data
    public static class EncryptedPackage {
        private final byte[] encryptedData;
        private final byte[] encryptedAESKey;

        public EncryptedPackage(byte[] encryptedData, byte[] encryptedAESKey) {
            this.encryptedData = encryptedData;
            this.encryptedAESKey = encryptedAESKey;
        }

        public byte[] getEncryptedData() {
            return encryptedData;
        }

        public byte[] getEncryptedAESKey() {
            return encryptedAESKey;
        }

        // Convert to Base64 encoded string for transmission
        public String toString() {
            String encodedData = Base64.getEncoder().encodeToString(encryptedData);
            String encodedKey = Base64.getEncoder().encodeToString(encryptedAESKey);
            return encodedKey + ":" + encodedData;
        }

        // Parse from string
        public static EncryptedPackage fromString(String packageStr) {
            String[] parts = packageStr.split(":", 2);
            byte[] encryptedKey = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedData = Base64.getDecoder().decode(parts[1]);
            return new EncryptedPackage(encryptedData, encryptedKey);
        }
    }

    // Encrypt data using hybrid encryption (AES + RSA)
    public static EncryptedPackage encryptHybrid(String data, PublicKey publicKey) throws Exception {
        SecretKey aesKey = generateAESKey();
        byte[] encryptedData = encryptWithAES(data.getBytes(), aesKey);
        byte[] encryptedAESKey = encryptAESKey(aesKey, publicKey);
        logger.info("Hybrid encryption completed successfully");
        return new EncryptedPackage(encryptedData, encryptedAESKey);
    }

    // Decrypt data using hybrid encryption (AES + RSA)
    public static String decryptHybrid(EncryptedPackage encryptedPackage, PrivateKey privateKey) throws Exception {
        SecretKey aesKey = decryptAESKey(encryptedPackage.getEncryptedAESKey(), privateKey);
        byte[] decryptedData = decryptWithAES(encryptedPackage.getEncryptedData(), aesKey);
        logger.info("Hybrid decryption completed successfully");
        return new String(decryptedData);
    }
} 