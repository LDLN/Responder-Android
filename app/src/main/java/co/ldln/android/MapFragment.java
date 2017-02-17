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

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mousebird.maply.ComponentObject;
import com.mousebird.maply.GlobeMapFragment;
import com.mousebird.maply.MBTiles;
import com.mousebird.maply.MapController;
import com.mousebird.maply.MapboxVectorTileSource;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.MarkerInfo;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.QuadPagingLayer;
import com.mousebird.maply.ScreenMarker;
import com.mousebird.maply.SelectedObject;
import com.mousebird.maply.SphericalMercatorCoordSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import co.ldln.android.sdk.LDLN;
import co.ldln.android.sdk.PlaintextObject;

/**
 * Fragment that appears in the "content_frame", shows the login information
 */
public class MapFragment extends GlobeMapFragment implements MapController.GestureDelegate, LDLN.ListSyncableObjectsListener {
	private MainActivity mActivity;
	private ArrayList<ComponentObject> markers = new ArrayList<>();

	public MapFragment() {
		// Empty constructor required for fragment subclasses
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return baseControl.getContentView();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mActivity = (MainActivity) getActivity();
	}

	@Override
	protected MapDisplayType chooseDisplayType() {
		return MapDisplayType.Map;
	}

	@Override
	protected void controlHasStarted() {
		checkPermissionsAndLoadOfflineMaps();
	}

