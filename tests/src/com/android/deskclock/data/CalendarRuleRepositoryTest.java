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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;

@RunWith(AndroidJUnit4ClassRunner.class)
public class CalendarRuleRepositoryTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final CalendarRuleRepository mRepository = CalendarRuleRepository.get(mContext);

    @Test
    public void identifiesChinaMainland2026WorkdayOverrides() {
        assertFalse(isWorkday(2026, Calendar.JANUARY, 1));
        assertTrue(isWorkday(2026, Calendar.JANUARY, 4));
        assertTrue(isWorkday(2026, Calendar.FEBRUARY, 14));
        assertFalse(isWorkday(2026, Calendar.FEBRUARY, 16));
        assertTrue(isWorkday(2026, Calendar.FEBRUARY, 28));
        assertTrue(isWorkday(2026, Calendar.MAY, 9));
        assertTrue(isWorkday(2026, Calendar.JUNE, 22));
        assertFalse(isWorkday(2026, Calendar.JUNE, 21));
        assertTrue(isWorkday(2026, Calendar.SEPTEMBER, 20));
        assertTrue(isWorkday(2026, Calendar.OCTOBER, 10));
    }

    private boolean isWorkday(int year, int month, int day) {
        return mRepository.isWorkday(RepeatRules.CALENDAR_CN_MAINLAND,
                RepeatRules.FALLBACK_MON_TO_FRI, new GregorianCalendar(year, month, day));
    }
}
