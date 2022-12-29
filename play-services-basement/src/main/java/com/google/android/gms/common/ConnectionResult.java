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

package com.google.android.gms.common;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentSender;
import android.text.TextUtils;

import java.util.Arrays;

/**
 * Contains all possible error codes for when a client fails to connect to Google Play services.
 * These error codes are used by {@link GoogleApiClient.OnConnectionFailedListener}.
 */
public class ConnectionResult {
    /**
     * The connection was successful.
     */
    public static final int SUCCESS = 0;

    /**
     * The Drive API requires external storage (such as an SD card), but no external storage is
     * mounted. This error is recoverable if the user installs external storage (if none is
     * present) and ensures that it is mounted (which may involve disabling USB storage mode,
     * formatting the storage, or other initialization as required by the device).
     * <p/>
     * This error should never be returned on a device with emulated external storage. On devices
     * with emulated external storage, the emulated "external storage" is always present regardless
     * of whether the device also has removable storage.
     */
    @Deprecated
    public static final int DRIVE_EXTERNAL_STORAGE_REQUIRED = 1500;

    private final int statusCode;
    private final PendingIntent pendingIntent;
    private final String message;

    /**
     * Creates a connection result.
     *
     * @param statusCode    The status code.
     * @param pendingIntent A pending intent that will resolve the issue when started, or null.
     * @param message       An additional error message for the connection result, or null.
     */
    public ConnectionResult(int statusCode, PendingIntent pendingIntent, String message) {
        this.statusCode = statusCode;
        this.pendingIntent = pendingIntent;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ConnectionResult)) {
            return false;
        } else {
            ConnectionResult r = (ConnectionResult)o;
            return statusCode == r.statusCode && pendingIntent == null ? r.pendingIntent == null : pendingIntent.equals(r.pendingIntent) && TextUtils.equals(message, r.message);
        }
    }

    /**
     * Returns an error message for connection result.
     *
     * @return the message
     */
    public String getErrorMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{statusCode, pendingIntent, message});
    }

    /**
     * Returns {@code true} if calling {@link #startResolutionForResult(Activity, int)} will start
     * any intents requiring user interaction.
     *
     * @return {@code true} if there is a resolution that can be started.
     */
    public boolean hasResolution() {
        return statusCode != 0 && pendingIntent != null;
    }

    /**
     *
     * @param activity    An Activity context to use to resolve the issue. The activity's
     *                     method will be invoked after the user
     *                    is done. If the resultCode is {@link Activity#RESULT_OK}, the application
     *                    should try to connect again.
     * @param requestCode The request code to pass to .
     * @throws IntentSender.SendIntentException If the resolution intent has been canceled or is no
     *                                          longer able to execute the request.
     */
    public void startResolutionForResult(Activity activity, int requestCode) throws
            IntentSender.SendIntentException {
        if (hasResolution()) {
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(), requestCode, null, 0, 0, 0);
        }
    }
}
