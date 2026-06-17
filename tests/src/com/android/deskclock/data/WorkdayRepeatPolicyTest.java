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

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(AndroidJUnit4ClassRunner.class)
public class WorkdayRepeatPolicyTest {
    @Test
    public void showsChinaMainlandWorkdayRepeatInChinaRegion() {
        assertTrue(WorkdayRepeatPolicy.shouldShowChinaMainlandWorkdayRepeat(
                new Locale("en", "CN"), false));
    }

    @Test
    public void hidesChinaMainlandWorkdayRepeatOutsideChinaRegion() {
        assertFalse(WorkdayRepeatPolicy.shouldShowChinaMainlandWorkdayRepeat(
                Locale.US, false));
    }

    @Test
    public void showsChinaMainlandWorkdayRepeatInDevelopmentBuilds() {
        assertTrue(WorkdayRepeatPolicy.shouldShowChinaMainlandWorkdayRepeat(
                Locale.US, true));
    }
}
