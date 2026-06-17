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

import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class PreReminderUiModel {
    public static final String SELECTOR_DATE_INTERVAL = "date_interval";
    public static final String SELECTOR_WEEKLY = "weekly";

    public String id;
    public boolean enabled;
    public String label;
    public int offsetMinutes;
    public boolean additive;
    public String selectorType;
    public String anchorDate;
    public int intervalDays;
    public Weekdays weekdays;

    public static List<PreReminderUiModel> fromAlarm(Alarm alarm) {
        List<PreReminderUiModel> result = new ArrayList<>();
        if (alarm.repeatRule == null || alarm.repeatRule.trim().isEmpty()) {
            return result;
        }

        try {
            JSONObject root = new JSONObject(alarm.repeatRule);
            JSONArray reminders = root.optJSONArray("pre_reminders");
            if (reminders == null) {
                return result;
            }

            for (int i = 0; i < reminders.length(); i++) {
                PreReminderUiModel model = fromJson(reminders.optJSONObject(i));
                if (model != null) {
                    result.add(model);
                }
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return result;
    }

    public static boolean hasEnabled(List<PreReminderUiModel> reminders) {
        for (PreReminderUiModel reminder : reminders) {
            if (reminder.enabled) {
                return true;
            }
        }
        return false;
    }

    public static String applyToRepeatRule(String repeatRule, List<PreReminderUiModel> reminders) {
        try {
            JSONObject root = parseRoot(repeatRule);
            JSONArray array = new JSONArray();
            for (PreReminderUiModel reminder : reminders) {
                array.put(reminder.toJson());
            }
            root.put("pre_reminders", array);

            if (array.length() == 0 && !root.has("type")) {
                return null;
            }
            return root.toString();
        } catch (JSONException ignored) {
            return repeatRule;
        }
    }

    public static PreReminderUiModel createDateInterval(Calendar nextAlarmTime) {
        return createDateInterval(nextAlarmTime, "");
    }

    public static PreReminderUiModel createDateInterval(Calendar nextAlarmTime,
            String defaultLabel) {
        PreReminderUiModel model = createBase(nextAlarmTime);
        model.label = defaultLabel;
        model.selectorType = SELECTOR_DATE_INTERVAL;
        model.anchorDate = formatYmd(nextAlarmTime);
        model.intervalDays = 2;
        model.weekdays = Weekdays.NONE;
        return model;
    }

    public static PreReminderUiModel createWeekly(Calendar nextAlarmTime) {
        return createWeekly(nextAlarmTime, "");
    }

    public static PreReminderUiModel createWeekly(Calendar nextAlarmTime, String defaultLabel) {
        PreReminderUiModel model = createBase(nextAlarmTime);
        model.label = defaultLabel;
        model.selectorType = SELECTOR_WEEKLY;
        model.anchorDate = formatYmd(nextAlarmTime);
        model.intervalDays = 1;
        model.weekdays = Weekdays.fromCalendarDays(nextAlarmTime.get(Calendar.DAY_OF_WEEK));
        return model;
    }

    public String summary(Context context) {
        final String mode = context.getString(additive
                ? R.string.pre_reminder_summary_additive
                : R.string.pre_reminder_summary_absolute);
        if (SELECTOR_WEEKLY.equals(selectorType)) {
            return context.getString(R.string.pre_reminder_summary_weekly,
                    offsetMinutes, weeklySummary(context), mode);
        }
        return context.getString(R.string.pre_reminder_summary,
                offsetMinutes, intervalDays, mode);
    }

    public String title(Context context) {
        return labelOrDefault(context);
    }

    public String secondarySummary(Context context) {
        if (SELECTOR_WEEKLY.equals(selectorType)) {
            return "";
        }
        return context.getString(R.string.pre_reminder_starts_on, anchorDate);
    }

    private String labelOrDefault(Context context) {
        return label == null || label.trim().isEmpty()
                ? context.getString(R.string.pre_reminder_default_label)
                : label.trim();
    }

    private String weeklySummary(Context context) {
        StringBuilder builder = new StringBuilder();
        final String separator = context.getString(R.string.pre_reminder_weekday_separator);
        appendDay(builder, Calendar.MONDAY,
                context.getString(R.string.pre_reminder_weekday_short_mon), separator);
        appendDay(builder, Calendar.TUESDAY,
                context.getString(R.string.pre_reminder_weekday_short_tue), separator);
        appendDay(builder, Calendar.WEDNESDAY,
                context.getString(R.string.pre_reminder_weekday_short_wed), separator);
        appendDay(builder, Calendar.THURSDAY,
                context.getString(R.string.pre_reminder_weekday_short_thu), separator);
        appendDay(builder, Calendar.FRIDAY,
                context.getString(R.string.pre_reminder_weekday_short_fri), separator);
        appendDay(builder, Calendar.SATURDAY,
                context.getString(R.string.pre_reminder_weekday_short_sat), separator);
        appendDay(builder, Calendar.SUNDAY,
                context.getString(R.string.pre_reminder_weekday_short_sun), separator);
        return builder.length() == 0
                ? context.getString(R.string.pre_reminder_weekday_none)
                : builder.toString();
    }

    private void appendDay(StringBuilder builder, int calendarDay, String label,
            String separator) {
        if (!weekdays.isBitOn(calendarDay)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(separator);
        }
        builder.append(label);
    }

    private JSONObject toJson() throws JSONException {
        JSONObject selector = new JSONObject();
        if (SELECTOR_WEEKLY.equals(selectorType)) {
            selector.put("type", SELECTOR_WEEKLY);
            selector.put("days", weekdaysToJson());
        } else {
            selector.put("type", SELECTOR_DATE_INTERVAL);
            selector.put("anchor_date", anchorDate);
            selector.put("interval_days", Math.max(1, intervalDays));
        }

        JSONObject reminder = new JSONObject();
        reminder.put("id", id == null || id.isEmpty() ? generateId() : id);
        reminder.put("enabled", enabled);
        reminder.put("label", sanitizeLabel(label));
        reminder.put("offset_minutes", clamp(offsetMinutes, 1, PreReminder.MAX_OFFSET_MINUTES));
        reminder.put("require_main_occurrence", true);
        reminder.put("combine_mode", additive ? "additive" : "absolute");
        reminder.put("selector", selector);
        return reminder;
    }

    private JSONArray weekdaysToJson() {
        JSONArray days = new JSONArray();
        addDay(days, Calendar.MONDAY, "MON");
        addDay(days, Calendar.TUESDAY, "TUE");
        addDay(days, Calendar.WEDNESDAY, "WED");
        addDay(days, Calendar.THURSDAY, "THU");
        addDay(days, Calendar.FRIDAY, "FRI");
        addDay(days, Calendar.SATURDAY, "SAT");
        addDay(days, Calendar.SUNDAY, "SUN");
        return days;
    }

    private void addDay(JSONArray days, int calendarDay, String value) {
        if (weekdays.isBitOn(calendarDay)) {
            days.put(value);
        }
    }

    private static PreReminderUiModel fromJson(JSONObject json) throws JSONException {
        if (json == null) {
            return null;
        }

        JSONObject selector = json.optJSONObject("selector");
        if (selector == null) {
            return null;
        }

        PreReminderUiModel model = new PreReminderUiModel();
        model.id = json.optString("id", "");
        model.enabled = json.optBoolean("enabled", true);
        model.label = sanitizeLabel(json.optString("label", ""));
        model.offsetMinutes = json.optInt("offset_minutes", 30);
        model.additive = !"absolute".equals(json.optString("combine_mode", "additive"));
        model.selectorType = selector.optString("type", SELECTOR_DATE_INTERVAL);
        model.anchorDate = selector.optString("anchor_date", formatYmd(Calendar.getInstance()));
        model.intervalDays = selector.optInt("interval_days", 2);
        model.weekdays = parseWeekdays(selector.optJSONArray("days"));
        return model;
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

    private static PreReminderUiModel createBase(Calendar nextAlarmTime) {
        PreReminderUiModel model = new PreReminderUiModel();
        model.id = generateId();
        model.enabled = true;
        model.label = "";
        model.offsetMinutes = 30;
        model.additive = true;
        model.anchorDate = formatYmd(nextAlarmTime);
        return model;
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

    private static String sanitizeLabel(String label) {
        return label == null ? "" : label.trim();
    }

    private static String generateId() {
        return "pre_" + System.currentTimeMillis();
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
