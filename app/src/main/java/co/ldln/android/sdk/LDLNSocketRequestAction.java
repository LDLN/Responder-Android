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

/**
 * An enumerated list of actions available for use in
 * a request via the LDLN protocol, along with the keys 
 * for use in the @{link LDLNSocketClient}.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.001
 * @since 2015-03-11
 *
 */
public enum LDLNSocketRequestAction {
	CLIENT_GET_USERS ("client_get_users"),
	CLIENT_GET_SCHEMAS ("client_get_schemas"),
	CLIENT_UPDATE_REQUEST ("client_update_request"),
	CLIENT_DIFF_REQUEST ("client_diff_request");
	
	private final String action_key;
	
	LDLNSocketRequestAction(String action_key)
	{
		this.action_key = action_key;
	}
	
	String getActionKey() { return this.action_key; }
}
