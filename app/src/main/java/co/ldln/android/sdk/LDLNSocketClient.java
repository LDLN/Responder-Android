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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import io.realm.Realm;

/**
 * Extension of TooTallNate's Java-WebSocket WebSocketClient class,
 * found at http://java-websocket.org/ and on Github at the address
 * https://github.com/TooTallNate/Java-WebSocket
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.001
 * @since 2015-03-11
 */
public class LDLNSocketClient extends WebSocketClient
{
	private Context mContext;

	/**
	 * This open a websocket connection as specified by rfc6455
	 * 
	 * @param uri    The {@link java.net.URI} of the websocket server
	 */
	public LDLNSocketClient(URI uri, HashMap<String, String> headerMap)
	{
		// More Info:
		// http://stackoverflow.com/questions/25802290/web-socket-connection-failed-from-android-client
		// http://stackoverflow.com/questions/21035326/what-draft-does-java-websockets-websocketserver-use/21045261#21045261
		//
		super(uri, new Draft_17(), headerMap, 500);
	}

	public void initialize(Context context)
	{
		this.mContext = context;
	}

	@Override
	public void onOpen(ServerHandshake serverHandshake) 
	{
		Log.d("Websocket", "Opened");

		// Send request for User objects 
		sendRequest(LDLNSocketRequestAction.CLIENT_GET_USERS, null, null);

		// Send request for Schema objects
		sendRequest(LDLNSocketRequestAction.CLIENT_GET_SCHEMAS, null, null);

		// Send request for SyncableObject objects
		HashMap<String, JSONObject> additionalData = new HashMap<String, JSONObject>();
		JSONObject objectUuidsJsonObject = new JSONObject();
		try {
			Realm realm = Realm.getInstance(LDLN.getRealmConfig(mContext, LDLN.RealmLevel.GLOBAL));
			List<SyncableObject> locallyStoredObjects = realm.where(SyncableObject.class).findAll();
			for (SyncableObject locallyStoredObject : locallyStoredObjects) {
				objectUuidsJsonObject.put(locallyStoredObject.getUuid(), locallyStoredObject.getTimeModifiedSinceCreation());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		additionalData.put("object_uuids", objectUuidsJsonObject);
		sendRequest(LDLNSocketRequestAction.CLIENT_DIFF_REQUEST, additionalData, null);
	}

	private void sendRequest(LDLNSocketRequestAction action, HashMap<String,JSONObject> additionalJsonObjectData, HashMap<String,JSONArray> additionalJsonArrayData)
	{
		try {
			JSONObject messageJsonObj = new JSONObject();
			messageJsonObj.put("action", action.getActionKey());
			if (additionalJsonObjectData != null) {
				for (Entry<String,JSONObject> entry : additionalJsonObjectData.entrySet()) {
					messageJsonObj.put(entry.getKey(), entry.getValue());
				}
			}
			if (additionalJsonArrayData != null) {
				for (Entry<String,JSONArray> entry : additionalJsonArrayData.entrySet()) {
					messageJsonObj.put(entry.getKey(), entry.getValue());
				}
			}
			send(messageJsonObj.toString());
		} catch (JSONException e) {
			Log.e("Websocket", "Error sending a LDLN socket request.");
		}

	}

	@Override
	public void onMessage(String s) 
	{
		Log.d("Websocket", "Message from server! \"" + s + "\"");

		try {
			JSONObject responseJsonObj = new JSONObject(s);
			String action_code = responseJsonObj.getString("action");
			Realm realm = Realm.getInstance(LDLN.getRealmConfig(mContext, LDLN.RealmLevel.GLOBAL));

			if (LDLNSocketResponseAction.SERVER_SEND_USERS.getActionKey().equals(action_code)) {
				Log.d("Websocket", LDLNSocketResponseAction.SERVER_SEND_USERS.getActionKey() + " response recognized.");

				// Handle users response from the server, per the LDLN protocol.
				/*
				   {
					    "action": "server_send_users",
					    "users": [
					        {
					            "encrypted_kek": "abcdef1234567890",
					            "encrypted_rsa_private": "abcdef1234567890",
					            "hashed_password": "abcdef1234567890",
					            "rsa_public": "abcdef1234567890",
					            "username": "admin"
					        }
					    ]
					}
				 */

				// Parse the array and iterate through it, either saving or updating
				JSONArray usersJsonArray = responseJsonObj.getJSONArray("users");
				for (int i=0; i<usersJsonArray.length(); i++) {
					JSONObject userJsonObject = usersJsonArray.getJSONObject(i);
					User user = new User(userJsonObject);
					realm.beginTransaction();
					realm.insertOrUpdate(user);
					realm.commitTransaction();
				}
			} else if (LDLNSocketResponseAction.SERVER_SEND_SCHEMAS.getActionKey().equals(action_code)) {
				Log.d("Websocket", LDLNSocketResponseAction.SERVER_SEND_SCHEMAS.getActionKey() + " response recognized.");

				// Handle schemas response from the server, per the LDLN protocol.
				/*
				 {
				    "action": "server_send_schemas",
				    "schemas": [
				        {
				            "object_key": "example",
				            "object_label": "Example Thing",
				            "schema": [
				                {
				                    "label": "Attribute 1",
				                    "type": "text",
				                    "weight": 1
				                }
								...
				            ],
				            "weight": 1
				        }
				    ]
				}
				 */

				// Parse the array and iterate through it, either saving or updating
				JSONArray schemasJsonArray = responseJsonObj.getJSONArray("schemas");
				for (int i=0; i<schemasJsonArray.length(); i++) {

					JSONObject schemaJsonObject = schemasJsonArray.getJSONObject(i);

					// Save the Schema object
					Schema schema = new Schema(schemaJsonObject);
					realm.beginTransaction();
					realm.insertOrUpdate(schema);
					realm.commitTransaction();

					// Save the associated SchemaField objects
					JSONArray schemaFieldsJsonArray = schemaJsonObject.getJSONArray("schema");
					for (int j=0; j<schemaFieldsJsonArray.length(); j++) {
						JSONObject schemaFieldJsonObject = schemaFieldsJsonArray.getJSONObject(j);
						SchemaField schemaField = new SchemaField(schemaFieldJsonObject, schema);
						realm.beginTransaction();
						realm.insertOrUpdate(schemaField);
						realm.commitTransaction();
					}
				}

			} else if (LDLNSocketResponseAction.SERVER_UPDATE_RESPONSE.getActionKey().equals(action_code)) {
				Log.d("Websocket", LDLNSocketResponseAction.SERVER_UPDATE_RESPONSE.getActionKey() + " response recognized.");
				/*
				 {
				    "action": "server_update_response",
				    "created_object_uuids": null,
				    "updated_objects": [
				        {
				            "key_value_pairs": "XXXXXX",
				            "object_type": "test",
				            "time_modified_since_creation": 0,
				            "uuid": "XXXXXX"
				        }
				    ]
				}
				 */
				// TODO: Handle update response from the server, per the LDLN protocol. Perhaps this can just be a success/fail log?
			} else if (LDLNSocketResponseAction.SERVER_DIFF_RESPONSE.getActionKey().equals(action_code)) {
				Log.d("Websocket", LDLNSocketResponseAction.SERVER_DIFF_RESPONSE.getActionKey() + " response recognized.");
				/*
				 {
				    "action": "server_diff_response",
				    "client_unknown_objects": [
				        {
				            "key_value_pairs": "XXXXXX",
				            "object_type": "test",
				            "time_modified_since_creation": 0,
				            "uuid": "XXXXXX"
				        }
				    ],
				    "modified_objects": null,
				    "server_unknown_object_uuids": [
				        "03481600-0478-11e4-9191-0800200c9a66",
				        "13481600-0477-11e4-9191-0800200c9a66"
				    ]
				}
				 */

				// Handling new server objects and server-updated objects here
				//   new ones get created
				//   updated ones get overwritten
				// Open database for caching plaintext objects while logged in
				Realm userRealm = Realm.getInstance(LDLN.getRealmConfig(mContext, LDLN.RealmLevel.USER));
				userRealm.beginTransaction();
				JSONArray newSyncableObjectsToPullJsonArray = (responseJsonObj.isNull("client_unknown_objects")) ? new JSONArray() : responseJsonObj.getJSONArray("client_unknown_objects");
				for (int i=0; i<newSyncableObjectsToPullJsonArray.length(); i++) {

					JSONObject syncableObjectToPullJsonObject = newSyncableObjectsToPullJsonArray.getJSONObject(i);

					// Save the SyncableObject object
					SyncableObject syncableObject = new SyncableObject(syncableObjectToPullJsonObject, mContext);
					realm.beginTransaction();
					realm.insertOrUpdate(syncableObject);
					realm.commitTransaction();

					LDLN.cachePlaintextObject(userRealm, syncableObject);
				}
				JSONArray modifiedSyncableObjectsToPullJsonArray = (responseJsonObj.isNull("modified_objects")) ? new JSONArray() : responseJsonObj.getJSONArray("modified_objects");
				for (int i=0; i<modifiedSyncableObjectsToPullJsonArray.length(); i++) {

					JSONObject syncableObjectToPullJsonObject = modifiedSyncableObjectsToPullJsonArray.getJSONObject(i);

					// Save the SyncableObject object
					SyncableObject syncableObject = new SyncableObject(syncableObjectToPullJsonObject, mContext);
					realm.beginTransaction();
					realm.insertOrUpdate(syncableObject);
					realm.commitTransaction();

					LDLN.cachePlaintextObject(userRealm, syncableObject);
				}
				userRealm.commitTransaction();

				// Send app broadcast so views can receive a refresh notice!
				Intent intent = new Intent(LDLN.BROADCAST_KEY);
				intent.putExtra("message", LDLN.BroadcastMessageType.SYNCABLE_OBJECTS_REFRESHED);
				LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
				
				// Identify the objects that the server doesn't have, and send them up
				JSONArray newSyncableObjectsToPushJsonArray = new JSONArray();
				JSONArray newSyncableObjectUuidsToPushJsonArray = (responseJsonObj.isNull("server_unknown_object_uuids")) ? new JSONArray() : responseJsonObj.getJSONArray("server_unknown_object_uuids");
				for (int i=0; i<newSyncableObjectUuidsToPushJsonArray.length(); i++) {
					
					// Look up the object by uuid
					String syncableObjectUuidToPush = newSyncableObjectUuidsToPushJsonArray.getString(i);
					SyncableObject existingSyncableObjectToPush = realm.where(SyncableObject.class).equalTo("uuid", syncableObjectUuidToPush).findFirst();
					newSyncableObjectsToPushJsonArray.put(existingSyncableObjectToPush.getAsJson());
				}
				
				// If we indeed have objects to push, send them up
				if (newSyncableObjectsToPushJsonArray.length() > 0) {
					HashMap<String,JSONArray> additionalData = new HashMap<String,JSONArray>();
					additionalData.put("objects", newSyncableObjectsToPushJsonArray);
					sendRequest(LDLNSocketRequestAction.CLIENT_UPDATE_REQUEST, null, additionalData);
				}
			}
		} catch (JSONException e) {
			Log.e("Websocket", "Error parsing a LDLN socket response.");
			e.printStackTrace();
			return;
		}
	}

	@Override
	public void onClose(int i, String s, boolean b) 
	{
		Log.d("Websocket", "Closed " + s);
	}

	@Override
	public void onError(Exception e) 
	{
		Log.d("Websocket", "Error " + e.getMessage());
	}
}
