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

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by matt on 8/29/16.
 */
// Inspired by: http://pillsfromtheweb.blogspot.it/2014/09/properties-file-in-android.html
public class LDLNProperties {
    private static Properties properties;

    public static String getProperty(Context context, String key) {
        try {
            if (properties == null) {
                properties = new Properties();
                InputStream inputStream = context.getAssets().open("LDLN.properties");
                properties.load(inputStream);
            }

            return properties.getProperty(key);
        } catch (IOException e) {
            Log.e("Properties read error", "for key " + key + ": " + e.getMessage());
        }
        return null;
    }
}
