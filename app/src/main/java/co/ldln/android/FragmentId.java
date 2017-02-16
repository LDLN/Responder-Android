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

import android.app.Fragment;
import android.util.Log;

public enum FragmentId {
	// Nav Items for anon
	LOG_IN("Log In", LogInFragment.class),

	// Nav Items for user
	SYNC_HISTORY("Sync History", SyncHistoryFragment.class),
	OBJECT_TYPES("Object Types", ObjectTypesFragment.class),
	MAP("Map", MapFragment.class),
	SYNC_OPTIONS("Sync Options", SyncOptionsFragment.class),
	LOG_OUT("Log Out", null),

	// Other Fragments
	SYNCABLE_OBJECT_LIST("Syncable Objects", ObjectListFragment.class),
	SYNCABLE_OBJECT_READ("Syncable Object Read", ObjectReadFragment.class),
	SYNCABLE_OBJECT_CREATE("Create Object", ObjectCreateFragment.class);

	private String label;
	private Class<Fragment> clazz;

	FragmentId(String label, Class clazz) {
		this.label = label;
		this.clazz = clazz;
	}

	public Fragment getFragment() {
		try {
			if (this.clazz != null) {
				return (Fragment) this.clazz.newInstance();
			}
		} catch(ClassCastException e) {
			Log.e("LDLN", e.getMessage());
		} catch (InstantiationException e) {
			Log.e("LDLN", e.getMessage());
		} catch (IllegalAccessException e) {
			Log.e("LDLN", e.getMessage());
		}

		return null;
	}

	@Override
	public String toString() {
		return this.label;
	}
}