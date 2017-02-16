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

import android.graphics.Bitmap;

import com.mousebird.maply.Point2d;
import com.mousebird.maply.ScreenMarker;

/**
 * Created by matt on 8/28/16.
 */
public class LDLNScreenMarker extends ScreenMarker {

    public LDLNScreenMarker(double lat, double lon, int size, Bitmap image, LDLNMarkerAttributes properties) {
        this.loc = Point2d.FromDegrees(lon, lat); // Longitude, Latitude
        this.image = image;
        this.size = new Point2d(size, size);
        this.selectable = true;
        this.userObject = properties;
    }
}
