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

package com.google.android.gms.iid;

import android.os.IBinder;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;

public class MessengerCompat implements Parcelable {
    private final Messenger messenger;
    private IMessengerCompat messengerCompat;

    public MessengerCompat(IBinder binder) {
        messenger = new Messenger(binder);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MessengerCompat && ((MessengerCompat) o).getBinder().equals(getBinder());
    }

    public IBinder getBinder() {
        return messenger.getBinder();
    }

    @Override
    public int hashCode() {
        return getBinder().hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(getBinder());
    }

    public static final Creator<MessengerCompat> CREATOR = new Creator<MessengerCompat>() {
        @Override
        public MessengerCompat createFromParcel(Parcel source) {
            IBinder binder = source.readStrongBinder();
            return binder != null ? new MessengerCompat(binder) : null;
        }

        @Override
        public MessengerCompat[] newArray(int size) {
            return new MessengerCompat[size];
        }
    };

}
