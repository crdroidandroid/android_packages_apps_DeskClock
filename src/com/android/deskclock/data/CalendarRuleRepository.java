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
import android.content.res.Resources;
import android.util.ArrayMap;

import com.android.deskclock.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CalendarRuleRepository {
    private static final int SEARCH_WINDOW_DAYS = 370;

    private static CalendarRuleRepository sInstance;

    private final Context mContext;
    private final Map<String, Map<Integer, String>> mIndex = new ArrayMap<>();
    private final Map<String, CalendarRule> mCache = new ArrayMap<>();
    private boolean mIndexLoaded;

    private CalendarRuleRepository(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized CalendarRuleRepository get(Context context) {
        if (sInstance == null) {
            sInstance = new CalendarRuleRepository(context);
        }
        return sInstance;
    }

    public boolean isWorkday(String calendarId, String fallback, Calendar day) {
        CalendarRule rule = getRule(calendarId, day.get(Calendar.YEAR));
        int ymd = CalendarDateUtils.toYmdInt(day);

        if (rule != null) {
            if (rule.holidays.contains(ymd)) {
                return false;
            }

            if (rule.workdays.contains(ymd)) {
                return true;
            }

            String defaultRule = rule.defaultRule;
            if (defaultRule != null && !defaultRule.isEmpty()) {
                return fallbackIsWorkday(defaultRule, day);
            }
        }

        return fallbackIsWorkday(fallback, day);
    }

    public int getDistanceToNextWorkday(String calendarId, String fallback, Calendar from) {
        Calendar probe = (Calendar) from.clone();
        for (int days = 0; days <= SEARCH_WINDOW_DAYS; days++) {
            if (isWorkday(calendarId, fallback, probe)) {
                return days;
            }
            probe.add(Calendar.DAY_OF_YEAR, 1);
        }
        return -1;
    }

    public int getDistanceToPreviousWorkday(String calendarId, String fallback, Calendar from) {
        Calendar probe = (Calendar) from.clone();
        for (int days = 1; days <= SEARCH_WINDOW_DAYS; days++) {
            probe.add(Calendar.DAY_OF_YEAR, -1);
            if (isWorkday(calendarId, fallback, probe)) {
                return days;
            }
        }
        return -1;
    }

    private boolean fallbackIsWorkday(String fallback, Calendar day) {
        if (RepeatRules.FALLBACK_MON_TO_FRI.equals(fallback)) {
            return CalendarDateUtils.isMonToFri(day);
        }

        return CalendarDateUtils.isMonToFri(day);
    }

    private CalendarRule getRule(String calendarId, int year) {
        loadIndexIfNeeded();
        Map<Integer, String> years = mIndex.get(calendarId);
        if (years == null) {
            return null;
        }

        String resourceName = years.get(year);
        if (resourceName == null) {
            return null;
        }

        String cacheKey = calendarId + ":" + year;
        CalendarRule cached = mCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        CalendarRule rule = loadRule(resourceName);
        if (rule != null) {
            mCache.put(cacheKey, rule);
        }
        return rule;
    }

    private void loadIndexIfNeeded() {
        if (mIndexLoaded) {
            return;
        }

        try {
            JSONObject root = readRawJson(R.raw.calendar_index);
            JSONArray calendars = root.optJSONArray("calendars");
            if (calendars == null) {
                return;
            }

            for (int i = 0; i < calendars.length(); i++) {
                JSONObject calendar = calendars.getJSONObject(i);
                String id = calendar.optString("id", "");
                JSONObject yearsJson = calendar.optJSONObject("years");
                if (id.isEmpty() || yearsJson == null) {
                    continue;
                }

                Map<Integer, String> years = new ArrayMap<>();
                JSONArray names = yearsJson.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String year = names.getString(j);
                        try {
                            years.put(Integer.parseInt(year), yearsJson.getString(year));
                        } catch (NumberFormatException ignored) {
                            // Skip malformed year keys.
                        }
                    }
                }
                if (!years.isEmpty()) {
                    mIndex.put(id, years);
                }
            }
        } catch (JSONException | IOException | Resources.NotFoundException ignored) {
            // Missing or invalid data falls back to mon_to_fri scheduling.
        } finally {
            mIndexLoaded = true;
        }
    }

    private CalendarRule loadRule(String resourceName) {
        int resId = mContext.getResources().getIdentifier(
                resourceName, "raw", mContext.getPackageName());
        if (resId == 0) {
            return null;
        }

        try {
            JSONObject root = readRawJson(resId);
            String calendarId = root.optString("calendar", "");
            int year = root.optInt("year");
            String defaultRule = root.optString(
                    "default_rule", RepeatRules.FALLBACK_MON_TO_FRI);
            Set<Integer> workdays = parseDates(root.optJSONArray("workdays"));
            Set<Integer> holidays = parseDates(root.optJSONArray("holidays"));
            return new CalendarRule(calendarId, year, defaultRule, workdays, holidays);
        } catch (JSONException | IOException | Resources.NotFoundException ignored) {
            return null;
        }
    }

    private Set<Integer> parseDates(JSONArray dates) throws JSONException {
        Set<Integer> result = new HashSet<>();
        if (dates == null) {
            return result;
        }

        for (int i = 0; i < dates.length(); i++) {
            result.add(CalendarDateUtils.parseYmdToInt(dates.getString(i)));
        }
        return result;
    }

    private JSONObject readRawJson(int resId) throws IOException, JSONException {
        try (InputStream in = mContext.getResources().openRawResource(resId)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        }
    }
}
