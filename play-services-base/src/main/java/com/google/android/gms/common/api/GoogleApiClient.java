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

package com.google.android.gms.common.api;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;

import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.ApiClientSettings;
import org.microg.gms.common.api.GoogleApiClientImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * The main entry point for Google Play services integration.
 * <p/>
 * GoogleApiClient is used with a variety of static methods. Some of these methods require that
 * GoogleApiClient be connected, some will queue up calls before GoogleApiClient is connected;
 * check the specific API documentation to determine whether you need to be connected.
 * <p/>
 * Before any operation is executed, the GoogleApiClient must be connected using the
 * {@link #connect()} method. The client is not considered connected until the
 * {@link ConnectionCallbacks#onConnected(Bundle)} callback has been called.
 * <p/>
 * When your app is done using this client, call {@link #disconnect()}, even if the async result
 * from {@link #connect()} has not yet been delivered.
 * <p/>
 * You should instantiate a client object in your Activity's {@link Activity#onCreate(Bundle)}
 * method and then call {@link #connect()} in {@link Activity#onStart()} and {@link #disconnect()}
 * in {@link Activity#onStop()}, regardless of the state.
 */
@PublicApi
@Deprecated
public interface GoogleApiClient {

    /**
     * Connects the client to Google Play services. Blocks until the connection is set or failed or
     * has timed out. This is not allowed on the UI thread.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument
     * @return the result of the connection
     */
    ConnectionResult blockingConnect(long timeout, TimeUnit unit);

    /**
     * Clears the account selected by the user and reconnects the client asking the user to pick an
     * account again if {@link Builder#useDefaultAccount()} was set.
     *
     * @return the pending result is fired once the default account has been cleared, but before
     * the client is reconnected - for that {@link ConnectionCallbacks} can be used.
     */
    PendingResult<Status> clearDefaultAccountAndReconnect();

    /**
     * Connects the client to Google Play services. This method returns immediately, and connects
     * to the service in the background. If the connection is successful,
     * {@link ConnectionCallbacks#onConnected(Bundle)} is called and enqueued items are executed.
     * On a failure, {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)} is
     * called.
     */
    void connect();

    /**
     * Closes the connection to Google Play services. No calls can be made using this client after
     * calling this method. Any method calls that haven't executed yet will be canceled. That is
     * {@link ResultCallback#onResult(Result)} won't be called, if connection to the service hasn't
     * been established yet all calls already made will be canceled.
     *
     * @see #connect()
     */
    void disconnect();

    /**
     * Checks if the client is currently connected to the service, so that requests to other
     * methods will succeed. Applications should guard client actions caused by the user with a
     * call to this method.
     *
     * @return {@code true} if the client is connected to the service.
     */
    boolean isConnected();

    /**
     * Checks if the client is attempting to connect to the service.
     *
     * @return {@code true} if the client is attempting to connect to the service.
     */
    boolean isConnecting();

    /**
     * Returns {@code true} if the specified listener is currently registered to receive connection
     * events.
     *
     * @param listener The listener to check for.
     * @return {@code true} if the specified listener is currently registered to receive connection
     * events.
     * @see #registerConnectionCallbacks(ConnectionCallbacks)
     * @see #unregisterConnectionCallbacks(ConnectionCallbacks)
     */
    boolean isConnectionCallbacksRegistered(ConnectionCallbacks listener);

    /**
     * Returns {@code true} if the specified listener is currently registered to receive connection
     * failed events.
     *
     * @param listener The listener to check for.
     * @return {@code true} if the specified listener is currently registered to receive connection
     * failed events.
     * @see #registerConnectionFailedListener(OnConnectionFailedListener)
     * @see #unregisterConnectionFailedListener(OnConnectionFailedListener)
     */
    boolean isConnectionFailedListenerRegistered(OnConnectionFailedListener listener);

    /**
     * Closes the current connection to Google Play services and creates a new connection.
     * <p/>
     * This method closes the current connection then returns immediately and reconnects to the
     * service in the background.
     * <p/>
     * After calling this method, your application will receive
     * {@link ConnectionCallbacks#onConnected(Bundle)} if the connection is successful, or
     * {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)} if the connection
     * failed.
     *
     * @see #connect()
     * @see #disconnect()
     */
    void reconnect();

    /**
     * Registers a listener to receive connection events from this {@link GoogleApiClient}. If the
     * service is already connected, the listener's {@link ConnectionCallbacks#onConnected(Bundle)}
     * method will be called immediately. Applications should balance calls to this method with
     * calls to {@link #unregisterConnectionCallbacks(ConnectionCallbacks)} to avoid leaking
     * resources.
     * <p/>
     * If the specified listener is already registered to receive connection events, this method
     * will not add a duplicate entry for the same listener, but will still call the listener's
     * {@link ConnectionCallbacks#onConnected(Bundle)} method if currently connected.
     * <p/>
     * Note that the order of messages received here may not be stable, so clients should not rely
     * on the order that multiple listeners receive events in.
     *
     * @param listener the listener where the results of the asynchronous {@link #connect()} call
     *                 are delivered.
     */
    void registerConnectionCallbacks(ConnectionCallbacks listener);

    /**
     * Registers a listener to receive connection failed events from this {@link GoogleApiClient}.
     * Unlike {@link #registerConnectionCallbacks(ConnectionCallbacks)}, if the service is not
     * already connected, the listener's
     * {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)} method will not be
     * called immediately. Applications should balance calls to this method with calls to
     * {@link #unregisterConnectionFailedListener(OnConnectionFailedListener)} to avoid leaking
     * resources.
     * <p/>
     * If the specified listener is already registered to receive connection failed events, this
     * method will not add a duplicate entry for the same listener.
     * <p/>
     * Note that the order of messages received here may not be stable, so clients should not rely
     * on the order that multiple listeners receive events in.
     *
     * @param listener the listener where the results of the asynchronous {@link #connect()} call
     *                 are delivered.
     */
    void registerConnectionFailedListener(OnConnectionFailedListener listener);

    /**
     * Disconnects the client and stops automatic lifecycle management. Use this before creating a
     * new client (which might be necessary when switching accounts, changing the set of used APIs
     * etc.).
     * <p/>
     * This method must be called from the main thread.
     *
     * @param lifecycleActivity the activity managing the client's lifecycle.
     * @throws IllegalStateException if called from outside of the main thread.
     * @see #enableAutoManage(int, OnConnectionFailedListener)
     */
    void stopAutoManager(FragmentActivity lifecycleActivity) throws IllegalStateException;

    /**
     * Removes a connection listener from this {@link GoogleApiClient}. Note that removing a
     * listener does not generate any callbacks.
     * <p/>
     * If the specified listener is not currently registered to receive connection events, this
     * method will have no effect.
     *
     * @param listener the listener to unregister.
     */
    void unregisterConnectionCallbacks(ConnectionCallbacks listener);

    /**
     * Removes a connection failed listener from the {@link GoogleApiClient}. Note that removing a
     * listener does not generate any callbacks.
     * <p/>
     * If the specified listener is not currently registered to receive connection failed events,
     * this method will have no effect.
     *
     * @param listener the listener to unregister.
     */
    void unregisterConnectionFailedListener(OnConnectionFailedListener listener);

    /**
     * Builder to configure a {@link GoogleApiClient}.
     */
    @PublicApi
    class Builder {
        private final Context context;
        private final Map<Api, Api.ApiOptions> apis = new HashMap<>();
        private final Set<ConnectionCallbacks> connectionCallbacks = new HashSet<>();
        private final Set<OnConnectionFailedListener> connectionFailedListeners = new HashSet<>();
        private final Set<String> scopes = new HashSet<>();
        private int clientId = -1;
        private Looper looper;

        /**
         * Builder to help construct the {@link GoogleApiClient} object.
         *
         * @param context The context to use for the connection.
         */
        public Builder(Context context) {
            this.context = context;
            this.looper = context.getMainLooper();
        }

        /**
         * Builder to help construct the {@link GoogleApiClient} object.
         *
         * @param context                  The context to use for the connection.
         * @param connectedListener        The listener where the results of the asynchronous
         *                                 {@link #connect()} call are delivered.
         * @param connectionFailedListener The listener which will be notified if the connection
         *                                 attempt fails.
         */
        public Builder(Context context, ConnectionCallbacks connectedListener,
                       OnConnectionFailedListener connectionFailedListener) {
            this(context);
            addConnectionCallbacks(connectedListener);
            addOnConnectionFailedListener(connectionFailedListener);
        }

        /**
         * Specify which Apis are requested by your app. See {@link Api} for more information.
         *
         * @param api     The Api requested by your app.
         * @param options Any additional parameters required for the specific AP
         * @see Api
         */
        public <O extends Api.ApiOptions.HasOptions> Builder addApi(Api<O> api, O options) {
            apis.put(api, options);
            return this;
        }

        /**
         * Specify which Apis are requested by your app. See {@link Api} for more information.
         *
         * @param api The Api requested by your app.
         * @see Api
         */
        public Builder addApi(Api<? extends Api.ApiOptions.NotRequiredOptions> api) {
            apis.put(api, null);
            return this;
        }

        /**
         * Registers a listener to receive connection events from this {@link GoogleApiClient}.
         * Applications should balance calls to this method with calls to
         * {@link #unregisterConnectionCallbacks(ConnectionCallbacks)} to avoid
         * leaking resources.
         * <p/>
         * If the specified listener is already registered to receive connection events, this
         * method will not add a duplicate entry for the same listener.
         * <p/>
         * Note that the order of messages received here may not be stable, so clients should not
         * rely on the order that multiple listeners receive events in.
         *
         * @param listener the listener where the results of the asynchronous {@link #connect()}
         *                 call are delivered.
         */
        public void addConnectionCallbacks(ConnectionCallbacks listener) {
            connectionCallbacks.add(listener);
        }

        /**
         * Adds a listener to register to receive connection failed events from this
         * {@link GoogleApiClient}. Applications should balance calls to this method with calls to
         * {@link #unregisterConnectionFailedListener(OnConnectionFailedListener)} to avoid
         * leaking resources.
         * <p/>
         * If the specified listener is already registered to receive connection failed events,
         * this method will not add a duplicate entry for the same listener.
         * <p/>
         * Note that the order of messages received here may not be stable, so clients should not
         * rely on the order that multiple listeners receive events in.
         *
         * @param listener the listener where the results of the asynchronous {@link #connect()}
         *                 call are delivered.
         */
        public void addOnConnectionFailedListener(OnConnectionFailedListener listener) {
            connectionFailedListeners.add(listener);
        }

        /**
         * Builds a new {@link GoogleApiClient} object for communicating with the Google APIs.
         *
         * @return The {@link GoogleApiClient} object.
         */
        public GoogleApiClient build() {
            return new GoogleApiClientImpl(context, looper, getClientSettings(), apis, connectionCallbacks, connectionFailedListeners);
        }

        private ApiClientSettings getClientSettings() {
            return null;
        }

        /**
         * Specify that the default account should be used when connecting to services.
         */
        public Builder useDefaultAccount() {
            return this;
        }
    }

    /**
     * Provides callbacks that are called when the client is connected or disconnected from the
     * service. Most applications implement {@link #onConnected(Bundle)} to start making requests.
     */
    @PublicApi
    @Deprecated
    interface ConnectionCallbacks extends org.microg.gms.common.api.ConnectionCallbacks {
    }

    /**
     * Provides callbacks for scenarios that result in a failed attempt to connect the client to
     * the service. See {@link ConnectionResult} for a list of error codes and suggestions for
     * resolution.
     */
    @PublicApi
    @Deprecated
    interface OnConnectionFailedListener extends org.microg.gms.common.api.OnConnectionFailedListener {
    }
}
