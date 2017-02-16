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
 * a response from the LDLN server, along with the keys 
 * for use in the @{link LDLNSocketClient}.
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.001
 * @since 2015-03-11
 *
 */
public enum LDLNSocketResponseAction {
	SERVER_SEND_USERS ("server_send_users"),
	SERVER_SEND_SCHEMAS ("server_send_schemas"),
	SERVER_UPDATE_RESPONSE ("server_update_response"),
	SERVER_DIFF_RESPONSE ("server_diff_response");
	
	private final String action_key;
	
	LDLNSocketResponseAction(String action_key)
	{
		this.action_key = action_key;
	}
	
	String getActionKey() { return this.action_key; }
}
