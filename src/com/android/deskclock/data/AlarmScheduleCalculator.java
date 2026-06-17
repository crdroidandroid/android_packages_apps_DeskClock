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
import com.android.deskclock.provider.AlarmInstance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class AlarmScheduleCalculator {
    private AlarmScheduleCalculator() {}

    public static AlarmInstance createInstanceAfter(Context context, Alarm alarm, Calendar after) {
        Calendar nextInstanceTime = getNextAlarmTime(context, alarm, after);

        AlarmInstance result = new AlarmInstance(nextInstanceTime, alarm.id);
        result.mVibrate = alarm.vibrate;
        result.mLabel = alarm.label;
        result.mRingtone = alarm.alert;
        result.mIncreasingVolume = alarm.increasingVolume;
        result.mInstanceKind = AlarmInstance.KIND_MAIN;
        return result;
    }

    public static List<AlarmInstance> createInstancesAfter(Context context, Alarm alarm,
            Calendar after) {
        AlarmInstance mainInstance = createInstanceAfter(context, alarm, after);
        List<AlarmInstance> instances = new ArrayList<>();

        AlarmInstance preReminder = createPreReminderInstance(context, alarm, mainInstance, after);
        if (preReminder != null) {
            instances.add(preReminder);
        }
        instances.add(mainInstance);
        return instances;
    }

    public static AlarmInstance createPreReminderInstance(Context context, Alarm alarm,
            AlarmInstance mainInstance, Calendar after) {
        RepeatRule mainRule = RepeatRuleEngine.parseOrLegacy(alarm);
        PreReminderResult result = PreReminderCalculator.calculate(
                context, alarm, mainRule, mainInstance.getAlarmTime());
        if (!result.hasReminder() || result.time.getTimeInMillis() <= after.getTimeInMillis()) {
            return null;
        }

        AlarmInstance instance = new AlarmInstance(result.time, alarm.id);
        instance.mVibrate = alarm.vibrate;
        instance.mLabel = result.getJoinedLabel();
        instance.mRingtone = alarm.alert;
        instance.mIncreasingVolume = alarm.increasingVolume;
        instance.mInstanceKind = AlarmInstance.KIND_PRE_REMINDER;
        instance.mPreOffsetMinutes = result.offsetMinutes;
        return instance;
    }

    public static Calendar getNextAlarmTime(Context context, Alarm alarm, Calendar currentTime) {
        return RepeatRuleEngine.parseOrLegacy(alarm)
                .getNextAlarmTime(context, alarm, currentTime);
    }

    public static Calendar getPreviousAlarmTime(Context context, Alarm alarm, Calendar currentTime) {
        return RepeatRuleEngine.parseOrLegacy(alarm)
                .getPreviousAlarmTime(context, alarm, currentTime);
    }

    public static boolean isRepeating(Alarm alarm) {
        return RepeatRuleEngine.parseOrLegacy(alarm).isRepeating();
    }
}
