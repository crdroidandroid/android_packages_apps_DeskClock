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

import org.json.JSONException;
import org.json.JSONObject;

public final class RepeatRules {
    static final String TYPE_WORKDAY = "workday";
    public static final String CALENDAR_CN_MAINLAND = "cn-mainland";
    public static final String FALLBACK_MON_TO_FRI = "mon_to_fri";

    private RepeatRules() {}

    public static String workday(String calendarId, String fallback) {
        JSONObject json = new JSONObject();
        try {
            json.put("schema", 1);
            json.put("type", TYPE_WORKDAY);
            json.put("calendar", calendarId);
            json.put("fallback", fallback);
        } catch (JSONException ignored) {
            return null;
        }
        return json.toString();
    }

    public static boolean isWorkdayRule(String repeatRule) {
        if (repeatRule == null || repeatRule.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject json = new JSONObject(repeatRule);
            return TYPE_WORKDAY.equals(json.optString("type"));
        } catch (JSONException ignored) {
            return false;
        }
    }
}
