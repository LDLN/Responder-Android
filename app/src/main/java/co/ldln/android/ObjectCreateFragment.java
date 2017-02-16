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

package co.ldln.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import co.ldln.android.sdk.LDLN;
import co.ldln.android.sdk.LDLN.ReadSchemaListener;
import co.ldln.android.sdk.LDLN.SaveSyncableObjectListener;
import co.ldln.android.sdk.Schema;
import co.ldln.android.sdk.SchemaField;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ObjectCreateFragment extends Fragment implements ReadSchemaListener, SaveSyncableObjectListener {
	MainActivity mActivity;
	String mSchemaKey;
	String mMapLocation;
	LinearLayout mFormHolder;
	ArrayList<EditText> mTextFields;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_object_create, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mActivity = (MainActivity) getActivity();
		Bundle args = getArguments();
		mSchemaKey = args.getString("schema_key");
		if (args.containsKey("map_location_lat") && args.containsKey("map_location_lon")) {
			mMapLocation = args.getString("map_location_lat") + "," + args.getString("map_location_lon");
		}
		mFormHolder = (LinearLayout) view.findViewById(R.id.form_holder);

		LDLN.readSchema(mActivity, this, mSchemaKey);
	}

	@Override
	public void onReadSchemaResult(final Schema schema) {
		mTextFields = new ArrayList<EditText>();

		// Create the form based on the schema fields
		List<SchemaField> fieldList = new ArrayList<SchemaField>();
		fieldList.addAll(schema.getFields(mActivity));
		Collections.sort(fieldList);
		for (SchemaField field : fieldList) {
			String type = field.getType();
			String label = field.getLabel();

			// Create a linear layout to hold the field
			LinearLayout ll = new LinearLayout(mActivity);
			LayoutParams llParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
			ll.setLayoutParams(llParams);
			ll.setOrientation(LinearLayout.HORIZONTAL);

			// TODO: different UI for different field types

			// Default to EditText
			EditText et = new EditText(mActivity);
			LayoutParams etParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
			et.setLayoutParams(etParams);
			et.setHint(label);
			if (type.equals("map_location")) et.setText(mMapLocation);
			ll.addView(et);
			mTextFields.add(et);

			mFormHolder.addView(ll);
		}

		// Add submit button
		Button b = new Button(mActivity);
		LayoutParams bParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		b.setText("Save");
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					String kvPairs = "";
					JSONObject kvPairsJsonObject = new JSONObject();
					for (EditText et : mTextFields) {
						String key = et.getHint().toString();
						String value = et.getText().toString();
						kvPairsJsonObject.put(key, value);
					}
					kvPairs = kvPairsJsonObject.toString();

					LDLN.saveSyncableObject(mActivity, ObjectCreateFragment.this, schema.getKey(), kvPairs);
				} catch (JSONException e) {
					e.printStackTrace();
					onSaveSyncableObjectResult(false);
				}
			}
		});
		mFormHolder.addView(b);

		Toast.makeText(mActivity, "This is a form that's dynamically generated from a syncable object schema. It will soon have handling for dynamic form elements (photo, latlon picker, etc).", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onSaveSyncableObjectResult(boolean success) {
		if (success) {
			Toast.makeText(mActivity, "Item successfully created and stored for synchronization!", Toast.LENGTH_LONG).show();
			mActivity.onBackPressed();
			mActivity.refreshFragment();
		} else {
			Toast.makeText(mActivity, "There was an error creating the item!", Toast.LENGTH_LONG).show();
		}
	}
}
