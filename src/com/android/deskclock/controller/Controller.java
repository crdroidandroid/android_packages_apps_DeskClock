/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.deskclock.controller;

import android.content.Context;

import com.android.deskclock.Utils;

public final class Controller {

    private static final Controller sController = new Controller();

    private Context mContext;

    private ShortcutController mShortcutController;

    private Controller() {}

    public static Controller getController() {
        return sController;
    }

    public void setContext(Context context) {
        if (mContext != null) {
            throw new IllegalStateException("context has already been set");
        }
        mContext = context;
        if (Utils.isNMR1OrLater()) {
            mShortcutController = new ShortcutController(mContext);
        }
    }

    public void updateShortcuts() {
        Utils.enforceMainLooper();
        if (mShortcutController != null) {
            mShortcutController.updateShortcuts();
        }
    }
}
