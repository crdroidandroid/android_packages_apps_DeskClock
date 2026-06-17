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

import java.util.Calendar;

public final class CalendarDateUtils {
    private CalendarDateUtils() {}

    public static Calendar createAlarmTime(Alarm alarm, Calendar base) {
        final Calendar alarmTime = Calendar.getInstance(base.getTimeZone());
        alarmTime.set(Calendar.YEAR, base.get(Calendar.YEAR));
        alarmTime.set(Calendar.MONTH, base.get(Calendar.MONTH));
        alarmTime.set(Calendar.DAY_OF_MONTH, base.get(Calendar.DAY_OF_MONTH));
        resetHourMinute(alarm, alarmTime);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);
        return alarmTime;
    }

    public static void resetHourMinute(Alarm alarm, Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minutes);
    }

    public static int toYmdInt(Calendar calendar) {
        return calendar.get(Calendar.YEAR) * 10000
                + (calendar.get(Calendar.MONTH) + 1) * 100
                + calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int parseYmdToInt(String value) {
        String[] parts = value.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid date: " + value);
        }
        return Integer.parseInt(parts[0]) * 10000
                + Integer.parseInt(parts[1]) * 100
                + Integer.parseInt(parts[2]);
    }

    public static boolean isMonToFri(Calendar calendar) {
        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        return dow >= Calendar.MONDAY && dow <= Calendar.FRIDAY;
    }
}
