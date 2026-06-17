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

import com.android.deskclock.provider.Alarm;

import org.json.JSONException;
import org.json.JSONObject;

public final class RepeatRuleEngine {
    private RepeatRuleEngine() {}

    public static RepeatRule parseOrLegacy(Alarm alarm) {
        if (alarm.repeatRule == null || alarm.repeatRule.trim().isEmpty()) {
            return new LegacyWeeklyRepeatRule(alarm.daysOfWeek);
        }

        try {
            JSONObject json = new JSONObject(alarm.repeatRule);
            int schema = json.optInt("schema", 1);
            String type = json.optString("type", "");

            if (schema == 1 && RepeatRules.TYPE_WORKDAY.equals(type)) {
                String calendar = json.optString("calendar", "");
                String fallback = json.optString("fallback", RepeatRules.FALLBACK_MON_TO_FRI);
                return new WorkdayRepeatRule(calendar, fallback);
            }
        } catch (JSONException ignored) {
            // Fall back to legacy weekly scheduling below.
        }

        return new LegacyWeeklyRepeatRule(alarm.daysOfWeek);
    }

    public static boolean isWorkdayRule(String repeatRule) {
        return RepeatRules.isWorkdayRule(repeatRule);
    }
}
