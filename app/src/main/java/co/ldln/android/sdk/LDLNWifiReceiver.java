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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * The wifi connection listener for use in triggering synchronizations
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.001
 * @since 2015-04-20
 */
public class LDLNWifiReceiver extends BroadcastReceiver 
{
	private final static String TAG = LDLNWifiReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {     
		ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE); 
		NetworkInfo netInfo = conMan.getActiveNetworkInfo();
		if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			String bssid = netInfo.getExtraInfo();
			Log.d("WifiReceiver", "Have Wifi Connection: " + bssid);
			
			// TODO: Complete Handshake Logic here. Perhaps look at NSDManager as an alternative:
			// http://developer.android.com/reference/android/net/nsd/NsdManager.html
			
			LDLN.initializeWebSocketSync(context);
		} else {
			Log.d("WifiReceiver", "Don't have Wifi Connection");
		}
	}
}
