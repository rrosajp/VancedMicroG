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

package org.microg.gms.auth;

import static android.accounts.AccountManager.KEY_ACCOUNTS;
import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_CALLER_PID;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.auth.IAuthManagerService;
import com.google.android.gms.auth.AccountChangeEventsRequest;
import com.google.android.gms.auth.AccountChangeEventsResponse;
import com.google.android.gms.auth.GetHubTokenInternalResponse;
import com.google.android.gms.auth.GetHubTokenRequest;
import com.google.android.gms.auth.HasCababilitiesRequest;
import com.google.android.gms.auth.TokenData;
import com.google.android.gms.common.api.Scope;

import org.microg.gms.common.PackageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AuthManagerServiceImpl extends IAuthManagerService.Stub {
    private static final String TAG = "GmsAuthManagerSvc";

    public static final String KEY_ACCOUNT_FEATURES = "account_features";
    public static final String KEY_CALLER_UID = "callerUid";
    public static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
    public static final String KEY_CLIENT_PACKAGE_NAME = "clientPackageName";
    public static final String KEY_HANDLE_NOTIFICATION = "handle_notification";

    public static final String KEY_ERROR = "Error";

    private final Context context;

    public AuthManagerServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public Bundle getToken(String accountName, String scope, Bundle extras) {
        return getTokenWithAccount(new Account(accountName, AuthConstants.DEFAULT_ACCOUNT_TYPE), scope, extras);
    }

    private List<Scope> getScopes(String scope) {
        if (!scope.startsWith("oauth2:")) return null;
        String[] strings = scope.substring(7).split(" ");
        List<Scope> res = new ArrayList<>();
        for (String string : strings) {
            res.add(new Scope(string));
        }
        return res;
    }

    @Override
    public AccountChangeEventsResponse getChangeEvents(AccountChangeEventsRequest request) {
        return new AccountChangeEventsResponse();
    }

    @Override
    public Bundle getTokenWithAccount(Account account, String scope, Bundle extras) {
        String packageName = extras.getString(KEY_ANDROID_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty())
            packageName = extras.getString(KEY_CLIENT_PACKAGE_NAME);
        packageName = PackageUtils.getAndCheckCallingPackage(context, packageName, extras.getInt(KEY_CALLER_UID, 0), extras.getInt(KEY_CALLER_PID, 0));
        boolean notify = extras.getBoolean(KEY_HANDLE_NOTIFICATION, false);

        Log.d(TAG, "getToken: account:" + account.name + " scope:" + scope + " extras:" + extras + ", notify: " + notify);

        /*
         * TODO: This scope seems to be invalid (according to https://developers.google.com/oauthplayground/),
         * but is used in some applications anyway. Removing it is unlikely a good solution, but works for now.
         */
        scope = scope.replace("https://www.googleapis.com/auth/identity.plus.page.impersonation ", "");

        assert packageName != null;
        AuthManager authManager = new AuthManager(context, account.name, packageName, scope);
        Bundle result = new Bundle();
        result.putString(KEY_ACCOUNT_NAME, account.name);
        result.putString(KEY_ACCOUNT_TYPE, authManager.getAccountType());
        if (!authManager.accountExists()) {
            result.putString(KEY_ERROR, "NetworkError");
            return result;
        }
        try {
            AuthResponse res = authManager.requestAuth(false);
            if (res.auth != null) {
                Log.d(TAG, "getToken: " + res);
                result.putString(KEY_AUTHTOKEN, res.auth);
                Bundle details = new Bundle();
                details.putParcelable("TokenData", new TokenData(res.auth, res.expiry, scope.startsWith("oauth2:"), getScopes(scope)));
                result.putBundle("tokenDetails", details);
                result.putString(KEY_ERROR, "OK");
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            result.putString(KEY_ERROR, "NetworkError");
        }
        return result;
    }

    @Override
    public Bundle getAccounts(Bundle extras) {
        PackageUtils.assertExtendedAccess(context);
        String[] accountFeatures = extras.getStringArray(KEY_ACCOUNT_FEATURES);
        String accountType = extras.getString(KEY_ACCOUNT_TYPE);
        Account[] accounts;
        if (accountFeatures != null) {
            try {
                accounts = AccountManager.get(context).getAccountsByTypeAndFeatures(accountType, accountFeatures, null, null).getResult(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.w(TAG, e);
                return null;
            }
        } else {
            accounts = AccountManager.get(context).getAccountsByType(accountType);
        }
        Bundle res = new Bundle();
        res.putParcelableArray(KEY_ACCOUNTS, accounts);
        return res;
    }

    @Override
    public Bundle removeAccount(Account account) {
        Log.w(TAG, "Not implemented: removeAccount(" + account + ")");
        return null;
    }

    @Override
    public Bundle requestGoogleAccountsAccess(String packageName) {
        Log.w(TAG, "Not implemented: requestGoogleAccountsAccess(" + packageName + ")");
        return null;
    }

    @Override
    public int hasCapabilities(HasCababilitiesRequest request) {
        Log.w(TAG, "Not implemented: hasCapabilities(" + request.account + ", " + Arrays.toString(request.capabilities) + ")");
        return 1;
    }

    @Override
    public GetHubTokenInternalResponse getHubToken(GetHubTokenRequest request, Bundle extras) {
        Log.w(TAG, "Not implemented: getHubToken()");
        return null;
    }

    @Override
    @SuppressLint("MissingPermission") // Workaround bug in Android Linter
    public Bundle clearToken(String token, Bundle extras) {

        Log.d(TAG, "clearToken: token:" + token + " extras:" + extras);
        AccountManager.get(context).invalidateAuthToken(AuthConstants.DEFAULT_ACCOUNT_TYPE, token);
        return null;
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
