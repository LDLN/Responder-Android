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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * An object to be synced via the LDLN protocol.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.002
 * @since 2015-03-10
 */
public class PlaintextObject extends RealmObject
{
	@PrimaryKey
	private String uuid;
	private String keyValuePairs;
	private Schema schema;
	private int timeModifiedSinceCreation;

	/* Empty Default Constructor Required for Realm */
	public PlaintextObject() {}

	/* Create a new Syncable object */
	public PlaintextObject(Schema schema, String keyValuePairs, String DEK) throws UnsupportedEncodingException {
		this.schema = schema;
		this.keyValuePairs = keyValuePairs;
		this.timeModifiedSinceCreation = 0;
		this.uuid = UUID.randomUUID().toString();
	}

	public PlaintextObject(SyncableObject syncableObject, String DEK, boolean encryptionIsEnabled)
	{
		setSchema(syncableObject.getSchema());
		if (encryptionIsEnabled) {
			setKeyValuePairs(syncableObject.getPlaintextKeyValuePairs(DEK));
		} else {
			setKeyValuePairs(syncableObject.getRawKeyValuePairs());
		}
		setTimeModifiedSinceCreation(syncableObject.getTimeModifiedSinceCreation());
		setUuid(syncableObject.getUuid());
	}

	// Setters
	void setUuid(String uuid) { this.uuid = uuid; }
	void setKeyValuePairs(String keyValuePairs) { this.keyValuePairs = keyValuePairs; }
	void setSchema(Schema schema) { this.schema = schema; }
	void setTimeModifiedSinceCreation(int timeModifiedSinceCreation) {this.timeModifiedSinceCreation = timeModifiedSinceCreation; }

	// Getters
	public String getUuid() { return this.uuid; }
	public Schema getSchema() { return this.schema; }
	public int getTimeModifiedSinceCreation() { return this.timeModifiedSinceCreation; }
	public String getKeyValuePairs()
	{
		return this.keyValuePairs;
	}

	public LinkedHashMap<String,String> getKeyValueMap()
	{
		LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();
		try {
			if (this.keyValuePairs != null) {
				JSONObject keyValueJsonObj = new JSONObject(this.keyValuePairs);
				Iterator keys = keyValueJsonObj.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					String value = keyValueJsonObj.getString(key);
					map.put(key, value);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return map;
	}
}
