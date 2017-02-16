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

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import co.ldln.android.sdk.EncryptionHelper.EncodingType;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * An object to be synced via the LDLN protocol.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.002
 * @since 2015-03-10
 */
public class SyncableObject extends RealmObject
{
	@PrimaryKey
	private String uuid;
	private String keyValuePairs;
	private Schema schema;
	private int timeModifiedSinceCreation;

	/* Empty Default Constructor Required for Realm */
	public SyncableObject() { }

	/* Create a new Syncable object */
	public SyncableObject(Schema schema, String plainTextKeyValuePairs, String DEK) throws UnsupportedEncodingException {
		this.schema = schema;
		this.keyValuePairs = EncryptionHelper.AES.encrypt(plainTextKeyValuePairs, DEK, EncodingType.HEX);
		this.timeModifiedSinceCreation = 0;
		this.uuid = UUID.randomUUID().toString();
	}

	/* Parse the JSONObject in the form of:
	{
        "keyValuePairs": "XXXXXX",
        "object_type": "test",
        "timeModifiedSinceCreation": 0,
        "uuid": "XXXXXX"
    }
	 */
	public SyncableObject(JSONObject syncableObjectJsonObject, Context context) throws JSONException
	{
		setEncryptedKeyValuePairs(syncableObjectJsonObject.getString("key_value_pairs"));

		String schemaKey = syncableObjectJsonObject.getString("object_type");
		Realm realm = Realm.getInstance(LDLN.getRealmConfig(context, LDLN.RealmLevel.GLOBAL));
		setSchema(realm.where(Schema.class).equalTo("key", schemaKey).findFirst());

		setTimeModifiedSinceCreation(syncableObjectJsonObject.getInt("time_modified_since_creation"));
		setUuid(syncableObjectJsonObject.getString("uuid"));
	}

	// Setters
	void setUuid(String uuid) { this.uuid = uuid; }
	void setKeyValuePairs(String plaintextKeyValuePairs, String DEK, boolean encryptionIsEnabled) throws UnsupportedEncodingException {
		if (encryptionIsEnabled) {
			this.keyValuePairs = EncryptionHelper.AES.encrypt(plaintextKeyValuePairs, DEK, EncodingType.HEX);
		} else {
			this.keyValuePairs = plaintextKeyValuePairs;
		}
	}
	void setEncryptedKeyValuePairs(String encryptedKeyValuePairs) { this.keyValuePairs = encryptedKeyValuePairs; }
	void setSchema(Schema schema) { this.schema = schema; }
	void setTimeModifiedSinceCreation(int timeModifiedSinceCreation) {this.timeModifiedSinceCreation = timeModifiedSinceCreation; }

	// Getters
	public String getUuid() { return this.uuid; }
	public Schema getSchema() { return this.schema; }
	public int getTimeModifiedSinceCreation() { return this.timeModifiedSinceCreation; }

	public String getPlaintextKeyValuePairs(String DEK)
	{
		if (this.keyValuePairs == null) return null;

		try {
			byte[] plaintextBytes = EncryptionHelper.AES.decrypt(this.keyValuePairs, DEK, EncodingType.HEX);
			return new String(plaintextBytes);
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

	protected String getRawKeyValuePairs() {
		return this.keyValuePairs;
	}

	public JSONObject getAsJson() throws JSONException 
	{
		JSONObject syncableObjectJsonObject = new JSONObject();
		syncableObjectJsonObject.put("uuid", this.getUuid());
		syncableObjectJsonObject.put("key_value_pairs", this.keyValuePairs); // Use encrypted pairs
		syncableObjectJsonObject.put("object_type", this.getSchema().getKey());
		syncableObjectJsonObject.put("time_modified_since_creation", this.getTimeModifiedSinceCreation());
		return syncableObjectJsonObject;
	}
}
