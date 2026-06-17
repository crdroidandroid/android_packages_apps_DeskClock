/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.alarms;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.android.deskclock.data.AlarmScheduleCalculator;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.PreReminderUiModel;
import com.android.deskclock.data.RepeatRules;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.ringtone.RingtonePickerActivity;

import java.util.Calendar;
import java.util.List;

/**
 * Click handler for an alarm time item.
 */
public final class AlarmTimeClickHandler {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AlarmTimeClickHandler");

    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";

    private final Fragment mFragment;
    private final Context mContext;
    private final AlarmUpdateHandler mAlarmUpdateHandler;
    private final ScrollHandler mScrollHandler;

    private Alarm mSelectedAlarm;
    private Bundle mPreviousDaysOfWeekMap;

    public AlarmTimeClickHandler(Fragment fragment, Bundle savedState,
            AlarmUpdateHandler alarmUpdateHandler, ScrollHandler smoothScrollController) {
        mFragment = fragment;
        mContext = mFragment.getActivity() != null
                ? mFragment.getActivity().getApplicationContext()
                : null;
        mAlarmUpdateHandler = alarmUpdateHandler;
        mScrollHandler = smoothScrollController;
        if (savedState != null) {
            mPreviousDaysOfWeekMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
        }
        if (mPreviousDaysOfWeekMap == null) {
            mPreviousDaysOfWeekMap = new Bundle();
        }
    }

    public void setSelectedAlarm(Alarm selectedAlarm) {
        mSelectedAlarm = selectedAlarm;
    }

