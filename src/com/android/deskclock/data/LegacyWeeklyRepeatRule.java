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

final class LegacyWeeklyRepeatRule implements RepeatRule {
    private final Weekdays mDaysOfWeek;

    LegacyWeeklyRepeatRule(Weekdays daysOfWeek) {
        mDaysOfWeek = daysOfWeek;
    }

    @Override
    public boolean isRepeating() {
        return mDaysOfWeek.isRepeating();
    }

    @Override
    public Calendar getNextAlarmTime(Context context, Alarm alarm, Calendar currentTime) {
        final Calendar nextInstanceTime = CalendarDateUtils.createAlarmTime(alarm, currentTime);

        if (nextInstanceTime.getTimeInMillis() <= currentTime.getTimeInMillis()) {
            nextInstanceTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        final int addDays = mDaysOfWeek.getDistanceToNextDay(nextInstanceTime);
        if (addDays > 0) {
            nextInstanceTime.add(Calendar.DAY_OF_WEEK, addDays);
        }

        CalendarDateUtils.resetHourMinute(alarm, nextInstanceTime);
        return nextInstanceTime;
    }

    @Override
    public Calendar getPreviousAlarmTime(Context context, Alarm alarm, Calendar currentTime) {
        final Calendar previousInstanceTime = CalendarDateUtils.createAlarmTime(alarm, currentTime);

        final int subtractDays = mDaysOfWeek.getDistanceToPreviousDay(previousInstanceTime);
        if (subtractDays > 0) {
            previousInstanceTime.add(Calendar.DAY_OF_WEEK, -subtractDays);
            return previousInstanceTime;
        }

        return null;
    }
}
