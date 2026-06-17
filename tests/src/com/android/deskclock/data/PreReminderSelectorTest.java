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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.android.deskclock.provider.Alarm;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PreReminderSelectorTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Alarm mAlarm = new Alarm(8, 30);
    private final RepeatRule mMainRule = new LegacyWeeklyRepeatRule(Weekdays.fromCalendarDays(
            Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY));

    @Test
    public void dateIntervalMatchesEveryTwoNaturalDays() {
        DateIntervalSelector selector = new DateIntervalSelector(
                new GregorianCalendar(2026, Calendar.JUNE, 18), 2);

        assertTrue(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 18)));
        assertFalse(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 19)));
        assertTrue(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 20)));
        assertFalse(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 21)));
        assertTrue(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 22)));
    }

    @Test
    public void weeklyMatchesSelectedDaysOnly() {
        WeeklySelector selector = new WeeklySelector(
                Weekdays.fromCalendarDays(Calendar.MONDAY, Calendar.WEDNESDAY));

        assertTrue(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 22)));
        assertFalse(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 23)));
        assertTrue(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 24)));
    }

    @Test
    public void everyNOccurrencesCountsMainAlarmOccurrences() {
        EveryNOccurrencesSelector selector = new EveryNOccurrencesSelector(
                new GregorianCalendar(2026, Calendar.JUNE, 22), 2);

        assertTrue(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 22)));
        assertFalse(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 24)));
        assertTrue(selector.matches(mContext, mAlarm, mMainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 26)));
    }
}
