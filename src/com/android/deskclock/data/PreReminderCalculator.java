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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class PreReminderCalculator {
    private PreReminderCalculator() {}

    public static PreReminderResult calculate(Context context, Alarm alarm, RepeatRule mainRule,
            Calendar mainTime) {
        return calculate(context, alarm, mainRule, mainTime,
                PreReminder.parseFromAlarm(alarm, mainTime));
    }

    static PreReminderResult calculate(Context context, Alarm alarm, RepeatRule mainRule,
            Calendar mainTime, List<PreReminder> reminders) {
        Calendar targetDate = CalendarDateUtils.dateOnly(mainTime);
        boolean mainOccurs = mainRule.matchesDate(context, alarm, targetDate);

        int additiveTotal = 0;
        int absoluteMax = 0;
        List<String> labels = new ArrayList<>();

        for (PreReminder reminder : reminders) {
            if (!reminder.enabled || reminder.selector == null) {
                continue;
            }

            if (reminder.requireMainOccurrence && !mainOccurs) {
                continue;
            }

            if (!reminder.selector.matches(context, alarm, mainRule, targetDate)) {
                continue;
            }

            labels.add(sanitizeLabel(context, reminder.label));
            if (reminder.combineMode == CombineMode.ABSOLUTE) {
                absoluteMax = Math.max(absoluteMax, reminder.offsetMinutes);
            } else {
                additiveTotal += reminder.offsetMinutes;
            }
        }

        int finalOffset = Math.max(additiveTotal, absoluteMax);
        if (finalOffset <= 0 || labels.isEmpty()) {
            return PreReminderResult.none();
        }

        Calendar preTime = (Calendar) mainTime.clone();
        preTime.add(Calendar.MINUTE, -finalOffset);
        return new PreReminderResult(preTime, labels, finalOffset);
    }

    private static String sanitizeLabel(Context context, String label) {
        if (label == null || label.trim().isEmpty()) {
            return context.getString(R.string.pre_reminder_default_label);
        }
        return label.trim();
    }
}
