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

package org.microg.gms.auth.login;

import static android.accounts.AccountManager.PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE;
import static android.accounts.AccountManager.VISIBILITY_USER_MANAGED_VISIBLE;
import static android.os.Build.VERSION.SDK_INT;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.microg.gms.auth.AuthPrefs.isAuthVisible;
import static org.microg.gms.checkin.CheckinPrefs.hideLauncherIcon;
import static org.microg.gms.checkin.CheckinPrefs.isSpoofingEnabled;
import static org.microg.gms.checkin.CheckinPrefs.setSpoofingEnabled;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewFeature;

import com.mgoogle.android.gms.R;

import org.json.JSONArray;
import org.microg.gms.auth.AuthConstants;
import org.microg.gms.auth.AuthManager;
import org.microg.gms.auth.AuthRequest;
import org.microg.gms.auth.AuthResponse;
import org.microg.gms.checkin.CheckinManager;
import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.HttpFormClient;
import org.microg.gms.common.Utils;
import org.microg.gms.people.PeopleManager;
import org.microg.gms.profile.Build;
import org.microg.gms.profile.ProfileManager;
import org.microg.gms.ui.UtilsKt;

import java.io.IOException;
import java.util.Locale;

public class LoginActivity extends AssistantActivity {
    public static final String TMPL_NEW_ACCOUNT = "new_account";
    public static final String EXTRA_TMPL = "tmpl";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_TOKEN = "masterToken";

    private static final String TAG = "GmsAuthLoginBrowser";
    private static final String EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup";
    private static final String PROGRAMMATIC_AUTH_URL = "https://accounts.google.com/o/oauth2/programmatic_auth";
    private static final String GOOGLE_SUITE_URL = "https://accounts.google.com/signin/continue";
    private static final String MAGIC_USER_AGENT = " MinuteMaid";
    private static final String COOKIE_OAUTH_TOKEN = "oauth_token";

    private WebView webView;
    private String accountType;
    private AccountManager accountManager;
    private ViewGroup authContent;
    private int state = 0;

    @SuppressLint("AddJavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountType = AuthConstants.DEFAULT_ACCOUNT_TYPE;
        accountManager = AccountManager.get(LoginActivity.this);
        webView = createWebView(this);
        webView.addJavascriptInterface(new JsBridge(), "mm");
        authContent = findViewById(R.id.auth_content);
        ((ViewGroup) findViewById(R.id.auth_root)).addView(webView);
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "pageFinished: " + view.getUrl());
                Uri uri = Uri.parse(view.getUrl());

                // Begin login.
                if ("identifier".equals(uri.getFragment()) || uri.getPath().endsWith("/identifier"))
                    runOnUiThread(() -> webView.setVisibility(VISIBLE));

                // Normal login.
                if ("close".equals(uri.getFragment()))
                    closeWeb(false);

                // Google Suite login.
                if (url.startsWith(GOOGLE_SUITE_URL))
                    closeWeb(false);

