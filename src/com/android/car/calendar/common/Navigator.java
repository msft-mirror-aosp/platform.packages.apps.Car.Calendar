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
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;
import com.android.car.calendar.R;

/** Launches a navigation activity. */
public class Navigator {
    private static final String TAG = "CarCalendarNavigator";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private final Context mContext;
    public Navigator(Context context) {
        this.mContext = context;
    }
    /** Launches a navigation activity to the given address or place name. */
    public void navigate(String locationText) {
        Uri navigateUri = Uri.parse("google.navigation:q=" + locationText);
        Intent intent = new Intent(Intent.ACTION_VIEW, navigateUri);
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.no_navigator, Toast.LENGTH_LONG).show();
            if (DEBUG) {
                Log.d(TAG, "Not able to start activity for navigation.\n" + e.toString());
            }
        }
    }
}