	private void checkPermissionsAndLoadOfflineMaps() {

		// Here, thisActivity is the current activity
		if (ContextCompat.checkSelfPermission(mActivity,
				Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
					Manifest.permission.READ_EXTERNAL_STORAGE)) {

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.


			} else {

				// No explanation needed, we can request the permission.

				ActivityCompat.requestPermissions(mActivity,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
						100);

				// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
				// app-defined int constant. The callback method gets the
				// result of the request.
				// TODO: Determine if this is ever actually needed on any devices/OSs
			}
		} else {
			// Permission was previously granted, can just continue to loading the maps
			loadOfflineMaps();
		}
	}

	private void loadOfflineMaps() {
		// Find the mbtiles file in a LDLN subfolder of the SDK card
		// Note: if there is more than one file, the following logic will
		// find the last alphabetically-ordered mbtiles file in that folder
		// and use that.
		String filePath = null;
		File sdCardRoot = Environment.getExternalStorageDirectory();
		File dir = new File(sdCardRoot, "LDLN");
		for (File f : dir.listFiles()) {
			if (f.isFile()) {
				String name = f.getName();
				Log.d("LDLN Dir", name);
				if (name.substring(name.lastIndexOf('.') + 1).equals("mbtiles"))
					filePath = dir + "/" + name;
			}
		}

		// Load the map
		if (filePath != null) {
			// Load the map and create a suitable object for including in the map as a layer
			File file = new File(filePath);
			MBTiles mbTiles = new MBTiles(file);
			LDLNVectorSimpleStyleGenerator vectorStyleSimpleGenerator = new LDLNVectorSimpleStyleGenerator(mActivity, mapControl);
			MapboxVectorTileSource mapboxVectorTileSource = new MapboxVectorTileSource(mbTiles, vectorStyleSimpleGenerator);
			QuadPagingLayer quadPagingLayer = new QuadPagingLayer(mapControl, new SphericalMercatorCoordSystem(), mapboxVectorTileSource);

			// Set controller to be gesture delegate.
			// Needed to allow selection.
			mapControl.gestureDelegate = this;

			// Set the color of the land
			mapControl.setClearColor(Color.LTGRAY);

			// Add the layer
			mapControl.addLayer(quadPagingLayer);

			// Stop the map from being able to rotate
			mapControl.setAllowRotateGesture(false);

			// Insert markers and center
			insertMarkersAndCenter();

			// Provide some instructions for using the map
			Toast.makeText(mActivity, "Map is loaded!\n-Pan & Zoom like any map\n- Longtap to create pins\n- Tap pins to see details", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case 100: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					// storage-related task you need to do.
					loadOfflineMaps();
				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	@Override
	public void userDidTap(MapController mapController, Point2d loc, Point2d screenLoc) {
		// Log the various data points associated with the tap
		Log.d("Map Tapped", "Loc | x: " + loc.getX() + " y: " + loc.getY() + " z: " + mapControl.getPositionGeo().getZ());
		Log.d("Map Tapped", "Latlon | lon: " + loc.toDegrees().getX() + " lat: " + loc.toDegrees().getY());
		Log.d("Map Tapped", "ScreenLoc | x: " + screenLoc.getX() + " y: " + screenLoc.getY());
	}

	@Override
	public void userDidLongPress(MapController mapController, SelectedObject[] selObjs, Point2d loc, Point2d screenLoc) {
		// Log the various data points associated with the tap
		Log.d("Map Longpressed", "Loc | x: " + loc.getX() + " y: " + loc.getY() + " z: " + mapControl.getPositionGeo().getZ());
		Log.d("Map Longpressed", "Latlon | lon: " + loc.toDegrees().getX() + " lat: " + loc.toDegrees().getY());
		Log.d("Map Longpressed", "ScreenLoc | x: " + screenLoc.getX() + " y: " + screenLoc.getY());
		if (selObjs != null) {
			Log.d("Map Longpressed", selObjs.toString());
		} else {
			Bundle args = new Bundle();
			args.putString("map_location_lat", String.valueOf(loc.toDegrees().getY()));
			args.putString("map_location_lon", String.valueOf(loc.toDegrees().getX()));
			args.putString("schema_key", "poi"); // TODO: REMOVE HARDCODED KEY, PROVIDE SELECTION!
			mActivity.openFragment(FragmentId.SYNCABLE_OBJECT_CREATE, args);
		}
		super.userDidLongPress(mapController, selObjs, loc, screenLoc);
	}

	@Override
	public void userDidSelect(MapController mapControl, SelectedObject[] selObjs, Point2d loc, Point2d screenLoc) {
		// Log the various data points associated with the selection
		Log.d("Map Selected", "Loc | x: " + loc.getX() + " y: " + loc.getY() + " z: " + mapControl.getPositionGeo().getZ());
		Log.d("Map Selected", "Latlon | lon: " + loc.toDegrees().getX() + " lat: " + loc.toDegrees().getY());
		Log.d("Map Selected", "ScreenLoc | x: " + screenLoc.getX() + " y: " + screenLoc.getY());

		// Handle selection of specific object types
		if (selObjs != null) {
			Log.d("Map Selected", selObjs.toString());
			if (selObjs[0].selObj.getClass().equals(LDLNScreenMarker.class)) {
				LDLNScreenMarker marker = (LDLNScreenMarker) selObjs[0].selObj;
				LDLNMarkerAttributes properties = (LDLNMarkerAttributes) marker.userObject;
				Toast.makeText(mActivity, properties.toString() + "\nlon: " + loc.toDegrees().getX() + "\nlat: " + loc.toDegrees().getY(), Toast.LENGTH_SHORT).show();
			}
		}
		super.userDidSelect(mapControl, selObjs, loc, screenLoc);
	}

	private void insertMarker(double lat, double lon, String title, String description, int icon_id, boolean recenterMap) {
		// Prepare the features needed for constructing the marker
		Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(), icon_id);
		LDLNMarkerAttributes properties = new LDLNMarkerAttributes(title, description);

		// Create the marker and add it to the array to be added to the map
		ScreenMarker marker = new LDLNScreenMarker(lat, lon, 45, icon, properties);

		// Add your marker to the map controller.
		MarkerInfo markerInfo = new MarkerInfo();
		ComponentObject markersComponentObject = mapControl.addScreenMarker(marker, markerInfo, MaplyBaseController.ThreadMode.ThreadAny);

		// Keep list of objects for removal.
		// ComponentObject is your handle to the marker in the map controller.
		// You can use this to enable, disable, and remove your marker from the map.
		markers.add(markersComponentObject);

		// Center the map if asked to
		if (recenterMap) {
			Point2d centerPt = Point2d.FromDegrees(lon, lat);
			mapControl.animatePositionGeo(centerPt.getX(), centerPt.getY(), mapControl.getPositionGeo().getZ(), 0.6);
		}
	}

	void insertMarkersAndCenter() {
		// Insert Markers
		LDLN.listSyncableObjects(mActivity, this, "poi"); // TODO: GET RID OF HARDCODED SCHEMA KEY!
	}

	@Override
	public void onListSyncableObjectsResult(List<PlaintextObject> plaintextObjects) {
		if (plaintextObjects != null) {
			// Remove all existing markers
			while (markers.size() > 0) {
				mapControl.removeObject(markers.remove(0), MaplyBaseController.ThreadMode.ThreadAny);
			}

			// Place new markers
			for (PlaintextObject po : plaintextObjects) {
				HashMap<String, String> hashMap = po.getKeyValueMap();
				if (hashMap.containsKey("Map Location") && !hashMap.get("Map Location").equals("")) {
					String[] mapLocationBits = hashMap.get("Map Location").split(",");
					double lat = Double.parseDouble(mapLocationBits[0]);
					double lon = Double.parseDouble(mapLocationBits[1]);
					String title = hashMap.get("Title");
					String desc = hashMap.get("Note");
					insertMarker(lat, lon, title, desc, R.drawable.ic_city, true);
				}
			}

			// Manually center the map after all stored pins have been created
			double centerLat = Double.valueOf(LDLNProperties.getProperty(mActivity, "map.center.lat"));
			double centerLon = Double.valueOf(LDLNProperties.getProperty(mActivity, "map.center.lon"));
			double centerZoom = Double.valueOf(LDLNProperties.getProperty(mActivity, "map.center.zoom"));
			Point2d centerPt = Point2d.FromDegrees(centerLon, centerLat);
			mapControl.setPositionGeo(centerPt.getX(), centerPt.getY(), centerZoom);
		}
	}

	@Override
	public void onListSyncableObjectsError(String error) {
		Log.e("Map Pins Error", "Error listing syncable objects: " + error);
	}
}