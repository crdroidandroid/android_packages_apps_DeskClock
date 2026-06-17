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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class PreReminder {
    public static final int MAX_OFFSET_MINUTES = 720;

    public final String id;
    public final boolean enabled;
    public final String label;
    public final int offsetMinutes;
    public final boolean requireMainOccurrence;
    public final CombineMode combineMode;
    public final PreReminderSelector selector;

    PreReminder(String id, boolean enabled, String label, int offsetMinutes,
            boolean requireMainOccurrence, CombineMode combineMode,
            PreReminderSelector selector) {
        this.id = id;
        this.enabled = enabled;
        this.label = label;
        this.offsetMinutes = offsetMinutes;
        this.requireMainOccurrence = requireMainOccurrence;
        this.combineMode = combineMode;
        this.selector = selector;
    }

    public static List<PreReminder> parseFromAlarm(Alarm alarm, Calendar template) {
        List<PreReminder> reminders = new ArrayList<>();
        if (alarm.repeatRule == null || alarm.repeatRule.trim().isEmpty()) {
            return reminders;
        }

        try {
            JSONObject root = new JSONObject(alarm.repeatRule);
            JSONArray array = root.optJSONArray("pre_reminders");
            if (array == null) {
                return reminders;
            }

            for (int i = 0; i < array.length(); i++) {
                PreReminder reminder = parse(array.optJSONObject(i), template);
                if (reminder != null) {
                    reminders.add(reminder);
                }
            }
        } catch (JSONException | IllegalArgumentException ignored) {
            return new ArrayList<>();
        }

        return reminders;
    }

    private static PreReminder parse(JSONObject json, Calendar template) throws JSONException {
        if (json == null) {
            return null;
        }

        int offset = json.optInt("offset_minutes", 0);
        if (offset <= 0 || offset > MAX_OFFSET_MINUTES) {
            return null;
        }

        PreReminderSelector selector = parseSelector(json.optJSONObject("selector"), template);
        if (selector == null) {
            return null;
        }

        return new PreReminder(
                json.optString("id", ""),
                json.optBoolean("enabled", true),
                json.optString("label", ""),
                offset,
                json.optBoolean("require_main_occurrence", true),
                CombineMode.parse(json.optString("combine_mode", "additive")),
                selector);
    }

    private static PreReminderSelector parseSelector(JSONObject selector, Calendar template)
            throws JSONException {
        if (selector == null) {
            return null;
        }

        String type = selector.optString("type", "");
        if ("date_interval".equals(type)) {
            Calendar anchor = CalendarDateUtils.parseYmd(
                    selector.getString("anchor_date"), template);
            return new DateIntervalSelector(anchor, selector.optInt("interval_days", 0));
        }

        if ("weekly".equals(type)) {
            return new WeeklySelector(parseWeekdays(selector.optJSONArray("days")));
        }

        if ("every_n_occurrences".equals(type)) {
            Calendar anchor = CalendarDateUtils.parseYmd(
                    selector.getString("anchor_date"), template);
            return new EveryNOccurrencesSelector(anchor, selector.optInt("n", 0));
        }

        return null;
    }

    private static Weekdays parseWeekdays(JSONArray days) throws JSONException {
        Weekdays weekdays = Weekdays.NONE;
        if (days == null) {
            return weekdays;
        }

        for (int i = 0; i < days.length(); i++) {
            weekdays = weekdays.setBit(parseCalendarDay(days.getString(i)), true);
        }
        return weekdays;
    }

    private static int parseCalendarDay(String value) {
        if ("MON".equals(value)) {
            return Calendar.MONDAY;
        } else if ("TUE".equals(value)) {
            return Calendar.TUESDAY;
        } else if ("WED".equals(value)) {
            return Calendar.WEDNESDAY;
        } else if ("THU".equals(value)) {
            return Calendar.THURSDAY;
        } else if ("FRI".equals(value)) {
            return Calendar.FRIDAY;
        } else if ("SAT".equals(value)) {
            return Calendar.SATURDAY;
        } else if ("SUN".equals(value)) {
            return Calendar.SUNDAY;
        }
        throw new IllegalArgumentException("Invalid weekday: " + value);
    }
}
