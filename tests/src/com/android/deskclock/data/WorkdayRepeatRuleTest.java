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

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.android.deskclock.provider.Alarm;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;

@RunWith(AndroidJUnit4ClassRunner.class)
public class WorkdayRepeatRuleTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void schedulesNextChinaMainlandWorkday() {
        assertNextAlarm(2026, Calendar.JANUARY, 3, 20, 0,
                2026, Calendar.JANUARY, 4, 8, 30);
        assertNextAlarm(2026, Calendar.FEBRUARY, 13, 20, 0,
                2026, Calendar.FEBRUARY, 14, 8, 30);
        assertNextAlarm(2026, Calendar.FEBRUARY, 14, 20, 0,
                2026, Calendar.FEBRUARY, 24, 8, 30);
        assertNextAlarm(2026, Calendar.JUNE, 18, 20, 0,
                2026, Calendar.JUNE, 22, 8, 30);
        assertNextAlarm(2026, Calendar.SEPTEMBER, 19, 20, 0,
                2026, Calendar.SEPTEMBER, 20, 8, 30);
        assertNextAlarm(2026, Calendar.OCTOBER, 9, 20, 0,
                2026, Calendar.OCTOBER, 10, 8, 30);
    }

    private void assertNextAlarm(int fromYear, int fromMonth, int fromDay, int fromHour,
            int fromMinute, int expectedYear, int expectedMonth, int expectedDay,
            int expectedHour, int expectedMinute) {
        Alarm alarm = new Alarm(8, 30);
        alarm.daysOfWeek = Weekdays.fromCalendarDays(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY);
        alarm.repeatRule = RepeatRules.workday(
                RepeatRules.CALENDAR_CN_MAINLAND, RepeatRules.FALLBACK_MON_TO_FRI);

        Calendar actual = AlarmScheduleCalculator.getNextAlarmTime(mContext, alarm,
                new GregorianCalendar(fromYear, fromMonth, fromDay, fromHour, fromMinute));

        assertEquals(new GregorianCalendar(expectedYear, expectedMonth, expectedDay,
                expectedHour, expectedMinute), actual);
    }
}
