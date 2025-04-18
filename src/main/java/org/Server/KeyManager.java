package org.Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages encryption keys for the server
 * Handles key generation, storage, and retrieval
 */
public class KeyManager {
    private static final String KEY_FOLDER = "keys";
    private static final String SERVER_KEYS_FILE = "server_keys.dat";
    private static final String CLIENT_KEYS_FILE = "client_keys.dat";
    
    private KeyPair serverKeyPair;
    private Map<String, PublicKey> clientPublicKeys;
    
    private static KeyManager instance;
    
    /**
     * Get singleton instance of KeyManager
     */
    public static synchronized KeyManager getInstance() {
        if (instance == null) {
            instance = new KeyManager();
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private KeyManager() {
        clientPublicKeys = new HashMap<>();
        initialize();
    }
    
    /**
     * Initialize the key manager
     * Generate or load keys as needed
     */
    private void initialize() {
        try {
            // Ensure key directory exists
            File keyDir = new File(KEY_FOLDER);
            if (!keyDir.exists()) {
                keyDir.mkdir();
                EncryptionLogger.logOperation("KeyManager", 0, false);
                System.out.println("Created key directory: " + keyDir.getAbsolutePath());
            }
            
            // Load or create server keys
            File serverKeysFile = new File(KEY_FOLDER + File.separator + SERVER_KEYS_FILE);
            if (serverKeysFile.exists()) {
                loadServerKeys();
            } else {
                generateServerKeys();
                saveServerKeys();
            }
            
            // Load client keys if available
            File clientKeysFile = new File(KEY_FOLDER + File.separator + CLIENT_KEYS_FILE);
            if (clientKeysFile.exists()) {
                loadClientKeys();
            }
            
        } catch (Exception e) {
            EncryptionLogger.logError("KeyManager initialization", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate new RSA keys for the server
     */
    private void generateServerKeys() throws NoSuchAlgorithmException {
        serverKeyPair = CryptoUtils.generateRSAKeyPair();
        EncryptionLogger.logKeyGeneration("RSA server", 2048);
    }
    
    /**
     * Save server keys to file
     */
    private void saveServerKeys() throws IOException {
        String publicKeyStr = CryptoUtils.keyToString(serverKeyPair.getPublic());
        String privateKeyStr = CryptoUtils.keyToString(serverKeyPair.getPrivate());
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(KEY_FOLDER + File.separator + SERVER_KEYS_FILE))) {
            writer.println(publicKeyStr);
            writer.println(privateKeyStr);
        }
        
        System.out.println("Server keys saved to file");
    }
    
    /**
     * Load server keys from file
     */
    private void loadServerKeys() throws Exception {
        String[] keyLines = Files.readAllLines(Paths.get(KEY_FOLDER + File.separator + SERVER_KEYS_FILE)).toArray(new String[0]);
        
        if (keyLines.length >= 2) {
            String publicKeyStr = keyLines[0];
            String privateKeyStr = keyLines[1];
            
            PublicKey publicKey = CryptoUtils.stringToPublicKey(publicKeyStr);
            PrivateKey privateKey = CryptoUtils.stringToPrivateKey(privateKeyStr);
            
            serverKeyPair = new KeyPair(publicKey, privateKey);
            System.out.println("Server keys loaded from file");
        } else {
            throw new IOException("Server key file format is invalid");
        }
    }
    
    /**
     * Save client public keys to file
     */
    private void saveClientKeys() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(KEY_FOLDER + File.separator + CLIENT_KEYS_FILE))) {
            oos.writeObject(clientPublicKeys);
        }
        
        System.out.println("Client keys saved to file");
    }
    
    /**
     * Load client public keys from file
     */
    @SuppressWarnings("unchecked")
    private void loadClientKeys() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(KEY_FOLDER + File.separator + CLIENT_KEYS_FILE))) {
            clientPublicKeys = (Map<String, PublicKey>) ois.readObject();
        }
        
        System.out.println("Client keys loaded from file");
    }
    
    /**
     * Register a client public key
     * @param clientId Client identifier
     * @param publicKey Client's public key
     */
    public void registerClientKey(String clientId, PublicKey publicKey) {
        clientPublicKeys.put(clientId, publicKey);
        try {
            saveClientKeys();
            System.out.println("Registered client key: " + clientId);
        } catch (IOException e) {
            EncryptionLogger.logError("Client key registration", e.getMessage());
        }
    }
    
    /**
     * Get client public key
     * @param clientId Client identifier
     * @return Client's public key or null if not found
     */
    public PublicKey getClientPublicKey(String clientId) {
        return clientPublicKeys.get(clientId);
    }
    
    /**
     * Get server public key
     * @return Server's public key
     */
    public PublicKey getServerPublicKey() {
        return serverKeyPair.getPublic();
    }
    
    /**
     * Get server private key
     * @return Server's private key
     */
    public PrivateKey getServerPrivateKey() {
        return serverKeyPair.getPrivate();
    }
    
    /**
     * Get server public key as Base64 string
     * @return Server's public key as string
     */
    public String getServerPublicKeyString() {
        return CryptoUtils.keyToString(serverKeyPair.getPublic());
    }
} 