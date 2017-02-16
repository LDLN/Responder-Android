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
import java.util.List;
import co.ldln.android.sdk.LDLN;
import co.ldln.android.sdk.LDLN.ListSyncableObjectsListener;
import co.ldln.android.sdk.PlaintextObject;
import co.ldln.android.sdk.SyncableObject;
import android.os.Bundle;
import android.app.Fragment;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment that appears in the "content_frame", shows the login information
 */
public class ObjectListFragment extends Fragment implements ListSyncableObjectsListener {
	private MainActivity mActivity;

	private TextView mEmptyTextView;
	private ListView mListView;
	private List<PlaintextObject> mSyncableObjects;

	public ObjectListFragment() {
		// Empty constructor required for fragment subclasses
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_object_types, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mActivity = (MainActivity) getActivity();
		mEmptyTextView = (TextView) view.findViewById(R.id.empty_object_types_msg);
		mListView = (ListView) view.findViewById(R.id.object_types_list);
		LDLN.listSyncableObjects(mActivity, this, getArguments().getString("schema_key"));
	}

	@Override
	public void onListSyncableObjectsResult(List<PlaintextObject> syncableObjects) {
		mSyncableObjects = syncableObjects;
		if (mSyncableObjects != null && mSyncableObjects.size() > 0) {
			mEmptyTextView.setVisibility(View.GONE);
		} else {
			mEmptyTextView.setVisibility(View.VISIBLE);
		}
		
		mListView.setAdapter(new SyncableObjectListAdapter());
	}

	@Override
	public void onListSyncableObjectsError(String error) {
		// Log show-stopping errors!
		Toast.makeText(mActivity, "Error: " + error, Toast.LENGTH_SHORT).show();
	}

	private class SyncableObjectListAdapter extends BaseAdapter 
	{
		private LayoutInflater inflater = null;

		public SyncableObjectListAdapter() {
			this.inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mSyncableObjects.size();
		}

		@Override
		public PlaintextObject getItem(int position) {
			return mSyncableObjects.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi=convertView;
			if(convertView==null)
				vi = inflater.inflate(R.layout.syncable_object_list_item, null);

			// Get the item at the position
			final PlaintextObject syncableObject = getItem(position);
			
			// Get references to dynamic view elements
			LinearLayout rowLayout = (LinearLayout) vi.findViewById(R.id.syncable_object_list_row);
			TextView titleTextView = (TextView) vi.findViewById(R.id.syncable_object_label);
			TextView uuidTextView = (TextView) vi.findViewById(R.id.syncable_object_uuid);
			TextView timeTextView = (TextView) vi.findViewById(R.id.syncable_object_time);

			// Set the values of these elements
			HashMap<String,String> keyValueMap = syncableObject.getKeyValueMap();
			String title = "";
			if (keyValueMap.containsKey("Title")) {
				title = keyValueMap.get("Title");
			} else if (keyValueMap.containsKey("Name")) {
				title = keyValueMap.get("Name");
			} else if (keyValueMap.containsKey("First Name")) {
				title = keyValueMap.get("First Name");
				if (keyValueMap.containsKey("Last Name")) {
					title += " " + keyValueMap.get("Last Name");
				}
			} else {
				title = syncableObject.getUuid();
			}
			titleTextView.setText(title);
			uuidTextView.setText("UUID: " + syncableObject.getUuid());
			timeTextView.setText("Time Since Mod.: " + String.valueOf(syncableObject.getTimeModifiedSinceCreation()));

			// Set click actions
			rowLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putString("syncable_object_uuid", syncableObject.getUuid());
					mActivity.openFragment(FragmentId.SYNCABLE_OBJECT_READ, args);
				}
			});

			return vi;
		}

	}
}
