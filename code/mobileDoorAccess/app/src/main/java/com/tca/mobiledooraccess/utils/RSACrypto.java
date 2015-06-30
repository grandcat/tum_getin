package com.tca.mobiledooraccess.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.tca.mobiledooraccess.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;

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

    protected Context context;  //< Required for access to Android resource files.

    protected PrivateKey ownPrivKey;    //< Our private key to decryptCiphertext received messages.
    protected PublicKey targetPubKey;   //< Public key for target of the encrypted message.

    Cipher decryptionCipher;
    Cipher encryptionCipher;

    public RSACrypto(Context context) {
        this.context = context;

    }

    /**
     * Returns public key encoded as byte array.
     * @return privateKey.getEncoded()
     */
    public byte[] getPublicKey() {
        return targetPubKey.getEncoded();
    }

    /**
     * Loads a PKCS#8 encoded private key file from a local raw resource.
     * Note: Android only accepts DER formatted files.
     *
     * @param resID Resource ID to fetch the key from.
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

    public void initDecryption() {
        initDecryption(null);
    }

    public void initDecryption(PrivateKey privKey) {
        if (privKey != null) {
            this.ownPrivKey = privKey;
        }

        // Init structures for RSA decryption based on our own private key
        try {
            decryptionCipher = Cipher.getInstance(CIPHER_ID, "BC");
            decryptionCipher.init(Cipher.DECRYPT_MODE, ownPrivKey, new OAEPParameterSpec("SHA-256",
                    "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
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
            // Asymmetric encryption decryptionCipher could not be created correctly
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

    public void initEncryption(PublicKey pubKey) {
        if (targetPubKey != null) {
            this.targetPubKey = pubKey;
        }
    }
}
