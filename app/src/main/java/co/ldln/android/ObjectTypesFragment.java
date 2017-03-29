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

import java.util.List;

import co.ldln.android.sdk.LDLN;
import co.ldln.android.sdk.LDLN.ListSchemasListener;
import co.ldln.android.sdk.Schema;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment that appears in the "content_frame", shows the login information
 */
public class ObjectTypesFragment extends Fragment implements ListSchemasListener {
	private MainActivity mActivity;

	private TextView mEmptyTextView;
	private ListView mListView;
	private List<Schema> mSchemas;

	public ObjectTypesFragment() {
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
		LDLN.listSchemas(mActivity, this);
	}

	@Override
	public void onListSchemasSuccess(List<Schema> schemas) {
		mSchemas = schemas;
		if (mSchemas != null && mSchemas.size() > 0) {
			mEmptyTextView.setVisibility(View.GONE);
		} else {
			mEmptyTextView.setVisibility(View.VISIBLE);
		}
		mListView.setAdapter(new ObjectTypeListAdapter());
	}

	@Override
	public void onListSchemasError(String error) {
		Toast.makeText(mActivity, "Error: " + error, Toast.LENGTH_SHORT).show();
	}

	private class ObjectTypeListAdapter extends BaseAdapter 
	{
		private LayoutInflater inflater = null;

		public ObjectTypeListAdapter() {
			this.inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mSchemas.size();
		}

		@Override
		public Schema getItem(int position) {
			return mSchemas.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi=convertView;
			if(convertView==null)
				vi = inflater.inflate(R.layout.schema_list_item, null);

			RelativeLayout rowLayout = (RelativeLayout) vi.findViewById(R.id.schema_list_row);
			TextView title = (TextView) vi.findViewById(R.id.schema_label);
			Button createButton = (Button) vi.findViewById(R.id.create_button);

			// Setting all values in listview
			final Schema schema = getItem(position);
			title.setText(schema.getLabel());

			// Set click actions
			rowLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putString("schema_key", schema.getKey());
					mActivity.openFragment(FragmentId.SYNCABLE_OBJECT_LIST, args);
				}
			});
			createButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putString("schema_key", schema.getKey());
					mActivity.openFragment(FragmentId.SYNCABLE_OBJECT_CREATE, args);
				}
			});

			return vi;
		}  
	}

}
