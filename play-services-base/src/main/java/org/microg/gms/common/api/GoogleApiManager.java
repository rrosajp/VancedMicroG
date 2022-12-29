/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GoogleApiManager {
    @SuppressLint("StaticFieldLeak")
    private static GoogleApiManager instance;
    private final Context context;
    private final Map<ApiInstance, ApiClient> clientMap = new HashMap<>();
    private final Map<ApiInstance, List<WaitingApiCall<?>>> waitingApiCallMap = new HashMap<>();

    private GoogleApiManager(Context context) {
        this.context = context;
    }

    public synchronized static GoogleApiManager getInstance(Context context) {
        if (instance == null) instance = new GoogleApiManager(context);
        return instance;
    }

    private synchronized <O extends Api.ApiOptions, A extends ApiClient> A clientForApi(GoogleApi<O> api) {
        ApiInstance apiInstance = new ApiInstance(api);
        if (clientMap.containsKey(apiInstance)) {
            return (A) clientMap.get(apiInstance);
        } else {
            ApiClient client = api.api.getBuilder().build(api.getOptions(), context, context.getMainLooper(), null, new ConnectionCallback(apiInstance), new ConnectionFailedListener(apiInstance));
            clientMap.put(apiInstance, client);
            waitingApiCallMap.put(apiInstance, new ArrayList<>());
            return (A) client;
        }
    }

    public synchronized <O extends Api.ApiOptions, R, A extends ApiClient> void scheduleTask(GoogleApi<O> api, PendingGoogleApiCall<R, A> apiCall, TaskCompletionSource<R> completionSource) {
        A client = clientForApi(api);
        boolean connecting = client.isConnecting();
        boolean connected = client.isConnected();
        if (connected) {
            apiCall.execute(client, completionSource);
        } else {
            Objects.requireNonNull(waitingApiCallMap.get(new ApiInstance(api))).add(new WaitingApiCall<>((PendingGoogleApiCall<R, ApiClient>) apiCall, completionSource));
            if (!connecting) {
                client.connect();
            }
        }
    }

    private synchronized void onInstanceConnected(ApiInstance apiInstance) {
        List<WaitingApiCall<?>> waitingApiCalls = waitingApiCallMap.get(apiInstance);
        assert waitingApiCalls != null;
        for (WaitingApiCall<?> waitingApiCall : waitingApiCalls) {
            waitingApiCall.execute(clientMap.get(apiInstance));
        }
        waitingApiCalls.clear();
    }

    private synchronized void onInstanceSuspended() {

    }

    private synchronized void onInstanceFailed(ApiInstance apiInstance, ConnectionResult result) {
        List<WaitingApiCall<?>> waitingApiCalls = waitingApiCallMap.get(apiInstance);
        assert waitingApiCalls != null;
        for (WaitingApiCall<?> waitingApiCall : waitingApiCalls) {
            waitingApiCall.failed(new RuntimeException(result.getErrorMessage()));
        }
        waitingApiCalls.clear();
    }

    private class ConnectionCallback implements ConnectionCallbacks {
        private final ApiInstance apiInstance;

        public ConnectionCallback(ApiInstance apiInstance) {
            this.apiInstance = apiInstance;
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            onInstanceConnected(apiInstance);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            onInstanceSuspended();
        }
    }

    private class ConnectionFailedListener implements OnConnectionFailedListener {
        private final ApiInstance apiInstance;

        public ConnectionFailedListener(ApiInstance apiInstance) {
            this.apiInstance = apiInstance;
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            onInstanceFailed(apiInstance, result);
        }
    }

    private static class WaitingApiCall<R> {
        private final PendingGoogleApiCall<R, ApiClient> apiCall;
        private final TaskCompletionSource<R> completionSource;

        public WaitingApiCall(PendingGoogleApiCall<R, ApiClient> apiCall, TaskCompletionSource<R> completionSource) {
            this.apiCall = apiCall;
            this.completionSource = completionSource;
        }

        public void execute(ApiClient client) {
            apiCall.execute(client, completionSource);
        }

        public void failed(Exception e) {
            completionSource.setException(e);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WaitingApiCall<?> that = (WaitingApiCall<?>) o;

            if (!Objects.equals(apiCall, that.apiCall)) return false;
            return Objects.equals(completionSource, that.completionSource);
        }

        @Override
        public int hashCode() {
            int result = apiCall != null ? apiCall.hashCode() : 0;
            result = 31 * result + (completionSource != null ? completionSource.hashCode() : 0);
            return result;
        }
    }

    private static class ApiInstance {
        private final Class<?> apiClass;
        private final Api.ApiOptions apiOptions;

        public ApiInstance(Class<?> apiClass, Api.ApiOptions apiOptions) {
            this.apiClass = apiClass;
            this.apiOptions = apiOptions;
        }

        public ApiInstance(GoogleApi<?> api) {
            this(api.getClass(), api.getOptions());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ApiInstance that = (ApiInstance) o;

            if (!Objects.equals(apiClass, that.apiClass)) return false;
            return Objects.equals(apiOptions, that.apiOptions);
        }

        @Override
        public int hashCode() {
            int result = apiClass != null ? apiClass.hashCode() : 0;
            result = 31 * result + (apiOptions != null ? apiOptions.hashCode() : 0);
            return result;
        }
    }
}
