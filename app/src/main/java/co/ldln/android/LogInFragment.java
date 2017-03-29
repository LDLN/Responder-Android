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

import co.ldln.android.sdk.LDLN;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * Fragment that appears in the "content_frame", shows the login information
 */
public class LogInFragment extends Fragment implements OnClickListener {
	private MainActivity mActivity;
	private EditText mUsernameEditText;
	private EditText mPasswordEditText;

	public LogInFragment() {
		// Empty constructor required for fragment subclasses
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_log_in, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mActivity = (MainActivity) getActivity();
		mUsernameEditText = (EditText) view.findViewById(R.id.login_username);
		mPasswordEditText = (EditText) view.findViewById(R.id.login_password);
		view.findViewById(R.id.login_button).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) 
	{
		switch (v.getId()) {
		case R.id.login_button:
			LDLN.login(mActivity, mActivity, mUsernameEditText.getText().toString(), mPasswordEditText.getText().toString());
			break;
		}
	}
}
