package com.encryption;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.encryption.exception.DecryptException;
import com.encryption.exception.EncryptException;
import com.encryption.exception.SecretKeyInitException;
import com.servicebus.messaging.util.AppConstants;

/**
 * @author rprasad017
 * <p>AES encryption</p>
 */
public class AES
{
    
    private static SecretKeySpec secretKey ;
    private static byte[] key ;
    
    /**
     * Initialize encryption key
     * @param myKey
     * @throws SecretKeyInitException 
     */
    public static void setKey(String myKey) throws SecretKeyInitException{
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(AppConstants.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 32); // use only first 256 bit
            secretKey = new SecretKeySpec(key, "AES");
            
        } catch (NoSuchAlgorithmException e) {
            throw new SecretKeyInitException("Unable to initialize secret key", e);
        } catch (UnsupportedEncodingException e) {
        	throw new SecretKeyInitException("Unable to initialize secret key", e);
        }
    }
    
    /**
     * Encrypts byte array and encode as base 64
     * @param data
     * @return
     * @throws EncryptException 
     */
    public static byte[] encrypt(byte[] data) throws EncryptException
    {
        try
        {
        	// Create cipher instance using specified transformation
        	// A transformation always includes the name of a cryptographic algorithm (e.g., AES), 
        	// and may be followed by a feedback mode and padding scheme
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        
            // Initialize cipher to encryption mode with secret key
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
            // Encrypt data
            byte[] encryptedText = cipher.doFinal(data);
            
            // return base64 String representation of encrypted data
            return Base64.encodeBase64(encryptedText);
        }
        catch (NoSuchPaddingException e) {
            throw new EncryptException("Unable to Encrypt", e);
        } catch (NoSuchAlgorithmException e) {
        	throw new EncryptException("Unable to Encrypt", e);
		} catch (InvalidKeyException e) {
			throw new EncryptException("Unable to Encrypt", e);
		} catch (IllegalBlockSizeException e) {
			throw new EncryptException("Unable to Encrypt", e);
		} catch (BadPaddingException e) {
			throw new EncryptException("Unable to Encrypt", e);
		}
    }
    
    /**
     * Decode from base 64 and Decrypts Byte array
     * @param data
     * @return
     * @throws DecryptException 
     */
    public static byte[] decrypt(byte[] data) throws DecryptException
    {
        try
        {
        	// Create cipher instance using specified transformation
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
           
            // Initialize cipher to decrypt mode with secret key
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            // Base64 Decoding
            byte[] decodedText = Base64.decodeBase64(data);
            
            // Decrypt
            byte[] decryptedText = cipher.doFinal(decodedText);
            return decryptedText;
        }
        catch (InvalidKeyException e) {
            throw new DecryptException("Unable to decrypt", e);
        } catch (NoSuchAlgorithmException e) {
        	throw new DecryptException("Unable to decrypt", e);
		} catch (NoSuchPaddingException e) {
			throw new DecryptException("Unable to decrypt", e);
		} catch (IllegalBlockSizeException e) {
			throw new DecryptException("Unable to decrypt", e);
		} catch (BadPaddingException e) {
			throw new DecryptException("Unable to decrypt", e);
		}
    }
}