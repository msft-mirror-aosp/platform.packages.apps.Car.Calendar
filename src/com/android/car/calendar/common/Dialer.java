/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.calendar.common;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Objects;

import javax.annotation.Nullable;

/** Calls the default dialer with an optional access code. */
public class Dialer {

    private static final String TAG = "CarCalendarDialer";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;

    public Dialer(Context context) {
        mContext = context;
    }

    /** Calls a telephone using a phone number and access number. */
    public boolean dial(NumberAndAccess numberAndAccess) {
        Uri dialUri = numberAndAccess.getUri();
        PackageManager packageManager = mContext.getPackageManager();
        boolean useActionCall = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        Intent intent = new Intent(useActionCall ? Intent.ACTION_CALL : Intent.ACTION_DIAL);
        intent.setData(dialUri);
        if (intent.resolveActivity(packageManager) == null) {
            Log.i(TAG, "No dialler app found");
            return false;
        }
        if (DEBUG) Log.d(TAG, "Starting dialler activity");
        mContext.startActivity(intent);
        return true;
    }

    /** An immutable value representing the details required to enter a conference call. */
    public static class NumberAndAccess {
        private final String mNumber;

        @Nullable private final String mAccess;

        NumberAndAccess(String number, @Nullable String access) {
            this.mNumber = number;
            this.mAccess = access;
        }

        public String getNumber() {
            return mNumber;
        }

        @Nullable
        public String getAccess() {
            return mAccess;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("mNumber", mNumber)
                    .add("mAccess", mAccess)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NumberAndAccess that = (NumberAndAccess) o;
            return mNumber.equals(that.mNumber) && Objects.equals(mAccess, that.mAccess);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mNumber, mAccess);
        }

        public Uri getUri() {
            StringBuilder sb = new StringBuilder(getNumber());
            String access = getAccess();
            if (!Strings.isNullOrEmpty(access)) {
                // Wait for the number to dial if required.
                if (!access.startsWith(String.valueOf(PhoneNumberUtils.PAUSE))
                    && !access.startsWith(
                    String.valueOf(PhoneNumberUtils.WAIT))) {
                    // Insert a pause as the separator so the dialer automatically adds the access
                    // code.
                    access = PhoneNumberUtils.PAUSE + access;
                }
                sb.append(access);
            }
            return Uri.fromParts("tel", sb.toString(), /* fragment= */ null);
        }
    }
}
