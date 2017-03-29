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

import java.util.HashMap;

import co.ldln.android.sdk.LDLN;
import co.ldln.android.sdk.LDLN.ReadSchemaListener;
import co.ldln.android.sdk.LDLN.ReadSyncableObjectListener;
import co.ldln.android.sdk.PlaintextObject;
import co.ldln.android.sdk.Schema;
import co.ldln.android.sdk.SchemaField;
import co.ldln.android.sdk.SyncableObject;
import android.support.v4.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ObjectReadFragment extends Fragment implements ReadSyncableObjectListener, ReadSchemaListener {
	MainActivity mActivity;
	PlaintextObject mSyncableObject;
	Schema mSchema;
	LinearLayout mFormHolder;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_object_read, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mActivity = (MainActivity) getActivity();
		String syncableObjectUuid = getArguments().getString("syncable_object_uuid");
		mFormHolder = (LinearLayout) view.findViewById(R.id.form_holder);

		LDLN.readSyncableObject(mActivity, this, syncableObjectUuid);
	}

	@Override
	public void onReadSyncableObjectResult(PlaintextObject syncableObject) {
		mSyncableObject = syncableObject;
		onReadSchemaResult(mSyncableObject.getSchema());
	}

	@Override
	public void onReadSchemaResult(Schema schema) {
		mSchema = schema;
		HashMap<String, String> keyValueMap = mSyncableObject.getKeyValueMap();
		for (SchemaField schemaField : mSchema.getFields(mActivity)) {
			String label = schemaField.getLabel();
			String value = keyValueMap.get(label);
			String type = schemaField.getType();

			// Create a linear layout to hold the field
			LinearLayout ll = new LinearLayout(mActivity);
			LayoutParams llParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
			ll.setLayoutParams(llParams);
			ll.setOrientation(LinearLayout.HORIZONTAL);

			// TODO: different UI for different field types

			// Default to TextView
			TextView labelTv = new TextView(mActivity);
			LayoutParams labelTvParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
			labelTv.setLayoutParams(labelTvParams);
			labelTv.setText(label);
			labelTv.setPadding(0, 0, 20, 0);
			labelTv.setTypeface(Typeface.DEFAULT_BOLD);
			ll.addView(labelTv);
			
			TextView valueTv = new TextView(mActivity);
			LayoutParams valueTvParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
			valueTv.setLayoutParams(valueTvParams);
			valueTv.setText(value);
			ll.addView(valueTv);

			mFormHolder.addView(ll);
		}
	}
}
