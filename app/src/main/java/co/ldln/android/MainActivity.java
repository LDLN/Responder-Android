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

import co.ldln.android.sdk.LDLN;
import co.ldln.android.sdk.LDLN.LoginListener;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements LoginListener, OnBackStackChangedListener, LDLN.InitiateSyncListener {
	private ArrayList<FragmentId> mNavItems;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;
	private CharSequence mTitle;
	private FragmentManager mFragmentManager;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action_bar, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(null);
		setContentView(R.layout.drawer_layout);

		mFragmentManager = getSupportFragmentManager();
		mFragmentManager.addOnBackStackChangedListener(this);

		// Set up navigation bar based on session state
		mNavItems = new ArrayList<FragmentId>();
		if (LDLN.isLoggedIn()) {
			mNavItems.add(FragmentId.SYNC_HISTORY);
			mNavItems.add(FragmentId.OBJECT_TYPES);
			mNavItems.add(FragmentId.MAP);
			mNavItems.add(FragmentId.SYNC_OPTIONS);
			mNavItems.add(FragmentId.LOG_OUT);
		} else {
			mNavItems.add(FragmentId.SYNC_OPTIONS);
			mNavItems.add(FragmentId.LOG_IN);
		}

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getActionBar().setTitle(mTitle);
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle("Select an Action");
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// Get the nav list view for population
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// Set the adapter for the list view
		mDrawerList.setAdapter(new ArrayAdapter<FragmentId>(this,
				R.layout.drawer_list_item, mNavItems));

		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Register to receive messages broadcast from other parts of the app
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
				new IntentFilter(LDLN.BROADCAST_KEY));

		if (LDLN.isLoggedIn()) {
			if (savedInstanceState != null && mFragmentManager.getBackStackEntryCount() > 0) {
				Fragment currentFragment = mFragmentManager.findFragmentById(R.id.content_frame);
				mFragmentManager.beginTransaction().detach(currentFragment).attach(currentFragment).commit();
			} else {
				this.openFragment(FragmentId.OBJECT_TYPES, null);
			}
		} else {
			this.openFragment(FragmentId.LOG_IN, null);
		}
	}

	// Our handler for received LDLN Intents.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			LDLN.BroadcastMessageType messageType = (LDLN.BroadcastMessageType) intent.getSerializableExtra("message");
			if (messageType.equals(LDLN.BroadcastMessageType.SYNCABLE_OBJECTS_REFRESHED)) {
				refreshFragment();
			}
		}
	};

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			selectNavItem(position);
		}
	}

	/** Swaps fragments in the main content view */
	public void openFragment(FragmentId selectedItem, Bundle args) {

		// Create a fragment and add args based on selected item
		Fragment fragment = selectedItem.getFragment();

		// Handle log out and unfinished items in a special way
		if (selectedItem == FragmentId.LOG_OUT) {
			LDLN.logout(this);
			recreate();
			return;
		} else if (fragment == null) {
			Toast.makeText(this, selectedItem + " Not Implemented Yet!", Toast.LENGTH_SHORT).show();
			return;
		}

		// Add any args that were set above
		fragment.setArguments(args);

		// Insert the fragment by replacing any existing fragment
		FragmentTransaction fragmentTransaction;
		if (selectedItem == FragmentId.SYNCABLE_OBJECT_CREATE) {
			fragmentTransaction = mFragmentManager.beginTransaction()
					.add(R.id.content_frame, fragment);
		} else {
			// If it's a map fragment, reuse any previous instance of it that may exist
			if (selectedItem == FragmentId.MAP) {
				Fragment tmpFrag = mFragmentManager.findFragmentByTag(selectedItem.toString());
				fragment = (tmpFrag == null) ? fragment : tmpFrag;
			}
			fragmentTransaction = mFragmentManager.beginTransaction()
					.replace(R.id.content_frame, fragment);
		}
		if (selectedItem != FragmentId.LOG_IN) {
			fragmentTransaction.addToBackStack(selectedItem.toString());
		}
		fragmentTransaction.commit();

		// Highlight the selected item, update the title, and close the drawer
		setTitle(selectedItem.toString());
	}

	public void refreshFragment() {
		FragmentTransaction ft = mFragmentManager.beginTransaction();
		Fragment currentFragment = mFragmentManager.findFragmentById(R.id.content_frame);
		if (currentFragment.getClass().equals(MapFragment.class)) {
			((MapFragment) currentFragment).insertMarkersAndCenter();
		} else {
			ft.detach(currentFragment).attach(currentFragment).commit();
		}
	}

	/** Updates view based on clicked navigation item */
	private void selectNavItem(int position) {
		openFragment(mNavItems.get(position), new Bundle());
		mDrawerList.setItemChecked(position, true);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	@Override
	protected void onDestroy() {
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onDestroy();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		// Handle other ActionBar Items
		switch (item.getItemId()) {
			case R.id.action_refresh:
				// User chose the "Refresh" action, sync data
				LDLN.initiateSync(this, this);
				return true;

			default:
				// If we got here, the user's action was not recognized.
				// Invoke the superclass to handle it.
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onInitiateSynchronizationResult(boolean success) {
		if (success) {
			Toast.makeText(this, "Successfully Initiated Synchronization!", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Error Initiating Synchronization! Please ensure you're connected to a LDLN network and try again...", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onLoginResult(boolean success) {
		Log.d("LoginResult", String.valueOf(success));
		if (success) {
			// w00t! we're in...do some 1337 sheeez
			recreate();
		} else {
			// There has been an error!
			Toast.makeText(this, "There was an error logging in. Please check your credentials and try again.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onBackStackChanged() {
		// TODO: fix multiple instances of the same fragment (e.g. maps) being loaded into memory
		if (mFragmentManager.getBackStackEntryCount() > 0) {
			mTitle = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount()-1).getName();
			getActionBar().setTitle(mTitle);
		}
	}
}
