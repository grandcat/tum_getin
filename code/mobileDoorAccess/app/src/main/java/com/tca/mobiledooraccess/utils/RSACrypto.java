package com.tca.mobiledooraccess.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.tca.mobiledooraccess.MainActivity;
import com.tca.mobiledooraccess.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Created by Stefan on 30.06.2015.
 */
public class RSACrypto {
    public static final String TAG = "RSACrypto";

    public static final String ALGORITHM = "RSA";
    public static final String MODE = "NONE";
    public static final String PADDING = "OAEPWithSHA256AndMGF1Padding";
    public static final String CIPHER_ID = ALGORITHM + "/" + MODE + "/" + PADDING;
    public static final int KEYSIZE = 2048;
    public static final String SEC_PROVIDER = "BC"; // Bouncy Castle by Android, might replace it
                                                    // by Spongy Castle for more functionality

    protected Context context;  //< Required for access to Android resource files.

    protected PrivateKey ownPrivKey;    //< Our private key to decryptCiphertext received messages.
    protected PublicKey targetPubKey;   //< Public key for target of the encrypted message.

    private Cipher decryptionCipher;
    private Cipher encryptionCipher;

    public RSACrypto(Context context) {
        this.context = context;

    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(
                    ALGORITHM,
                    SEC_PROVIDER);
            keyGen.initialize(KEYSIZE);
            return keyGen.generateKeyPair();

        } catch (NoSuchAlgorithmException|NoSuchProviderException e) {
            Log.e(TAG, "Unexpected error occurred during generation of key pair.");
            e.printStackTrace();
        }

        return null;
    }

    public static String serializeKey(Key userKey) {
        byte[] key = userKey.getEncoded();
        return Base64.encodeToString(key, Base64.NO_WRAP);
    }

    /**
     * Loads a PKCS#8 encoded private key file from the preferences.
     */
    public void loadPrivateKeyFromPrefs() {
        SharedPreferences prefs = context.getSharedPreferences(
                MainActivity.TUM_GETIN_PREFERENCES,
                Context.MODE_PRIVATE
        );
        // Decode
        String b64Key = prefs.getString("priv_key", "");
        byte[] privateKey = Base64.decode(b64Key, Base64.NO_WRAP);
        // Generate private key
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKey));
            initDecryption(privKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a PKCS#8 encoded private key file from a local raw resource.
     * Note: Android only supports DER formatted files.
     *
     * openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem \ -out private_key.der -nocrypt
     *
     * @param resID Resource ID to fetch the key from.
     * @throws InvalidKeyException
     */
    public void loadPrivateKeyFromResource(int resID) throws InvalidKeyException {
        InputStream inputKey = context.getResources().openRawResource(resID);

        try {
            byte[] key = ByteStreams.toByteArray(inputKey);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            // Android requires PKCS8 encoding (DER format)
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
            ownPrivKey = keyFactory.generatePrivate(keySpec);

        } catch (IOException|NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "Provided key by resource ID not supported.");
            e.printStackTrace();
            throw new InvalidKeyException(e);
        }
    }

    /**
     * Loads a X509 public key file from a local raw resource.
     * Note: Android only supports DER formatted files.
     *
     * openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der
     *
     * @param resID Resource ID to fetch the key from.
     * @throws InvalidKeyException
     */
    public void loadPublicKeyFromResource(int resID) throws InvalidKeyException {
        InputStream inputKey = context.getResources().openRawResource(resID);

        try {
            byte[] key = ByteStreams.toByteArray(inputKey);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            // Android requires PKCS8 encoding (DER format)
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
            targetPubKey = keyFactory.generatePublic(keySpec);

        } catch (IOException|NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "Provided key by resource ID not supported.");
            e.printStackTrace();
            throw new InvalidKeyException(e);
        }
        Log.d(TAG, "Loaded public key with bytes: " + targetPubKey.getEncoded().length);
    }

    public void initDecryption() {
        initDecryption(null);
    }

    public void initDecryption(PrivateKey privKey) {
        if (privKey != null) {
            this.ownPrivKey = privKey;
        }

        // Init structures for RSA decryption based on our own private key
        try {
            decryptionCipher = Cipher.getInstance(CIPHER_ID, SEC_PROVIDER);
            decryptionCipher.init(Cipher.DECRYPT_MODE, ownPrivKey, new OAEPParameterSpec("SHA-256",
                    "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            Log.d(TAG, "Decryption initialized.");
        } catch (NoSuchAlgorithmException|NoSuchProviderException|NoSuchPaddingException e) {
            // Should not be fired, because only depends on CIPHER_ID
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // Should not be fired, because only depends on static values
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.e(TAG, "No valid private key was specified.");
            decryptionCipher = null;
            e.printStackTrace();
        }
    }

    public byte[] decryptCiphertext(String base64Ciphertext) {
        return decryptCiphertext(Base64.decode(base64Ciphertext, Base64.DEFAULT));
    }

    public byte[] decryptCiphertext(byte[] ciphertext) {
        if (decryptionCipher == null) {
            // Asymmetric decryption cipher was not created correctly
            Log.e(TAG, "Decryption cipher was not initialized correctly before.");
            return null;
        }

        byte[] plaintext = new byte[0];
        try {
            plaintext = decryptionCipher.doFinal(ciphertext);
        } catch (IllegalBlockSizeException|BadPaddingException e) {
            // Probably invalid input
            e.printStackTrace();
            Log.w(TAG, "Malformed ciphertext (" + ciphertext.length + " bytes) received.");
        }

        return plaintext;
    }

    public void initEncryption() {
        initEncryption(null);
    }

    public void initEncryption(PublicKey pubKey) {
        if (pubKey != null) {
            this.targetPubKey = pubKey;
        }

        // Init cipher generator for RSA encryption
        try {
            encryptionCipher = Cipher.getInstance(CIPHER_ID, SEC_PROVIDER);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, targetPubKey, new OAEPParameterSpec("SHA-256",
                    "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        } catch (NoSuchAlgorithmException|NoSuchProviderException|NoSuchPaddingException e) {
            // Should not be fired, because only depends on CIPHER_ID
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // Should not be fired, because only depends on static values
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.e(TAG, "No valid public key was specified.");
            encryptionCipher = null;
            e.printStackTrace();
        }
    }

    public String encryptPlaintextB64(byte[] plaintext) {
        return Base64.encodeToString(encryptPlaintext(plaintext), Base64.DEFAULT);
    }

    public byte[] encryptPlaintext(byte[] plaintext) {
        if (encryptionCipher == null) {
            // Asymmetric encryption cipher was not created correctly
            Log.e(TAG, "Encryption cipher was not initialized correctly before.");
            return null;
        }

        byte[] ciphertext = new byte[0];
        try {
            ciphertext = encryptionCipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException|BadPaddingException e) {
            // Probably plaintext input is too long
            e.printStackTrace();
            Log.w(TAG, "Malformed plaintext (" + ciphertext.length + " bytes) received.");
        }

        return ciphertext;
    }
}
