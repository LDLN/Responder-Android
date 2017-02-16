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

import java.util.ArrayList;
import java.util.HashSet;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * An flexible schema object as defined by the LDLN
 * synchronization protocol.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.002
 * @since 2015-03-10
 */
public class Schema extends RealmObject implements Comparable<Schema>
{
	@PrimaryKey
	private String key;
	private String label;
	private int weight;

	/* Empty Default Constructor Required for Realm */
	public Schema() { }

	/* Parse the JSONObject in the form of:
        {
            "object_key": "example",
            "object_label": "Example Thing",
            "schema": [
                {
                    "label": "Attribute 1",
                    "type": "text",
                    "weight": 1
                }
				...
            ],
            "weight": 1
        }
        
    * Note that we will ignore the SchemaField objects, as those 
    * will each have to be saved based on this successfully saving.
	*/
	public Schema(JSONObject schemaJsonObject) throws JSONException
	{
		setKey(schemaJsonObject.getString("object_key"));
		setLabel(schemaJsonObject.getString("object_label"));
		setWeight(schemaJsonObject.getInt("weight"));
	}

	// Setters
	public void setKey(String key) { this.key = key; }
	public void setLabel(String label) { this.label = label; }
	public void setWeight(int weight) {	this.weight = weight; }

	// Getters
	public String getKey() { return this.key; }
	public String getLabel() { return this.label; }
	public int getWeight() { return this.weight; }
	public ArrayList<SchemaField> getFields(Context context) {
		Realm realm = Realm.getInstance(LDLN.getRealmConfig(context, LDLN.RealmLevel.GLOBAL));
		return new ArrayList(realm.where(SchemaField.class).equalTo("schema.key", this.key).findAll());
	}
	public ArrayList<SyncableObject> getSyncableObjects(Context context) {
		Realm realm = Realm.getInstance(LDLN.getRealmConfig(context, LDLN.RealmLevel.GLOBAL));
		return new ArrayList(realm.where(SyncableObject.class).equalTo("schema.key", this.key).findAll());
	}

	@Override
	public int compareTo(Schema another) 
	{
		return this.weight - another.getWeight();
	}
}
