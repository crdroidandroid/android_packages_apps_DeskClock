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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public final class BasicPreReminderSettings {
    private static final String ID = "basic_pre_reminder";

    public final boolean enabled;
    public final String label;
    public final int offsetMinutes;
    public final String anchorDate;
    public final int intervalDays;

    public BasicPreReminderSettings(boolean enabled, String label, int offsetMinutes,
            String anchorDate, int intervalDays) {
        this.enabled = enabled;
        this.label = label;
        this.offsetMinutes = offsetMinutes;
        this.anchorDate = anchorDate;
        this.intervalDays = intervalDays;
    }

    public static BasicPreReminderSettings fromAlarm(Alarm alarm) {
        BasicPreReminderSettings defaults = defaults(Calendar.getInstance());
        if (alarm.repeatRule == null || alarm.repeatRule.trim().isEmpty()) {
            return defaults;
        }

        try {
            JSONObject root = new JSONObject(alarm.repeatRule);
            JSONArray reminders = root.optJSONArray("pre_reminders");
            if (reminders == null || reminders.length() == 0) {
                return defaults;
            }

            JSONObject reminder = reminders.optJSONObject(0);
            JSONObject selector = reminder == null ? null : reminder.optJSONObject("selector");
            if (selector == null || !"date_interval".equals(selector.optString("type"))) {
                return defaults;
            }

            return new BasicPreReminderSettings(
                    reminder.optBoolean("enabled", true),
                    reminder.optString("label", defaults.label),
                    reminder.optInt("offset_minutes", defaults.offsetMinutes),
                    selector.optString("anchor_date", defaults.anchorDate),
                    selector.optInt("interval_days", defaults.intervalDays));
        } catch (JSONException ignored) {
            return defaults;
        }
    }

    public static BasicPreReminderSettings defaults(Calendar now) {
        return new BasicPreReminderSettings(false, "", 30, formatYmd(now), 2);
    }

    public String applyToRepeatRule(String repeatRule) {
        try {
            JSONObject root = parseRoot(repeatRule);
            if (enabled) {
                JSONArray reminders = new JSONArray();
                reminders.put(toJson());
                root.put("pre_reminders", reminders);
                return root.toString();
            }

            root.put("pre_reminders", new JSONArray());
            if (!root.has("type")) {
                return null;
            }
            return root.toString();
        } catch (JSONException ignored) {
            return null;
        }
    }

    private JSONObject toJson() throws JSONException {
        JSONObject selector = new JSONObject();
        selector.put("type", "date_interval");
        selector.put("anchor_date", anchorDate);
        selector.put("interval_days", Math.max(1, intervalDays));

        JSONObject reminder = new JSONObject();
        reminder.put("id", ID);
        reminder.put("enabled", true);
        reminder.put("label", label == null ? "" : label.trim());
        reminder.put("offset_minutes", clamp(offsetMinutes, 1, PreReminder.MAX_OFFSET_MINUTES));
        reminder.put("require_main_occurrence", true);
        reminder.put("combine_mode", "additive");
        reminder.put("selector", selector);
        return reminder;
    }

    private static JSONObject parseRoot(String repeatRule) {
        JSONObject root;
        try {
            root = repeatRule == null || repeatRule.trim().isEmpty()
                    ? new JSONObject()
                    : new JSONObject(repeatRule);
        } catch (JSONException ignored) {
            root = new JSONObject();
        }

        try {
            root.put("schema", root.optInt("schema", 1));
        } catch (JSONException ignored) {
            return new JSONObject();
        }
        return root;
    }

    public static String formatYmd(Calendar calendar) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