                // IDK when this is called.
                if (url.startsWith(PROGRAMMATIC_AUTH_URL))
                    closeWeb(true);
            }
        });
        if (getIntent().hasExtra(EXTRA_TOKEN)) {
            if (getIntent().hasExtra(EXTRA_EMAIL)) {
                AccountManager accountManager = AccountManager.get(this);
                Account account = new Account(getIntent().getStringExtra(EXTRA_EMAIL), accountType);
                accountManager.addAccountExplicitly(account, getIntent().getStringExtra(EXTRA_TOKEN), null);
                if (isAuthVisible(this) && SDK_INT >= 26) {
                    accountManager.setAccountVisibility(account, PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE, VISIBILITY_USER_MANAGED_VISIBLE);
                }
                retrieveGmsToken(account);
            } else {
                retrieveRtToken(getIntent().getStringExtra(EXTRA_TOKEN));
            }
        } else {
            setMessage(R.string.auth_before_connect);
            setSpoofButtonText(R.string.brand_spoof_button);
            setNextButtonText(R.string.auth_sign_in);
        }
    }

    @Override
    protected void onHuaweiButtonClicked() {
        super.onHuaweiButtonClicked();
        state++;
        if (state == 1) {
            hideLauncherIcon(this, false);
            UtilsKt.hideIcon(this, false);
            if (!isSpoofingEnabled(this)) {
                LastCheckinInfo.clear(this);
                setSpoofingEnabled(this, true);
            }
            init();
        } else if (state == -1) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onNextButtonClicked() {
        super.onNextButtonClicked();
        state++;
        if (state == 1) {
            if (isSpoofingEnabled(this)) {
                LastCheckinInfo.clear(this);
                setSpoofingEnabled(this, false);
            }
            init();
        } else if (state == -1) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void init() {
        setTitle(R.string.just_a_sec);
        setSpoofButtonText(null);
        setNextButtonText(null);
        View loading = getLayoutInflater().inflate(R.layout.login_assistant_loading, authContent, false);
        authContent.removeAllViews();
        authContent.addView(loading);
        setMessage(R.string.auth_connecting);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().removeAllCookies(value -> start());
    }

    private static WebView createWebView(Context context) {
        WebView webView = new WebView(context);
        webView.setVisibility(INVISIBLE);
        webView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.TRANSPARENT);
        boolean systemIsDark =
                (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES;
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.getSettings(),
                    systemIsDark ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF);
        }
        prepareWebViewSettings(context, webView.getSettings());
        return webView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void prepareWebViewSettings(Context context, WebSettings settings) {
        ProfileManager.ensureInitialized(context);
        settings.setUserAgentString(Build.INSTANCE.generateWebViewUserAgentString(settings.getUserAgentString()) + MAGIC_USER_AGENT);
        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setSaveFormData(false);
        settings.setAllowFileAccess(false);
        settings.setDatabaseEnabled(false);
        settings.setNeedInitialFocus(false);
        settings.setUseWideViewPort(false);
        settings.setSupportZoom(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
    }

    private void start() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (LastCheckinInfo.read(this).getAndroidId() == 0) {
                new Thread(() -> {
                    Runnable next;
                    next = checkin(false) ? this::loadLoginPage : () -> showError(R.string.auth_general_error_desc);
                    LoginActivity.this.runOnUiThread(next);
                }).start();
            } else {
                loadLoginPage();
            }
        } else {
            showError(R.string.no_network_error_desc);
        }
    }

    private void showError(int errorRes) {
        setTitle(R.string.sorry);
        findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
        setMessage(errorRes);
    }

    private void setMessage(@StringRes int res) {
        setMessage(getText(res));
    }

    private void setMessage(CharSequence text) {
        ((TextView) findViewById(R.id.description_text)).setText(text);
    }

    private void loadLoginPage() {
        String tmpl = getIntent().hasExtra(EXTRA_TMPL) ? getIntent().getStringExtra(EXTRA_TMPL) : TMPL_NEW_ACCOUNT;
        webView.loadUrl(buildUrl(tmpl, Utils.getLocale()));
    }

    private void closeWeb(boolean programmaticAuth) {
        setMessage(R.string.auth_finalize);
        runOnUiThread(() -> webView.setVisibility(INVISIBLE));
        String cookies = CookieManager.getInstance().getCookie(programmaticAuth ? PROGRAMMATIC_AUTH_URL : EMBEDDED_SETUP_URL);
        String[] temp = cookies.split(";");
        for (String ar1 : temp) {
            if (ar1.trim().startsWith(COOKIE_OAUTH_TOKEN + "=")) {
                String[] temp1 = ar1.split("=");
                retrieveRtToken(temp1[1]);
                return;
            }
        }
        showError(R.string.auth_general_error_desc);
    }

    private void retrieveRtToken(String oAuthToken) {
        new AuthRequest().fromContext(this)
                .appIsGms()
                .callerIsGms()
                .service("ac2dm")
                .token(oAuthToken).isAccessToken()
                .addAccount()
                .getAccountId()
                .getResponseAsync(new HttpFormClient.Callback<AuthResponse>() {
                    @Override
                    public void onResponse(AuthResponse response) {
                        Account account = new Account(response.email, accountType);
                        if (accountManager.addAccountExplicitly(account, response.token, null)) {
                            accountManager.setAuthToken(account, "SID", response.Sid);
                            accountManager.setAuthToken(account, "LSID", response.LSid);
                            accountManager.setUserData(account, "flags", "1");
                            accountManager.setUserData(account, "services", response.services);
                            accountManager.setUserData(account, "oauthAccessToken", "1");
                            accountManager.setUserData(account, "firstName", response.firstName);
                            accountManager.setUserData(account, "lastName", response.lastName);
                            if (!TextUtils.isEmpty(response.accountId))
                                accountManager.setUserData(account, "GoogleUserId", response.accountId);

                            retrieveGmsToken(account);
                            setResult(RESULT_OK);
                        } else {
                            Log.w(TAG, "Account NOT created!");
                            runOnUiThread(() -> {
                                showError(R.string.auth_general_error_desc);
                                setNextButtonText(android.R.string.ok);
                            });
                            state = -2;
                        }
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException", exception);
                        runOnUiThread(() -> {
                            showError(R.string.auth_general_error_desc);
                            setNextButtonText(android.R.string.ok);
                        });
                        state = -2;
                    }
                });
    }

    private void retrieveGmsToken(final Account account) {
        final AuthManager authManager = new AuthManager(this, account.name, GMS_PACKAGE_NAME, "ac2dm");
        authManager.setPermitted(true);
        new AuthRequest().fromContext(this)
                .appIsGms()
                .callerIsGms()
                .service(authManager.getService())
                .email(account.name)
                .token(AccountManager.get(this).getPassword(account))
                .systemPartition()
                .hasPermission()
                .addAccount()
                .getAccountId()
                .getResponseAsync(new HttpFormClient.Callback<AuthResponse>() {
                    @Override
                    public void onResponse(AuthResponse response) {
                        authManager.storeResponse(response);
                        String accountId = PeopleManager.loadUserInfo(LoginActivity.this, account);
                        if (!TextUtils.isEmpty(accountId))
                            accountManager.setUserData(account, "GoogleUserId", accountId);
                        checkin(true);
                        finish();
                    }
                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException", exception);
                        runOnUiThread(() -> {
                            showError(R.string.auth_general_error_desc);
                            setNextButtonText(android.R.string.ok);
                        });
                        state = -2;
                    }
                });
    }

    private boolean checkin(boolean force) {
        try {
            CheckinManager.checkin(LoginActivity.this, force);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Checkin failed", e);
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KEYCODE_BACK) && webView.canGoBack() && (webView.getVisibility() == VISIBLE)) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static String buildUrl(String tmpl, Locale locale) {
        return Uri.parse(EMBEDDED_SETUP_URL).buildUpon()
                .appendQueryParameter("source", "android")
                .appendQueryParameter("xoauth_display_name", "Android Device")
                .appendQueryParameter("lang", locale.getLanguage())
                .appendQueryParameter("cc", locale.getCountry().toLowerCase(Locale.US))
                .appendQueryParameter("langCountry", locale.toString().toLowerCase(Locale.US))
                .appendQueryParameter("hl", locale.toString().replace("_", "-"))
                .appendQueryParameter("tmpl", tmpl)
                .build().toString();
    }

    private class JsBridge {

        @SuppressWarnings("MissingPermission")
        @JavascriptInterface
        public final String getAccounts() {
            Log.d(TAG, "JSBridge: getAccounts");
            Account[] accountsByType = accountManager.getAccountsByType(accountType);
            JSONArray json = new JSONArray();
            for (Account account : accountsByType) {
                json.put(account.name);
            }
            return json.toString();
        }

        @JavascriptInterface
        public final void log() {
            Log.d(TAG, "JSBridge: log");
        }

    }
}
