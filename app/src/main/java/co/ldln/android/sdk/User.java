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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import co.ldln.android.sdk.EncryptionHelper.EncodingType;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * A user object, as defined by the LDLN
 * synchronization protocol.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.002
 * @since 2015-03-10
 */
public class User extends RealmObject
{
	private static final String SALT = "Yp2iD6PcTwB6upati0bPw314GrFWhUy90BIvbJTj5ETbbE8CoViDDGsJS6YHMOBq4VlwW3V00GWUMbbV";

	private String encryptedDek;
	private String encryptedRsaPrivate;
	private String hashedPassword;
	private String rsaPublic;

	@PrimaryKey
	private String username;

	/* Empty Default Constructor Required for Realm */
	public User() { }

	/* Parse the JSONObject in the form of:
		{
			"encrypted_kek": "abcdef1234567890",
			"encryptedRsaPrivate": "abcdef1234567890",
			"hashedPassword": "abcdef1234567890",
			"rsaPublic": "abcdef1234567890",
			"username": "admin"
		}
	 */
	public User(JSONObject userJsonObject) throws JSONException
	{
		setEncryptedDek(userJsonObject.getString("encrypted_kek"));
		setEncryptedRsaPrivate(userJsonObject.getString("encrypted_rsa_private"));
		setHashedPassword(userJsonObject.getString("hashed_password"));
		setRsaPublic(userJsonObject.getString("rsa_public"));
		setUsername(userJsonObject.getString("username"));
	}

	// Setters
	void setEncryptedDek(String encrypted_dek) { this.encryptedDek = encrypted_dek; }
	void setEncryptedRsaPrivate(String encrypted_rsa_private) { this.encryptedRsaPrivate = encrypted_rsa_private; }
	void setHashedPassword(String hashed_password) { this.hashedPassword = hashed_password; }
	void setRsaPublic(String rsa_public) { this.rsaPublic = rsa_public; }
	void setUsername(String username) { this.username = username; }

	// Getters
	String getEncryptedDek() { return this.encryptedDek; }
	String getEncryptedRsaPrivate() { return this.encryptedRsaPrivate; }
	String getHashedPassword() { return this.hashedPassword; }
	String getRsaPublic() { return this.rsaPublic; }
	public String getUsername() { return this.username; }

	String getDecryptedDek(String username, String password)
	{
		try {
			String keyStr = password + "-" + username + "-" + SALT;
			byte[] dekBytes = EncryptionHelper.AES.decrypt(this.getEncryptedDek(), keyStr, EncodingType.HEX);
			return new String(dekBytes, "utf-8");
		} catch (Exception e) {
			// NoSuchAlgorithmException, 
			// NoSuchPaddingException, 
			// IllegalBlockSizeException, 
			// BadPaddingException, 
			// InvalidKeyException,
			// InvalidAlgorithmParameterException,
			// UnsupportedEncodingException
			e.printStackTrace();
		}
		
		return null;
	}
	
	boolean checkHash(String username, String password) 
	{
		String toHash = password + "-" + username + "-" + SALT;
		final String SHA256 = "SHA256";
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance(SHA256);
			digest.update(toHash.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuilder hexString = new StringBuilder();
			for (byte aMessageDigest : messageDigest) {
				String h = Integer.toHexString(0xFF & aMessageDigest);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString().equals(this.hashedPassword);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return false;
	}
}
