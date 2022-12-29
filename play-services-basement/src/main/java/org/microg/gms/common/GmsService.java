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

package org.microg.gms.common;

public enum GmsService {
    UNKNOWN(-2),
    ANY(-1),
    PEOPLE(5, "com.google.android.gms.people.service.START"),
    ACCOUNT(9, "com.google.android.gms.accounts.ACCOUNT_SERVICE"),
    ADDRESS(12, "com.google.android.gms.identity.service.BIND"),
    AUTH_PROXY(16, "com.google.android.gms.auth.service.START"),
    INDEX(21, "com.google.android.gms.icing.INDEX_SERVICE"),
    COMMON(39, "com.google.android.gms.common.service.START"),
    SIGN_IN(44, "com.google.android.gms.signin.service.START"),
    CREDENTIALS(68, "com.google.android.gms.auth.api.credentials.service.START"),
    IDENTITY_SIGN_IN(212, "com.google.android.gms.auth.api.identity.service.signin.START"),
    ;

    public final int SERVICE_ID;
    public final String ACTION;
    public final String[] SECONDARY_ACTIONS;

    GmsService(int serviceId, String... actions) {
        this.SERVICE_ID = serviceId;
        this.ACTION = actions.length > 0 ? actions[0] : null;
        this.SECONDARY_ACTIONS = actions;
    }

    public static GmsService byServiceId(int serviceId) {
        for (GmsService service : values()) {
            if (service.SERVICE_ID == serviceId) return service;
        }
        return UNKNOWN;
    }

    public static String nameFromServiceId(int serviceId) {
        return byServiceId(serviceId).toString(serviceId);
    }

    public String toString(int serviceId) {
        if (this != UNKNOWN) return toString();
        return "UNKNOWN(" + serviceId + ")";
    }
}