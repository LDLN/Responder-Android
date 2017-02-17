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
import android.graphics.Typeface;
import android.util.Log;

import com.mousebird.maply.AttrDictionary;
import com.mousebird.maply.ComponentObject;
import com.mousebird.maply.LabelInfo;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.MaplyTileID;
import com.mousebird.maply.Point2d;
import com.mousebird.maply.ScreenLabel;
import com.mousebird.maply.VectorInfo;
import com.mousebird.maply.VectorObject;
import com.mousebird.maply.VectorStyle;
import com.mousebird.maply.VectorStyleInterface;
import com.mousebird.maply.MaplyBaseController.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class LDLNVectorSimpleStyleGenerator implements VectorStyleInterface {
    ThreadMode threadMode;
    MaplyBaseController controller;
    Context context;
    HashMap<String, LDLNVectorSimpleStyleGenerator.VectorStyleSimple> stylesByUUID;
    HashMap<String, LDLNVectorSimpleStyleGenerator.VectorStyleSimple> stylesByLayerName;

    public LDLNVectorSimpleStyleGenerator(Context context, MaplyBaseController inControl) {
        this.threadMode = ThreadMode.ThreadCurrent;
        this.controller = null;
        this.stylesByUUID = new HashMap();
        this.stylesByLayerName = new HashMap();
        this.context = context;
        this.controller = inControl;
    }

    public VectorStyle[] stylesForFeature(AttrDictionary attrs, MaplyTileID tileID, String layerName, MaplyBaseController controller) {
        // TODO: cache these up front to avoid race conditions!
        LDLNVectorSimpleStyleGenerator.VectorStyleSimple style = this.stylesByLayerName.get(layerName);
        if(style == null) {
            int layerOrder = attrs.getInt("layer_order").intValue();
            int geomType = attrs.getInt("geometry_type").intValue();
            switch(geomType) {
                case 1:
                    int LabelPriorityDefault = '\uea60'; // LabelInfo.LabelPriorityDefault is restricted to whirlyglobe package
                    style = new LDLNVectorSimpleStyleGenerator.VectorStyleSimplePoint(LabelPriorityDefault + layerOrder, layerName);
                    break;
                case 2:
                    style = new LDLNVectorSimpleStyleGenerator.VectorStyleSimpleLinear(VectorInfo.VectorPriorityDefault + layerOrder, layerName);
                    break;
                case 3:
                    style = new LDLNVectorSimpleStyleGenerator.VectorStyleSimplePolygon(VectorInfo.VectorPriorityDefault + layerOrder, layerName);
                    break;
                default:
                    Log.e("Map Style", "geomType " + geomType + " not specified!");
            }

            this.stylesByLayerName.put(layerName, style);
            this.stylesByUUID.put(style.getUuid(), style);
        }

        return new VectorStyle[]{style};
    }

    public boolean layerShouldDisplay(String layerName, MaplyTileID tileID) {
        return true;
    }

    public VectorStyle styleForUUID(String uuid, MaplyBaseController controller) {
        VectorStyle style = this.stylesByUUID.get(uuid);
        return style;
    }

    /* Polygons include: water, landuse, landuse_overlay, aeroway, building */
    public class VectorStyleSimplePolygon extends LDLNVectorSimpleStyleGenerator.VectorStyleSimple {
        float r;
        float g;
        float b;
        float a;
        boolean f;

        VectorStyleSimplePolygon(int priority, String layerName) {
            super();
            this.drawPriority = priority;

            // Default colors
            this.r = 0.0F / 255.0F; // Default
            this.g = 0.0F / 255.0F; // to
            this.b = 0.0F / 255.0F; // black
            this.a = 1.0F;
            this.f = true;

            // Overridden colors from properties file
            String layerRgb = LDLNProperties.getProperty(context, "map.colors.poly." + layerName);
            if (layerRgb != null && layerRgb.replaceAll("[^,]","").length() + 1 == 5) {
                String[] layerRgbBits = layerRgb.split(",");
                this.r = Float.valueOf(layerRgbBits[0]) / 255.0F;
                this.g = Float.valueOf(layerRgbBits[1]) / 255.0F;
                this.b = Float.valueOf(layerRgbBits[2]) / 255.0F;
                this.a = Float.valueOf(layerRgbBits[3]);
                this.f = Boolean.valueOf(layerRgbBits[4]);
            } else {
                Log.e("Map Style", layerName + " POLYGON style not defined!");
            }
        }

        public ComponentObject[] buildObjects(List<VectorObject> vecObjs, MaplyTileID tileID, MaplyBaseController controller) {
            VectorInfo vecInfo = new VectorInfo();
            vecInfo.disposeAfterUse = true;
            vecInfo.setColor(r, g, b, a);
            vecInfo.setFilled(f);
            vecInfo.setDrawPriority(this.drawPriority);
            vecInfo.setEnable(false);
            ComponentObject compObj = controller.addVectors(vecObjs, vecInfo, LDLNVectorSimpleStyleGenerator.this.threadMode);
            return compObj != null?new ComponentObject[]{compObj}:null;
        }
    }

    /* Lines include: admin, road, waterway, aeroway, barrier_line */
    public class VectorStyleSimpleLinear extends LDLNVectorSimpleStyleGenerator.VectorStyleSimple {
        float r;
        float g;
        float b;
        float a;
        float w;

        VectorStyleSimpleLinear(int priority, String layerName) {
            super();
            this.drawPriority = priority;

            // Default Colors
            this.r = 0.0F / 255.0F; // Default
            this.g = 0.0F / 255.0F; // to
            this.b = 0.0F / 255.0F; // black
            this.a = 1.0F;
            this.w = 4.0F;

            // Overridden colors from properties file
            String layerRgb = LDLNProperties.getProperty(context, "map.colors.line." + layerName);
            if (layerRgb != null && layerRgb.replaceAll("[^,]","").length() + 1 == 5) {
                String[] layerRgbBits = layerRgb.split(",");
                this.r = Float.valueOf(layerRgbBits[0]) / 255.0F;
                this.g = Float.valueOf(layerRgbBits[1]) / 255.0F;
                this.b = Float.valueOf(layerRgbBits[2]) / 255.0F;
                this.a = Float.valueOf(layerRgbBits[3]);
                this.w = Float.valueOf(layerRgbBits[4]);
            } else {
                Log.e("Map Style", layerName + " LINE style not defined!");
            }
        }

        public ComponentObject[] buildObjects(List<VectorObject> vecObjs, MaplyTileID tileID, MaplyBaseController controller) {
            VectorInfo vecInfo = new VectorInfo();
            vecInfo.disposeAfterUse = true;
            vecInfo.setColor(r, g, b, a);
            vecInfo.setLineWidth(w);
            vecInfo.setFilled(false);
            vecInfo.setDrawPriority(this.drawPriority);
            vecInfo.setEnable(false);
            ComponentObject compObj = controller.addVectors(vecObjs, vecInfo, LDLNVectorSimpleStyleGenerator.this.threadMode);
            return compObj != null?new ComponentObject[]{compObj}:null;
        }
    }

    /* Points include: country_label, marine_label, place_label, state_label, road_label, airport_label, water_label,
       motorway_junction, mountain_peak_label, rail_station_label, poi_label, housenum_label */
    public class VectorStyleSimplePoint extends LDLNVectorSimpleStyleGenerator.VectorStyleSimple {
        LabelInfo labelInfo;

        VectorStyleSimplePoint(int inPriority, String layerName) {
            super();

            // Default Colors
            float r = 0.0F / 255.0F; // Default
            float g = 0.0F / 255.0F; // to
            float b = 0.0F / 255.0F; // black
            float a = 1.0F;
            float fontSize = 20.0F;
            Typeface typeface = Typeface.DEFAULT;

            // Overridden colors from properties file
            String layerRgb = LDLNProperties.getProperty(context, "map.colors.point." + layerName);
            if (layerRgb != null && layerRgb.replaceAll("[^,]","").length() + 1 == 5) {
                String[] layerRgbBits = layerRgb.split(",");
                r = Float.valueOf(layerRgbBits[0]) / 255.0F;
                g = Float.valueOf(layerRgbBits[1]) / 255.0F;
                b = Float.valueOf(layerRgbBits[2]) / 255.0F;
                a = Float.valueOf(layerRgbBits[3]);
                fontSize = Float.valueOf(layerRgbBits[4]);
            } else {
                Log.e("Map Style", layerName + " POINT style not defined!");
            }

            this.drawPriority = inPriority;
            this.labelInfo = new LabelInfo();
            this.labelInfo.setFontSize(fontSize);
            this.labelInfo.setTextColor(r, g, b, a);
            this.labelInfo.setTypeface(typeface);
            this.labelInfo.setDrawPriority(this.drawPriority);
            this.labelInfo.setEnable(false);
            // Could also set background color and outline color of labelInfo
        }

        public ComponentObject[] buildObjects(List<VectorObject> vecObjs, MaplyTileID tileID, MaplyBaseController controller) {
            ArrayList labels = new ArrayList();
            Iterator compObj = vecObjs.iterator();

            while(compObj.hasNext()) {
                VectorObject point = (VectorObject)compObj.next();
                String name = point.getAttributes().getString("name");
                Point2d pt = point.centroid();
                if(pt != null) {
                    ScreenLabel label = new ScreenLabel();
                    label.text = name != null?name:".";
                    label.loc = pt;
                    labels.add(label);
                }
            }

            ComponentObject compObj1 = controller.addScreenLabels(labels, this.labelInfo, LDLNVectorSimpleStyleGenerator.this.threadMode);
            if(compObj1 != null) {
                return new ComponentObject[]{compObj1};
            } else {
                return null;
            }
        }
    }

    public abstract class VectorStyleSimple implements VectorStyle {
        String uuid = null;
        public int drawPriority;

        public VectorStyleSimple() {
            this.drawPriority = VectorInfo.VectorPriorityDefault;
        }

        public String getUuid() {
            if(this.uuid == null) {
                this.uuid = " " + Math.random() * 1000000.0D + Math.random() * 10000.0D;
            }

            return this.uuid;
        }

        public boolean geomIsAdditive() {
            return false;
        }
    }
}
