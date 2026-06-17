/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.deskclock.data;

import android.content.Context;
import android.os.LocaleList;

import com.android.deskclock.LogUtils;

import java.util.Locale;

public final class WorkdayRepeatPolicy {
    private static final String COUNTRY_CHINA = "CN";

    private WorkdayRepeatPolicy() {}

    public static boolean shouldShowChinaMainlandWorkdayRepeat(Context context) {
        return shouldShowChinaMainlandWorkdayRepeat(
                getPrimaryDeviceLocale(context), LogUtils.Logger.DEBUG);
    }

    static boolean shouldShowChinaMainlandWorkdayRepeat(
            Locale deviceLocale, boolean developmentBuild) {
        return developmentBuild
                || (deviceLocale != null
                        && COUNTRY_CHINA.equalsIgnoreCase(deviceLocale.getCountry()));
    }

    private static Locale getPrimaryDeviceLocale(Context context) {
        if (context != null) {
            final LocaleList locales = context.getResources().getConfiguration().getLocales();
            if (locales != null && locales.size() > 0) {
                return locales.get(0);
            }
        }

        return Locale.getDefault();
    }
}
