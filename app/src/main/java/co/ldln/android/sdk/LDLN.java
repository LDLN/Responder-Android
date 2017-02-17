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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import co.ldln.android.LDLNProperties;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;

/**
 * The primary LDLN SDK API for use in Android applications
 * 
 * @author Matthew Grasser <msgrasser@gmail.com>
 * @version 0.002
 * @since 2015-03-10
 */
public class LDLN 
{
	public static final String BROADCAST_KEY = "ldln-broadcast";

	private static User USER;
	private static String USERNAME;
	private static String DEK;
	private static RealmMigration realmMigration;
	static enum RealmLevel {
		GLOBAL,
		USER
	}
	private static boolean encryptionIsEnabled = true; // Default to true

	static RealmConfiguration getRealmConfig(Context context, RealmLevel realmLevel) {
		try {
			// Automatic Versioning based on app's version code
			int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;

			// Limit migration creation to avoid collisions
			if (realmMigration == null) {
				realmMigration = new LDLNRealmMigration();
			}

			// Build the database configuration and return it
			switch(realmLevel) {
				case GLOBAL:
					return new RealmConfiguration.Builder(context)
							.name("LDLN.realm")
							.schemaVersion(versionCode)
							.migration(realmMigration)
							.build();
				case USER:
					if (USER == null || DEK == null) return null;
					return new RealmConfiguration.Builder(context)
							.name(USERNAME + ".realm")
							.schemaVersion(versionCode)
							.migration(realmMigration)
							.encryptionKey((DEK+DEK).getBytes())
							.build();
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Attempts to initiate a synchronization of data with any LDLN software that can be contacted
	 *
	 * @return            void, results are returned via the {@link InitiateSyncListener}
	 */
	public static void initiateSync(Context context, InitiateSyncListener callback)
	{
		boolean success = initializeWebSocketSync(context);
		callback.onInitiateSynchronizationResult(success);
	}

	static boolean initializeWebSocketSync(Context context) {
		try {
			String websocketIp = LDLNProperties.getProperty(context, "websocket.ip");
			URI uri = new URI("ws://" + websocketIp + ":8080/ws");
			
			HashMap<String, String> headerMap = new HashMap<String, String>();
			headerMap.put("Origin", "http://android.ldln.co/");
			
			LDLNSocketClient wsClient = new LDLNSocketClient(uri, headerMap);
			wsClient.initialize(context);
			wsClient.connect();
			
			return true;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public interface InitiateSyncListener 
	{
		public void onInitiateSynchronizationResult(boolean success);
	}
	
	/**
	 * Checks if a valid user is currently logged in via the SDK.
	 *
	 * @return            true if the user is logged in, false otherwise
	 */
	public static boolean isLoggedIn()
	{
		return (USER != null && DEK != null);
	}

	/**
	 * Logs the user out of their current session.
	 */
	public static void logout(Context context)
	{
		// Destroy USER database
		Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.USER));
		realm.beginTransaction();
		realm.deleteAll();
		realm.commitTransaction();

		// Clear "session"
		USER = null;
		USERNAME = null;
		DEK = null;
	}
	
	/**
	 * Validates a user given a username and password.
	 *
	 * @param callback    The callback that implements {@link LoginListener} and will handle the result
	 * @param username    The username of the {@link User} to be authenticated.
	 * @param password    The password of the {@link User} to be authenticated.
	 * @return            void, results are returned via the {@link LoginListener}
	 */
	public static void login(final Context context, final LoginListener callback, final String username, final String password)
	{
		USER = null;

		// Check if encryption is enabled
		// TODO: replace this with a response from the server rather than local config
		String encryptionIsEnabledStr = LDLNProperties.getProperty(context, "encryption.enabled");
		encryptionIsEnabled = (encryptionIsEnabledStr == null || !encryptionIsEnabledStr.trim().toLowerCase().equals("false"));

		new AsyncTask<Void, Void, Boolean>() {

			ProgressDialog dialog = new ProgressDialog(context);

			@Override
			protected void onPreExecute() {
				dialog.setTitle("Logging in");
				dialog.show();
			}

			@Override
			protected void onPostExecute(final Boolean success) {
				if (success) {
					dialog.setTitle("Processing local data");
					new AsyncTask<Void, String, Void>() {
						@Override
						protected Void doInBackground(Void[] objects) {
							Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.GLOBAL));
							List<SyncableObject> syncableObjects = realm.where(SyncableObject.class).findAll();

							if (syncableObjects != null) {
								// Open database for caching plaintext objects while logged in
								Realm userRealm = Realm.getInstance(getRealmConfig(context, RealmLevel.USER));
								userRealm.beginTransaction();

								// Decrypt and store objects in local cache for currently logged in user
								int numDecrypted = 0;
								int numTotal = syncableObjects.size();
								for (SyncableObject so : syncableObjects) {
									publishProgress(numDecrypted++ + " records decrypted of " +  numTotal);
									LDLN.cachePlaintextObject(userRealm, so);
								}

								// Commit the changes to the database
								userRealm.commitTransaction();
							}
							return null;
						}

						@Override
						protected void onProgressUpdate(String... values) {
							dialog.setMessage(values[0]);
						}

						@Override
						protected void onPostExecute(Void aVoid) {
							dialog.dismiss();
							callback.onLoginResult(success);
						}
					}.execute();
				} else {
					dialog.dismiss();
					callback.onLoginResult(success);
				}
			}

			@Override
			protected Boolean doInBackground(Void[] objects) {
				Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.GLOBAL));
				List<User> matchingUsers = realm.where(User.class).equalTo("username", username).findAll();
				for (User user : matchingUsers) {
					if (user.checkHash(username, password)) {
						USER = user;
						USERNAME = USER.getUsername();
						DEK = user.getDecryptedDek(username, password);
						if (DEK == null) { USER = null; USERNAME = null; } // fail if we can't retrieve a DEK
						break;
					}
				}
				return (USER != null);
			}
		}.execute();
	}

	static void cachePlaintextObject(Realm userRealm, SyncableObject so) {
		// Per LDLN convention, we need to manually check duplicates based
		// on uuid and timeModifiedSinceCreation with a manual update rule,
		// and only (1) copy to realm if it doesn't exist, or (2) update in
		// realm if an older version of the object does exist.
		//
		// note:
		//   userRealm.insert() produces duplicates
		//   userRealm.copyToRealm() throws RealmPrimaryKeyConstraintException
		if (userRealm.where(PlaintextObject.class)
				.equalTo("uuid", so.getUuid())
				.lessThanOrEqualTo("timeModifiedSinceCreation", so.getTimeModifiedSinceCreation())
				.count() == 0) {
			PlaintextObject obj = new PlaintextObject(so, DEK, encryptionIsEnabled);
			userRealm.copyToRealmOrUpdate(obj);
		}
	}

	public interface LoginListener 
	{
		public void onLoginResult(boolean success);
	}

	/**
	 * Lists the {@Link Schema} objects available for this particular deployment of the
	 * LDLN network.
	 *
	 * @param callback    The callback that implements {@link ListSchemasListener} and will handle the result
	 * @return            void, results are returned via the {@link ListSchemasListener}
	 */
	public static void listSchemas(Context context, ListSchemasListener callback)
	{
		if (USER == null) callback.onListSchemasError("Not logged in!");
		Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.GLOBAL));
		List<Schema> allSchemas = realm.where(Schema.class).findAll();
		callback.onListSchemasSuccess(allSchemas);
	}

	public interface ListSchemasListener
	{
		public void onListSchemasSuccess(List<Schema> schemas);
		public void onListSchemasError(String error);
	}

	/**
	 * Reads a single {@Link Schema} object available in the local database,
	 * filtered by a supplied key.
	 *
	 * @param callback    The callback that implements {@link ReadSchemaListener} and will handle the result
	 * @param schemaKey   The key of the {@link Schema} to retrieve.
	 * @return            void, results are returned via the {@link ReadSchemaListener}
	 */
	public static void readSchema(Context context, ReadSchemaListener callback, String schemaKey)
	{
		// Find the schema object we're looking up
		Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.GLOBAL));
		Schema matchingSchema = realm.where(Schema.class).equalTo("key", schemaKey).findFirst();
		callback.onReadSchemaResult(matchingSchema);
	}

	public interface ReadSchemaListener
	{
		public void onReadSchemaResult(Schema schema);
	}

	/**
	 * Stores a {@link SyncableObject} created from some user input, to be stored for
	 * synchronization on the next connection to a LDLN Base Station or other LDLN-sync
	 * enabled device.
	 * 
	 * @param callback          The callback that implements {@link SaveSyncableObjectListener} and will handle the result
	 * @param schemaKey    		The key for the {@link Schema} of the {@link SyncableObject} to be saved.
	 * @param keyValuePairs		The key value pairs of the {@link SyncableObject} to be saved.
	 * @return                  void, results are returned via the {@link SaveSyncableObjectListener}
	 */
	public static void saveSyncableObject(Context context, SaveSyncableObjectListener callback, String schemaKey, String keyValuePairs)
	{
		try {
			// Generate one uuid for use in both the plaintext and encrypted objects
			String uuid = UUID.randomUUID().toString();

			// Save the encrypted version of the object
			Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.GLOBAL));
			realm.beginTransaction();
			SyncableObject syncableObject = realm.createObject(SyncableObject.class, uuid);
			syncableObject.setSchema(realm.where(Schema.class).equalTo("key", schemaKey).findFirst());
			syncableObject.setKeyValuePairs(keyValuePairs, DEK, encryptionIsEnabled);
			realm.commitTransaction();

			// Save the plaintext "cached" version of the object
			PlaintextObject plaintextObject = new PlaintextObject(syncableObject, DEK, encryptionIsEnabled);
			realm = Realm.getInstance(getRealmConfig(context, RealmLevel.USER));
			realm.beginTransaction();
			realm.insert(plaintextObject);
			realm.commitTransaction();

			// Sync with server
			LDLN.initializeWebSocketSync(context);

			// Call back to the implementor of the interface
			callback.onSaveSyncableObjectResult(true);
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		callback.onSaveSyncableObjectResult(false);
	}

	public interface SaveSyncableObjectListener
	{
		public void onSaveSyncableObjectResult(boolean success);
	}

	/**
	 * Lists the {@Link SyncableObject} objects available in the local database,
	 * filtered by a supplied {@link Schema}.
	 *
	 * @param callback    The callback that implements {@link ListSyncableObjectsListener} and will handle the result
	 * @param schemaKey   The optional {@link Schema} key for the {@link SyncableObject} list to be filtered on.
	 * @return            void, results are returned via the {@link ListSyncableObjectsListener}
	 */
	public static void listSyncableObjects(Context context, ListSyncableObjectsListener callback, String schemaKey)
	{
		if (USER == null || DEK == null) callback.onListSyncableObjectsError("Not logged in!");

		// Look up Syncable Object entries in database based on any filters provided
		Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.USER));
		List<PlaintextObject> plaintextObjects;
		if (schemaKey == null) {
			plaintextObjects = realm.where(PlaintextObject.class).findAll();
		} else {
			plaintextObjects = realm.where(PlaintextObject.class).equalTo("schema.key", schemaKey).findAll();
		}

		// Send the prepared results back to the callback
		callback.onListSyncableObjectsResult(plaintextObjects);
	}

	public interface ListSyncableObjectsListener
	{
		public void onListSyncableObjectsResult(List<PlaintextObject> plaintextObjects);
		public void onListSyncableObjectsError(String error);
	}

	/**
	 * Reads a single {@Link SyncableObject} object available in the local database,
	 * filtered by a supplied uuid.
	 *
	 * @param callback    The callback that implements {@link ReadSyncableObjectListener} and will handle the result
	 * @param uuid        The uuid of the {@link SyncableObject} to retrieve.
	 * @return            void, results are returned via the {@link ReadSyncableObjectListener}
	 */
	public static void readSyncableObject(Context context, ReadSyncableObjectListener callback, String uuid)
	{
		// First find the schema object we're looking up
		Realm realm = Realm.getInstance(getRealmConfig(context, RealmLevel.USER));
		PlaintextObject matchingPlaintextObject = realm.where(PlaintextObject.class).equalTo("uuid", uuid).findFirst();
		callback.onReadSyncableObjectResult(matchingPlaintextObject);
	}

	public interface ReadSyncableObjectListener
	{
		public void onReadSyncableObjectResult(PlaintextObject plaintextObject);
	}

	public enum BroadcastMessageType {
		SYNCABLE_OBJECTS_REFRESHED;
	}
}
