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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public final class PreReminderResult {
    private static final PreReminderResult NONE =
            new PreReminderResult(null, Collections.emptyList(), 0);

    public final Calendar time;
    public final List<String> labels;
    public final int offsetMinutes;

    PreReminderResult(Calendar time, List<String> labels, int offsetMinutes) {
        this.time = time;
        this.labels = Collections.unmodifiableList(new ArrayList<>(labels));
        this.offsetMinutes = offsetMinutes;
    }

    public static PreReminderResult none() {
        return NONE;
    }

    public boolean hasReminder() {
        return time != null && offsetMinutes > 0 && !labels.isEmpty();
    }

    public String getJoinedLabel() {
        StringBuilder builder = new StringBuilder();
        for (String label : labels) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(label);
        }
        return builder.toString();
    }
}
