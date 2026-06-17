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

final class EveryNOccurrencesSelector implements PreReminderSelector {
    private final Calendar mAnchorDate;
    private final int mEveryN;

    EveryNOccurrencesSelector(Calendar anchorDate, int everyN) {
        mAnchorDate = CalendarDateUtils.dateOnly(anchorDate);
        mEveryN = everyN;
    }

    @Override
    public boolean matches(Context context, Alarm alarm, RepeatRule mainRule, Calendar targetDate) {
        if (mEveryN <= 0 || CalendarDateUtils.daysBetween(mAnchorDate, targetDate) < 0) {
            return false;
        }

        Calendar probe = (Calendar) mAnchorDate.clone();
        int occurrence = 0;
        while (!probe.after(targetDate)) {
            if (mainRule.matchesDate(context, alarm, probe)) {
                occurrence++;
                if (CalendarDateUtils.toYmdInt(probe)
                        == CalendarDateUtils.toYmdInt(targetDate)) {
                    return (occurrence - 1) % mEveryN == 0;
                }
            }
            probe.add(Calendar.DAY_OF_YEAR, 1);
        }

        return false;
    }
}
