/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.android.deskclock.controller.Controller;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.LogEventTracker;
import com.android.deskclock.uidata.UiDataModel;

import java.io.File;

public class DeskClockApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final Context applicationContext = getApplicationContext();
        final SharedPreferences prefs = getDefaultSharedPreferences(applicationContext);

        DataModel.getDataModel().init(applicationContext, prefs);
        UiDataModel.getUiDataModel().init(applicationContext, prefs);
        Controller.getController().setContext(applicationContext);
        Controller.getController().addEventTracker(new LogEventTracker(applicationContext));
    }

    /**
     * Returns the default {@link SharedPreferences} instance from the underlying storage context.
     */
    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        final Context storageContext = context.createDeviceProtectedStorageContext();
        final String name = PreferenceManager.getDefaultSharedPreferencesName(context);
        final String prefsFilename = storageContext.getDataDir() + "/shared_prefs/" + name + ".xml";
        final File prefs = new File(Uri.parse(prefsFilename).getPath());
        // All N devices have split storage areas. Migrate the existing preferences into the new
        // device encrypted storage area if that has not yet occurred.
        if (!prefs.exists())  {
            if (!storageContext.moveSharedPreferencesFrom(context, name)) {
                LogUtils.wtf("Failed to migrate shared preferences");
            }
        }
        return PreferenceManager.getDefaultSharedPreferences(storageContext);
    }
}
