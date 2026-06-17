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

import com.android.deskclock.provider.Alarm;

import java.util.Calendar;

final class WorkdayRepeatRule implements RepeatRule {
    private final String mCalendarId;
    private final String mFallback;

    WorkdayRepeatRule(String calendarId, String fallback) {
        mCalendarId = calendarId;
        mFallback = fallback;
    }

    @Override
    public boolean isRepeating() {
        return true;
    }

    @Override
    public boolean matchesDate(Context context, Alarm alarm, Calendar date) {
        return CalendarRuleRepository.get(context).isWorkday(mCalendarId, mFallback, date);
    }

    @Override
    public Calendar getNextAlarmTime(Context context, Alarm alarm, Calendar currentTime) {
        Calendar next = CalendarDateUtils.createAlarmTime(alarm, currentTime);

        if (next.getTimeInMillis() <= currentTime.getTimeInMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        int addDays = CalendarRuleRepository.get(context)
                .getDistanceToNextWorkday(mCalendarId, mFallback, next);
        if (addDays > 0) {
            next.add(Calendar.DAY_OF_YEAR, addDays);
        }

        CalendarDateUtils.resetHourMinute(alarm, next);
        return next;
    }

    @Override
    public Calendar getPreviousAlarmTime(Context context, Alarm alarm, Calendar currentTime) {
        Calendar previous = CalendarDateUtils.createAlarmTime(alarm, currentTime);

        int subtractDays = CalendarRuleRepository.get(context)
                .getDistanceToPreviousWorkday(mCalendarId, mFallback, previous);
        if (subtractDays < 0) {
            return null;
        }

        if (subtractDays > 0) {
            previous.add(Calendar.DAY_OF_YEAR, -subtractDays);
        }

        CalendarDateUtils.resetHourMinute(alarm, previous);
        return previous;
    }
}
