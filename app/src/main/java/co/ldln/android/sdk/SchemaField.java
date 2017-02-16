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

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * A field defined within a flexible schema object,
 * as defined in the LDLN synchronization protocol.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.002
 * @since 2015-03-10
 */
public class SchemaField extends RealmObject implements Comparable<SchemaField>
{
	@PrimaryKey
	private String typeLabel;
	private String label;
	private String type;
	private int weight;
	private Schema schema;

	/* Empty Default Constructor Required for Realm */
	public SchemaField() { }

	/* Parse JSONObject in the form of:
        {
            "label": "Attribute 1",
            "type": "text",
            "weight": 1
        }
	 */
	public SchemaField(JSONObject schemaJsonObject, Schema schema) throws JSONException
	{
		setLabel(schemaJsonObject.getString("label"));
		setType(this.type = schemaJsonObject.getString("type"));
		setWeight(schemaJsonObject.getInt("weight"));
		setSchema(schema);
		setTypeLabel(getType() + "_" + getLabel());
	}

	void setLabel(String label) { this.label = label; }
	void setType(String type) { this.type = type; }
	void setWeight(int weight) { this.weight = weight; }
	void setSchema(Schema schema) { this.schema = schema; }
	void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }

	public String getLabel() { return this.label; }
	public String getType() { return this.type; }
	public int getWeight() { return this.weight; }
	public Schema getSchema() { return this.schema; }

	@Override
	public int compareTo(SchemaField another) 
	{
		return this.weight - another.getWeight();
	}
}
