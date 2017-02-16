/*
 * Copyright (c) 2017 LDLN
 *
 * This file is part of LDLN's Responder for Android.
 *
 * Responder for Android is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or any
 * later version.
 *
 * Responder for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LDLN Responder for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.ldln.android.sdk;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import tgio.rncryptor.RNCryptorNative;

/**
 * A helper class to help with any encryption/decryption that needs
 * to occur in the LDLN SDK.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.002
 * @since 2015-10-25
 */
class EncryptionHelper 
{
	enum EncodingType {
		HEX,
		BASE64
	}
	
	static class AES
	{
		static byte[] decrypt(String ciphertext, String key, EncodingType encodingType) throws UnsupportedEncodingException {
				// Set up cipher algo
				RNCryptorNative c = new RNCryptorNative();

		        // Decode ciphertext into byte array
		        byte[] ciphertextBytes;
		        switch (encodingType) {
		        case HEX:
					ciphertextBytes = EncodingHelper.hexStringToByteArray(ciphertext);
		        	break;
		        case BASE64:
					ciphertextBytes = Base64.decode(ciphertext, Base64.DEFAULT);
		        	break;
		        default: // Base64 by default
					ciphertextBytes = Base64.decode(ciphertext, Base64.DEFAULT);
		        }

		        // Decrypt
		        byte[] plaintextBytes = c.decrypt(Base64.encodeToString(ciphertextBytes, Base64.DEFAULT), formatKey(key)).getBytes();
		        
		        // Return result
		        return plaintextBytes;
		}

		static String encrypt(String plaintext, String key, EncodingType encodingType) throws UnsupportedEncodingException {
			// Set up cipher algo
			RNCryptorNative c = new RNCryptorNative();
			
			// Encode the plaintext
			byte[] plaintextBytes = Base64.encode(plaintext.getBytes("utf-8"), Base64.DEFAULT);

			// Encrypt
			byte[] ciphertextBytes = c.encrypt(new String(plaintextBytes), formatKey(key));

			String ret;
			switch (encodingType) {
	        case HEX:
	        	ret = EncodingHelper.bytesToHex(ciphertextBytes);
	        	ret = ret.toLowerCase();
	        	break;
	        case BASE64:
	        	ret = Base64.encodeToString(ciphertextBytes, Base64.DEFAULT);
	        	break;
	        default: // Base64 by default
	        	ret = Base64.encodeToString(ciphertextBytes, Base64.DEFAULT);
	        }
			
			return ret;
		}
	}

	private static String formatKey(String key) throws UnsupportedEncodingException {
		int keyLengthInBytes = 32;
		byte[] bytes = new byte[keyLengthInBytes];
		bytes = Arrays.copyOf(key.getBytes("utf-8"), keyLengthInBytes);
		return new String(bytes);
	}

	static class EncodingHelper
	{
		final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
		
		static byte[] hexStringToByteArray(String s) {
		    int len = s.length();
		    byte[] data = new byte[len / 2];
		    for (int i = 0; i < len; i += 2) {
		        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
		                             + Character.digit(s.charAt(i+1), 16));
		    }
		    return data;
		}
		
		static String bytesToHex(byte[] bytes) {
		    char[] hexChars = new char[bytes.length * 2];
		    for ( int j = 0; j < bytes.length; j++ ) {
		        int v = bytes[j] & 0xFF;
		        hexChars[j * 2] = hexArray[v >>> 4];
		        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		    }
		    return new String(hexChars);
		}
	}
}
