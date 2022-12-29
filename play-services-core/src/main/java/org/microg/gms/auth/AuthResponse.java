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

import static org.microg.gms.common.HttpFormClient.ResponseField;

import androidx.annotation.NonNull;

public class AuthResponse {

    @ResponseField("SID")
    public String Sid;
    @ResponseField("LSID")
    public String LSid;
    @ResponseField("Auth")
    public String auth;
    @ResponseField("Token")
    public String token;
    @ResponseField("Email")
    public String email;
    @ResponseField("services")
    public String services;
    @ResponseField("GooglePlusUpgrade")
    public boolean isGooglePlusUpgrade;
    @ResponseField("PicasaUser")
    public String picasaUserName;
    @ResponseField("RopText")
    public String ropText;
    @ResponseField("RopRevision")
    public int ropRevision;
    @ResponseField("firstName")
    public String firstName;
    @ResponseField("lastName")
    public String lastName;
    @ResponseField("issueAdvice")
    public String issueAdvice;
    @ResponseField("accountId")
    public String accountId;
    @ResponseField("Expiry")
    public long expiry = -1;
    @ResponseField("storeConsentRemotely")
    public boolean storeConsentRemotely = true;
    @ResponseField("Permission")
    public String permission;
    @ResponseField("ScopeConsentDetails")
    public String scopeConsentDetails;
    @ResponseField("ConsentDataBase64")
    public String consentDataBase64;

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AuthResponse{");
        sb.append("auth='").append(auth).append('\'');
        if (Sid != null) sb.append(", Sid='").append(Sid).append('\'');
        if (LSid != null) sb.append(", LSid='").append(LSid).append('\'');
        if (token != null) sb.append(", token='").append(token).append('\'');
        if (email != null) sb.append(", email='").append(email).append('\'');
        if (services != null) sb.append(", services='").append(services).append('\'');
        if (isGooglePlusUpgrade) sb.append(", isGooglePlusUpgrade=").append(isGooglePlusUpgrade);
        if (picasaUserName != null) sb.append(", picasaUserName='").append(picasaUserName).append('\'');
        if (ropText != null) sb.append(", ropText='").append(ropText).append('\'');
        if (ropRevision != 0) sb.append(", ropRevision=").append(ropRevision);
        if (firstName != null) sb.append(", firstName='").append(firstName).append('\'');
        if (lastName != null) sb.append(", lastName='").append(lastName).append('\'');
        if (issueAdvice != null) sb.append(", issueAdvice='").append(issueAdvice).append('\'');
        if (accountId != null) sb.append(", accountId='").append(accountId).append('\'');
        if (expiry != -1) sb.append(", expiry=").append(expiry);
        if (!storeConsentRemotely) sb.append(", storeConsentRemotely=").append(storeConsentRemotely);
        if (permission != null) sb.append(", permission='").append(permission).append('\'');
        if (scopeConsentDetails != null) sb.append(", scopeConsentDetails='").append(scopeConsentDetails).append('\'');
        if (consentDataBase64 != null) sb.append(", consentDataBase64='").append(consentDataBase64).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
