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

package org.microg.gms;

import android.accounts.Account;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;
import com.google.android.gms.common.internal.ValidateAccountRequest;

import org.microg.gms.common.GmsService;

import java.util.EnumSet;

public abstract class AbstractGmsServiceBroker extends IGmsServiceBroker.Stub {
    private static final String TAG = "GmsServiceBroker";
    private final EnumSet<GmsService> supportedServices;

    public AbstractGmsServiceBroker(EnumSet<GmsService> supportedServices) {
        this.supportedServices = supportedServices;
    }

    @Deprecated
    @Override
    public void getPeopleService(IGmsCallbacks callback, int versionCode, String packageName,
                                 Bundle params) throws RemoteException {
        callGetService(GmsService.PEOPLE, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getGoogleIdentityService(IGmsCallbacks callback, int versionCode,
                                         String packageName, Bundle params) throws RemoteException {
        callGetService(GmsService.ACCOUNT, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getAddressService(IGmsCallbacks callback, int versionCode, String packageName)
            throws RemoteException {
        callGetService(callback, versionCode, packageName);
    }

    private void callGetService(IGmsCallbacks callback, int gmsVersion,
                                String packageName) throws RemoteException {
        callGetService(GmsService.ADDRESS, callback, gmsVersion, packageName, null);
    }

    private void callGetService(GmsService service, IGmsCallbacks callback, int gmsVersion,
                                String packageName, Bundle extras) throws RemoteException {
        callGetService(service, callback, gmsVersion, packageName, extras, null, null);
    }

    private void callGetService(GmsService service, IGmsCallbacks callback, int gmsVersion, String packageName, Bundle extras, String accountName, String[] scopes) throws RemoteException {
        GetServiceRequest request = new GetServiceRequest(service.SERVICE_ID);
        request.gmsVersion = gmsVersion;
        request.packageName = packageName;
        request.extras = extras;
        request.account = accountName == null ? null : new Account(accountName, "com.mgoogle");
        request.scopes = scopes == null ? null : scopesFromStringArray(scopes);
        getService(callback, request);
    }

    private Scope[] scopesFromStringArray(String[] arr) {
        Scope[] scopes = new Scope[arr.length];
        for (int i = 0; i < arr.length; i++) {
            scopes[i] = new Scope(arr[i]);
        }
        return scopes;
    }

    @Override
    public void getService(IGmsCallbacks callback, GetServiceRequest request) throws RemoteException {
        GmsService gmsService = GmsService.byServiceId(request.serviceId);
        if ((supportedServices.contains(gmsService)) || supportedServices.contains(GmsService.ANY)) {
            handleServiceRequest(callback, request, gmsService);
        } else {
            Log.d(TAG, "Service not supported: " + request);
            throw new IllegalArgumentException("Service not supported: " + request.serviceId);
        }
    }

    public abstract void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException;

    @Override
    public void validateAccount(IGmsCallbacks callback, ValidateAccountRequest request) {
        throw new IllegalArgumentException("ValidateAccountRequest not supported");
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
