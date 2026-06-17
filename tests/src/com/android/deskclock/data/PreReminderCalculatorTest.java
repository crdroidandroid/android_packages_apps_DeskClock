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
import static org.junit.Assert.assertFalse;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PreReminderCalculatorTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void aggregatesAdditiveAndAbsoluteOffsets() {
        assertOffset(60,
                reminder("洗澡", 30, CombineMode.ADDITIVE),
                reminder("限号", 30, CombineMode.ADDITIVE),
                reminder("早会", 30, CombineMode.ABSOLUTE));

        assertOffset(30,
                reminder("早会", 30, CombineMode.ABSOLUTE),
                reminder("周报", 20, CombineMode.ABSOLUTE));

        assertOffset(60,
                reminder("洗澡", 30, CombineMode.ADDITIVE),
                reminder("早会", 60, CombineMode.ABSOLUTE));

        assertOffset(75,
                reminder("洗澡", 30, CombineMode.ADDITIVE),
                reminder("限号", 45, CombineMode.ADDITIVE),
                reminder("早会", 60, CombineMode.ABSOLUTE));
    }

    @Test
    public void skipsReminderWhenMainDoesNotOccurAndRequired() {
        Alarm alarm = new Alarm(8, 30);
        RepeatRule mainRule = new LegacyWeeklyRepeatRule(
                Weekdays.fromCalendarDays(Calendar.MONDAY));
        Calendar saturday = new GregorianCalendar(2026, Calendar.JUNE, 20, 8, 30);

        PreReminderResult result = PreReminderCalculator.calculate(mContext, alarm, mainRule,
                saturday, Arrays.asList(reminder("洗澡", 30, CombineMode.ADDITIVE)));

        assertFalse(result.hasReminder());
    }

    @Test
    public void createsPreReminderInstanceFromRepeatRuleJson() {
        Alarm alarm = new Alarm(8, 30);
        alarm.id = 42;
        alarm.repeatRule = "{"
                + "\"schema\":1,"
                + "\"type\":\"workday\","
                + "\"calendar\":\"cn-mainland\","
                + "\"fallback\":\"mon_to_fri\","
                + "\"pre_reminders\":["
                + "{\"id\":\"shower\",\"enabled\":true,\"label\":\"洗澡\","
                + "\"offset_minutes\":30,\"require_main_occurrence\":true,"
                + "\"combine_mode\":\"additive\","
                + "\"selector\":{\"type\":\"date_interval\","
                + "\"anchor_date\":\"2026-06-22\",\"interval_days\":2}},"
                + "{\"id\":\"meeting\",\"enabled\":true,\"label\":\"早会\","
                + "\"offset_minutes\":30,\"require_main_occurrence\":true,"
                + "\"combine_mode\":\"absolute\","
                + "\"selector\":{\"type\":\"weekly\",\"days\":[\"MON\"]}}"
                + "]}";

        List<AlarmInstance> instances = AlarmScheduleCalculator.createInstancesAfter(mContext,
                alarm, new GregorianCalendar(2026, Calendar.JUNE, 21, 20, 0));

        assertEquals(2, instances.size());
        assertEquals(AlarmInstance.KIND_PRE_REMINDER, instances.get(0).mInstanceKind);
        assertEquals("洗澡 · 早会", instances.get(0).mLabel);
        assertEquals(30, instances.get(0).mPreOffsetMinutes);
        assertEquals(new GregorianCalendar(2026, Calendar.JUNE, 22, 8, 0),
                instances.get(0).getAlarmTime());
        assertEquals(AlarmInstance.KIND_MAIN, instances.get(1).mInstanceKind);
        assertEquals(new GregorianCalendar(2026, Calendar.JUNE, 22, 8, 30),
                instances.get(1).getAlarmTime());
    }

    private void assertOffset(int expectedOffset, PreReminder... reminders) {
        Alarm alarm = new Alarm(8, 30);
        RepeatRule mainRule = new LegacyWeeklyRepeatRule(
                Weekdays.fromCalendarDays(Calendar.MONDAY));
        PreReminderResult result = PreReminderCalculator.calculate(mContext, alarm, mainRule,
                new GregorianCalendar(2026, Calendar.JUNE, 22, 8, 30),
                Arrays.asList(reminders));

        assertEquals(expectedOffset, result.offsetMinutes);
    }

    private PreReminder reminder(String label, int offset, CombineMode combineMode) {
        return new PreReminder("", true, label, offset, true, combineMode,
                new WeeklySelector(Weekdays.fromCalendarDays(Calendar.MONDAY)));
    }
}
