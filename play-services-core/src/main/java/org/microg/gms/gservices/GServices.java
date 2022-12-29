/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.gservices;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class GServices {
    public static final Uri CONTENT_URI = Uri.parse("content://com.mgoogle.android.gsf.gservices");
    public static final Uri MAIN_URI = Uri.parse("content://com.mgoogle.android.gsf.gservices/main");

    public static void setString(ContentResolver resolver, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("name", key);
        values.put("value", value);
        resolver.update(MAIN_URI, values, null, null);
    }

    public static String getString(ContentResolver resolver, String key) {
        return getString(resolver, key, null);
    }

    public static String getString(ContentResolver resolver, String key, String defaultValue) {
        String result = defaultValue;
        Cursor cursor = resolver.query(CONTENT_URI, null, null, new String[]{key}, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getString(1);
            }
            cursor.close();
        }
        return result;
    }

}
