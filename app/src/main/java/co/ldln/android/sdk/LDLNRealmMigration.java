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

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by matt on 7/16/16.
 */
class LDLNRealmMigration implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        // DynamicRealm exposes an editable schema
        RealmSchema schema = realm.getSchema();

        // Migrate to version 1: Add a new class.
        // Example:
        // public Person extends RealmObject {
        //     private String name;
        //     private int age;
        //     // getters and setters left out for brevity
        // }
        if (oldVersion == 0) {
            // Make updates to the schema here. For example:

            // schema.create("Person")
            //        .addField("name", String.class)
            //        .addField("age", int.class);

            // Increment so that old versions multiple steps behind get all migrations
            oldVersion++;
        }

        // Migrate to version 2: Add a primary key + object references
        // Example:
        // public Person extends RealmObject {
        //     private String name;
        //     @PrimaryKey
        //     private int age;
        //     private Dog favoriteDog;
        //     private RealmList<Dog> dogs;
        //     // getters and setters left out for brevity
        // }
        if (oldVersion == 1) {
            // Make updates to the schema here. For example:

            //schema.get("Person")
            //        .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
            //        .addRealmObjectField("favoriteDog", schema.get("Dog"))
            //        .addRealmListField("dogs", schema.get("Dog"));

            // Increment so that old versions multiple steps behind get all migrations
            oldVersion++;
        }
    }
}