    public void saveInstance(Bundle outState) {
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mPreviousDaysOfWeekMap);
    }

    public void setAlarmEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.enabled) {
            alarm.enabled = newState;
            Events.sendAlarmEvent(newState ? R.string.action_enable : R.string.action_disable,
                    R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);
            LOGGER.d("Updating alarm enabled state to " + newState);
        }
    }

    public void setAlarmVibrationEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.vibrate) {
            alarm.vibrate = newState;
            Events.sendAlarmEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Updating vibrate state to " + newState);

            if (newState) {
                // Buzz the vibrator to preview the alarm firing behavior.
                final Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if (v.hasVibrator()) {
                    v.vibrate(VibrationEffect.createOneShot(300,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }
        }
    }

    public void setDayOfWeekEnabled(Alarm alarm, boolean checked, int index) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);
        final List<PreReminderUiModel> preReminders = PreReminderUiModel.fromAlarm(alarm);

        final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(index);
        alarm.repeatRule = null;
        alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, checked);
        alarm.repeatRule = PreReminderUiModel.applyToRepeatRule(alarm.repeatRule, preReminders);

        // if the change altered the next scheduled alarm time, tell the user
        final Calendar newNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);
    }

    public void setWorkdayRepeatEnabled(Alarm alarm, boolean enabled) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);
        final List<PreReminderUiModel> preReminders = PreReminderUiModel.fromAlarm(alarm);

        if (enabled) {
            alarm.repeatRule = RepeatRules.workday(
                    RepeatRules.CALENDAR_CN_MAINLAND, RepeatRules.FALLBACK_MON_TO_FRI);
            alarm.daysOfWeek = alarm.daysOfWeek
                    .setBit(Calendar.MONDAY, true)
                    .setBit(Calendar.TUESDAY, true)
                    .setBit(Calendar.WEDNESDAY, true)
                    .setBit(Calendar.THURSDAY, true)
                    .setBit(Calendar.FRIDAY, true)
                    .setBit(Calendar.SATURDAY, false)
                    .setBit(Calendar.SUNDAY, false);
        } else {
            alarm.repeatRule = null;
        }
        alarm.repeatRule = PreReminderUiModel.applyToRepeatRule(alarm.repeatRule, preReminders);

        final Calendar newNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);
    }

    public void setBasicPreReminderEnabled(Alarm alarm, boolean enabled) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);

        List<PreReminderUiModel> reminders = PreReminderUiModel.fromAlarm(alarm);
        if (enabled && reminders.isEmpty()) {
            reminders.add(PreReminderUiModel.createDateInterval(oldNextAlarmTime,
                    mContext.getString(R.string.pre_reminder_default_label)));
        } else {
            for (PreReminderUiModel reminder : reminders) {
                reminder.enabled = enabled;
            }
        }
        alarm.repeatRule = PreReminderUiModel.applyToRepeatRule(alarm.repeatRule, reminders);

        final Calendar newNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);
    }

    public void onPreReminderConfigClicked(Alarm alarm) {
        final Context context = mFragment.requireContext();
        final List<PreReminderUiModel> reminders = PreReminderUiModel.fromAlarm(alarm);
        final AlertDialog[] dialogRef = new AlertDialog[1];
        final LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        final int padding = context.getResources().getDimensionPixelSize(
                R.dimen.label_edittext_padding);
        list.setPadding(padding, padding / 2, padding, padding / 2);

        if (!reminders.isEmpty()) {
            list.addView(createPreReminderSectionLabel(context,
                    R.string.pre_reminder_configured_rules));
            for (int i = 0; i < reminders.size(); i++) {
                final int index = i;
                list.addView(createPreReminderRuleRow(context, reminders.get(i), v -> {
                    dialogRef[0].dismiss();
                    showPreReminderEditor(alarm, reminders, index);
                }));
            }
        } else {
            TextView empty = createPreReminderSectionLabel(context, R.string.pre_reminder_empty);
            list.addView(empty);
        }

        list.addView(createPreReminderSectionLabel(context, R.string.pre_reminder_add_rule_section));

        list.addView(createPreReminderAddRuleRow(context,
                R.string.pre_reminder_add_date_interval, v -> {
            Calendar next = AlarmScheduleCalculator.getNextAlarmTime(
                    mContext, alarm, Calendar.getInstance());
            reminders.add(PreReminderUiModel.createDateInterval(next,
                    context.getString(R.string.pre_reminder_default_label)));
            savePreReminders(alarm, reminders);
            dialogRef[0].dismiss();
            showPreReminderEditor(alarm, reminders, reminders.size() - 1);
        }));

        list.addView(createPreReminderAddRuleRow(context,
                R.string.pre_reminder_add_weekly, v -> {
            Calendar next = AlarmScheduleCalculator.getNextAlarmTime(
                    mContext, alarm, Calendar.getInstance());
            reminders.add(PreReminderUiModel.createWeekly(next,
                    context.getString(R.string.pre_reminder_default_label)));
            savePreReminders(alarm, reminders);
            dialogRef[0].dismiss();
            showPreReminderEditor(alarm, reminders, reminders.size() - 1);
        }));

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(list);

        dialogRef[0] = new AlertDialog.Builder(context)
                .setTitle(R.string.pre_reminder_configure)
                .setView(scrollView)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialogRef[0].show();
    }

    private TextView createPreReminderSectionLabel(Context context, int textRes) {
        TextView label = new TextView(context);
        label.setText(textRes);
        label.setTextAppearance(R.style.body);
        label.setAllCaps(false);
        final int top = context.getResources().getDimensionPixelSize(
                R.dimen.alarm_clock_expanded_vertical_margin);
        label.setPadding(0, top, 0, top / 2);
        return label;
    }

    private View createPreReminderRuleRow(Context context, PreReminderUiModel reminder,
            View.OnClickListener listener) {
        final int padding = context.getResources().getDimensionPixelSize(
                R.dimen.alarm_horizontal_padding);
        final int smallPadding = context.getResources().getDimensionPixelSize(
                R.dimen.alarm_clock_expanded_vertical_margin);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(context.getResources().getDimensionPixelSize(
                R.dimen.touch_target_min_size));
        row.setPadding(padding, smallPadding, padding / 2, smallPadding);
        row.setBackgroundResource(R.drawable.pre_reminder_rule_card_background);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);
        row.setContentDescription(context.getString(
                R.string.pre_reminder_edit_rule, reminder.title(context)));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText(reminder.title(context));
        title.setTextAppearance(R.style.body);
        title.setSingleLine(true);
        texts.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView summary = new TextView(context);
        summary.setText(reminder.summary(context));
        summary.setTextColor(context.getColor(R.color.white_63p));
        summary.setSingleLine(true);
        summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(summary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final String secondary = reminder.secondarySummary(context);
        if (!secondary.isEmpty()) {
            TextView secondaryView = new TextView(context);
            secondaryView.setText(secondary);
            secondaryView.setTextColor(context.getColor(R.color.white_63p));
            secondaryView.setSingleLine(true);
            secondaryView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(secondaryView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        row.addView(texts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView chevron = new ImageView(context);
        chevron.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chevron_right_24dp));
        row.addView(chevron, new LinearLayout.LayoutParams(
                context.getResources().getDimensionPixelSize(R.dimen.touch_target_min_size),
                context.getResources().getDimensionPixelSize(R.dimen.touch_target_min_size)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, smallPadding);
        row.setLayoutParams(params);
        return row;
    }

    private View createPreReminderAddRuleRow(Context context, int titleRes,
            View.OnClickListener listener) {
        final int padding = context.getResources().getDimensionPixelSize(
                R.dimen.alarm_horizontal_padding);
        final int smallPadding = context.getResources().getDimensionPixelSize(
                R.dimen.alarm_clock_expanded_vertical_margin);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(context.getResources().getDimensionPixelSize(
                R.dimen.touch_target_min_size));
        row.setPadding(padding, 0, padding, 0);
        row.setBackgroundResource(R.drawable.pre_reminder_rule_card_background);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);

        ImageView icon = new ImageView(context);
        icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_add_24dp));
        icon.setColorFilter(context.getColor(R.color.accent_color));
        row.addView(icon, new LinearLayout.LayoutParams(
                context.getResources().getDimensionPixelSize(R.dimen.touch_target_min_size),
                context.getResources().getDimensionPixelSize(R.dimen.touch_target_min_size)));

        TextView title = new TextView(context);
        title.setText(titleRes);
        title.setTextAppearance(R.style.body);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, smallPadding);
        row.setLayoutParams(params);
        return row;
    }

    private AppCompatEditText addField(LinearLayout parent, String hint, int inputType) {
        final Context context = parent.getContext();
        final TextView label = new TextView(context);
        label.setText(hint);
        parent.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AppCompatEditText field = new AppCompatEditText(context);
        field.setSingleLine(true);
        field.setSelectAllOnFocus(true);
        field.setInputType(inputType);
        parent.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }

    private void showPreReminderEditor(Alarm alarm, List<PreReminderUiModel> reminders,
            int index) {
        if (index < 0 || index >= reminders.size()) {
            return;
        }
        final Context context = mFragment.requireContext();
        final PreReminderUiModel model = reminders.get(index);
        final LinearLayout fields = new LinearLayout(context);
        fields.setOrientation(LinearLayout.VERTICAL);
        final int padding = context.getResources()
                .getDimensionPixelSize(R.dimen.label_edittext_padding);
        fields.setPadding(padding, 0, padding, 0);

        final AppCompatEditText label = addField(fields,
                context.getString(R.string.pre_reminder_label_field),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        label.setText(model.label);

        final AppCompatEditText offset = addField(fields,
                context.getString(R.string.pre_reminder_offset_field),
                InputType.TYPE_CLASS_NUMBER);
        offset.setText(String.valueOf(model.offsetMinutes));

        final RadioGroup combineMode = new RadioGroup(context);
        final RadioButton additive = new RadioButton(context);
        additive.setId(android.view.View.generateViewId());
        additive.setText(R.string.pre_reminder_combine_additive);
        final RadioButton absolute = new RadioButton(context);
        absolute.setId(android.view.View.generateViewId());
        absolute.setText(R.string.pre_reminder_combine_absolute);
        combineMode.addView(additive);
        combineMode.addView(absolute);
        combineMode.check(model.additive ? additive.getId() : absolute.getId());
        fields.addView(combineMode);

        final RadioGroup selectorType = new RadioGroup(context);
        final RadioButton dateInterval = new RadioButton(context);
        dateInterval.setId(android.view.View.generateViewId());
        dateInterval.setText(R.string.pre_reminder_selector_date_interval);
        final RadioButton weekly = new RadioButton(context);
        weekly.setId(android.view.View.generateViewId());
        weekly.setText(R.string.pre_reminder_selector_weekly);
        selectorType.addView(dateInterval);
        selectorType.addView(weekly);
        selectorType.check(PreReminderUiModel.SELECTOR_WEEKLY.equals(model.selectorType)
                ? weekly.getId() : dateInterval.getId());
        fields.addView(selectorType);

        final LinearLayout dateIntervalFields = new LinearLayout(context);
        dateIntervalFields.setOrientation(LinearLayout.VERTICAL);
        fields.addView(dateIntervalFields);

        final AppCompatEditText interval = addField(dateIntervalFields,
                context.getString(R.string.pre_reminder_interval_field),
                InputType.TYPE_CLASS_NUMBER);
        interval.setText(String.valueOf(model.intervalDays));

        final Button anchor = new Button(context);
        anchor.setText(context.getString(R.string.pre_reminder_anchor_value, model.anchorDate));
        anchor.setOnClickListener(v -> showDatePicker(anchor, model));
        dateIntervalFields.addView(anchor);

        final LinearLayout weeklyFields = new LinearLayout(context);
        weeklyFields.setOrientation(LinearLayout.VERTICAL);
        fields.addView(weeklyFields);

        final CheckBox mon = addWeekday(weeklyFields, R.string.pre_reminder_weekday_mon,
                model.weekdays.isBitOn(Calendar.MONDAY));
        final CheckBox tue = addWeekday(weeklyFields, R.string.pre_reminder_weekday_tue,
                model.weekdays.isBitOn(Calendar.TUESDAY));
        final CheckBox wed = addWeekday(weeklyFields, R.string.pre_reminder_weekday_wed,
                model.weekdays.isBitOn(Calendar.WEDNESDAY));
        final CheckBox thu = addWeekday(weeklyFields, R.string.pre_reminder_weekday_thu,
                model.weekdays.isBitOn(Calendar.THURSDAY));
        final CheckBox fri = addWeekday(weeklyFields, R.string.pre_reminder_weekday_fri,
                model.weekdays.isBitOn(Calendar.FRIDAY));
        final CheckBox sat = addWeekday(weeklyFields, R.string.pre_reminder_weekday_sat,
                model.weekdays.isBitOn(Calendar.SATURDAY));
        final CheckBox sun = addWeekday(weeklyFields, R.string.pre_reminder_weekday_sun,
                model.weekdays.isBitOn(Calendar.SUNDAY));

        setPreReminderSelectorFieldsVisibility(
                selectorType.getCheckedRadioButtonId(), weekly.getId(),
                dateIntervalFields, weeklyFields);
        selectorType.setOnCheckedChangeListener((group, checkedId) ->
                setPreReminderSelectorFieldsVisibility(
                        checkedId, weekly.getId(), dateIntervalFields, weeklyFields));

        new AlertDialog.Builder(context)
                .setTitle(R.string.pre_reminder_rule)
                .setView(fields)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    model.enabled = true;
                    model.label = label.getText().toString();
                    model.offsetMinutes = PreReminderUiModel.clamp(parseInt(
                            offset.getText().toString(), 30), 1,
                            com.android.deskclock.data.PreReminder.MAX_OFFSET_MINUTES);
                    model.additive = combineMode.getCheckedRadioButtonId() == additive.getId();
                    model.selectorType = selectorType.getCheckedRadioButtonId() == weekly.getId()
                            ? PreReminderUiModel.SELECTOR_WEEKLY
                            : PreReminderUiModel.SELECTOR_DATE_INTERVAL;
                    model.intervalDays = Math.max(1, parseInt(interval.getText().toString(), 2));
                    model.weekdays = Weekdays.NONE
                            .setBit(Calendar.MONDAY, mon.isChecked())
                            .setBit(Calendar.TUESDAY, tue.isChecked())
                            .setBit(Calendar.WEDNESDAY, wed.isChecked())
                            .setBit(Calendar.THURSDAY, thu.isChecked())
                            .setBit(Calendar.FRIDAY, fri.isChecked())
                            .setBit(Calendar.SATURDAY, sat.isChecked())
                            .setBit(Calendar.SUNDAY, sun.isChecked());
                    savePreReminders(alarm, reminders);
                })
                .setNeutralButton(R.string.delete, (dialog, which) -> {
                    reminders.remove(index);
                    savePreReminders(alarm, reminders);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setPreReminderSelectorFieldsVisibility(int checkedId, int weeklyId,
            View dateIntervalFields, View weeklyFields) {
        final boolean isWeekly = checkedId == weeklyId;
        dateIntervalFields.setVisibility(isWeekly ? View.GONE : View.VISIBLE);
        weeklyFields.setVisibility(isWeekly ? View.VISIBLE : View.GONE);
    }

    private CheckBox addWeekday(LinearLayout fields, int labelRes, boolean checked) {
        CheckBox checkBox = new CheckBox(fields.getContext());
        checkBox.setText(labelRes);
        checkBox.setChecked(checked);
        fields.addView(checkBox);
        return checkBox;
    }

    private void showDatePicker(Button anchor, PreReminderUiModel model) {
        Calendar selectedDate;
        try {
            selectedDate = com.android.deskclock.data.CalendarDateUtils.parseYmd(
                    model.anchorDate, Calendar.getInstance());
        } catch (IllegalArgumentException ignored) {
            selectedDate = Calendar.getInstance();
        }

        final Calendar calendar = selectedDate;
        new DatePickerDialog(mFragment.requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            model.anchorDate = PreReminderUiModel.formatYmd(calendar);
            anchor.setText(anchor.getContext().getString(
                    R.string.pre_reminder_anchor_value, model.anchorDate));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void savePreReminders(Alarm alarm, List<PreReminderUiModel> reminders) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);
        alarm.repeatRule = PreReminderUiModel.applyToRepeatRule(alarm.repeatRule, reminders);

        final Calendar newNextAlarmTime = AlarmScheduleCalculator.getNextAlarmTime(
                mContext, alarm, now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException ignored) {
            return fallback;
        }
    }

    public void onDeleteClicked(AlarmItemHolder itemHolder) {
        if (mFragment instanceof AlarmClockFragment) {
            ((AlarmClockFragment) mFragment).removeItem(itemHolder);
        }
        final Alarm alarm = itemHolder.item;
        Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
        mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
        LOGGER.d("Deleting alarm.");
    }

    public void onClockClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);
        TimePickerDialogFragment.show(mFragment, alarm.hour, alarm.minutes);
    }

    public void dismissAlarmInstance(AlarmInstance alarmInstance) {
        final Intent dismissIntent = AlarmStateManager.createStateChangeIntent(
                mContext, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance,
                AlarmInstance.PREDISMISSED_STATE);
        mContext.startService(dismissIntent);
        mAlarmUpdateHandler.showPredismissToast(alarmInstance);
    }

    public void onRingtoneClicked(Context context, Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);

        final Intent intent =
                RingtonePickerActivity.createAlarmRingtonePickerIntent(context, alarm);
        context.startActivity(intent);
    }

    public void onEditLabelClicked(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);
        final LabelDialogFragment fragment =
                LabelDialogFragment.newInstance(alarm, alarm.label, mFragment.getTag());
        LabelDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void onTimeSet(int hourOfDay, int minute) {
        if (mSelectedAlarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            final Alarm a = new Alarm();
            a.hour = hourOfDay;
            a.minutes = minute;
            a.enabled = true;
            mAlarmUpdateHandler.asyncAddAlarm(a);
        } else {
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mSelectedAlarm.enabled = true;
            mScrollHandler.setSmoothScrollStableId(mSelectedAlarm.id);
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            mSelectedAlarm = null;
        }
    }
}
