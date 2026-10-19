/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.videoplayer;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Locale;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.preference.SeekBarPreference;
import app.morphe.extension.shared.settings.preference.SeekBarPreference.SeekBarConfig;
import app.morphe.extension.shared.theme.ThemeUtils;
import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.ui.SheetBottomDialog;
import app.morphe.extension.youtube.patches.LegacyPlayerControlsPatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PipDismissHelper;

@SuppressWarnings("unused")
public class VideoDelayDialogButton {

    @Nullable
    private static LegacyPlayerControlButton legacy;

    private static WeakReference<TextView> overlayTextRef = new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            if (LegacyPlayerControlsPatch.RESTORE_OLD_PLAYER_BUTTONS ||
                    !Settings.VIDEO_DELAY_BUTTON.get()) {
                return;
            }

            overlayTextRef = new WeakReference<>(PlayerOverlayButton.addButtonWithTextOverlay(
                    controlsView,
                    getOnClickListener(),
                    getOnLongClickListener()
            ));

            updateButtonAppearance();
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void initializeLegacyButton(View controlsView) {
        try {
            if (!LegacyPlayerControlsPatch.RESTORE_OLD_PLAYER_BUTTONS) {
                return;
            }

            legacy = new LegacyPlayerControlButton(
                    controlsView,
                    "morphe_video_delay_dialog_button_container",
                    "morphe_video_delay_dialog_button",
                    "morphe_video_delay_dialog_button_text",
                    null,
                    Settings.VIDEO_DELAY_BUTTON,
                    getOnClickListener(),
                    getOnLongClickListener()
            );

            updateButtonAppearance();
        } catch (Exception ex) {
            Logger.printException(() -> "initializeLegacyButton failure", ex);
        }
    }

    private static View.OnClickListener getOnClickListener() {
        return view -> showDialog(view.getContext());
    }

    private static View.OnLongClickListener getOnLongClickListener() {
        return view -> {
            setDelay(0);
            return true;
        };
    }

    /**
     * Saves the delay and updates the player button.
     * The new delay applies immediately, as the renderers read it on every playback loop.
     */
    private static void setDelay(int delayMs) {
        Settings.VIDEO_DELAY.save(delayMs);
        updateButtonAppearance();
    }

    /**
     * Updates the button text to show the current delay.
     * Can be called from any thread.
     */
    public static void updateButtonAppearance() {
        Utils.runOnMainThread(() -> {
            try {
                String delayText = formatButtonText(SeekBarPreference.clampToRange(Settings.VIDEO_DELAY));
                if (legacy != null) {
                    legacy.setTextOverlay(delayText);
                }

                TextView overlay = overlayTextRef.get();
                if (overlay != null) {
                    overlay.setText(delayText);
                }
            } catch (Exception ex) {
                Logger.printException(() -> "updateButtonAppearance failure", ex);
            }
        });
    }

    /**
     * The button has room for about 4 characters, so longer delays are shown in seconds.
     */
    private static String formatButtonText(int delayMs) {
        if (delayMs < 1000) {
            return delayMs + "ms";
        }
        return String.format(Locale.ROOT, "%.1fs", delayMs / 1000f);
    }

    /**
     * Shows a dialog with a slider and a number field for adjusting the delay.
     * Changes are applied immediately, so the sync can be adjusted while the video plays.
     */
    private static void showDialog(Context context) {
        try {
            SeekBarConfig config = SeekBarPreference.configFor(Settings.VIDEO_DELAY);
            if (config == null) return;

            final int foregroundColor = ThemeUtils.getAppForegroundColor();

            SheetBottomDialog.DraggableLinearLayout mainLayout =
                    SheetBottomDialog.createMainLayout(context, LegacyPlayerControlButton.getDialogBackgroundColor());

            LinearLayout contentLayout = new LinearLayout(context);
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            contentLayout.setPadding(Dim.dp16, 0, Dim.dp16, Dim.dp16);

            TextView titleView = new TextView(context);
            titleView.setText(str("morphe_video_delay_title"));
            titleView.setTextColor(foregroundColor);
            titleView.setTextSize(16);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            titleView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            titleParams.setMargins(0, Dim.dp20, 0, Dim.dp8);
            titleView.setLayoutParams(titleParams);

            SeekBar slider = new SeekBar(context);
            slider.setMax((config.max() - config.min()) / config.step());
            slider.setProgress(SeekBarPreference.valueToProgress(config, Settings.VIDEO_DELAY.get()));
            slider.getProgressDrawable().setColorFilter(
                    new PorterDuffColorFilter(foregroundColor, PorterDuff.Mode.SRC_IN));
            slider.getThumb().setColorFilter(
                    new PorterDuffColorFilter(foregroundColor, PorterDuff.Mode.SRC_IN));
            LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sliderParams.setMargins(0, Dim.dp8, 0, Dim.dp16);
            slider.setLayoutParams(sliderParams);

            EditText inputField = new EditText(context);
            inputField.setHint(str("morphe_video_delay_dialog_input_hint"));
            inputField.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputField.setImeOptions(EditorInfo.IME_ACTION_DONE);
            inputField.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(String.valueOf(config.max()).length())
            });
            inputField.setTextColor(foregroundColor);
            inputField.setHintTextColor(Color.GRAY);
            inputField.setBackgroundColor(Color.TRANSPARENT);
            inputField.setPadding(0, 0, 0, 0);
            inputField.setText(String.valueOf(SeekBarPreference.clampToRange(Settings.VIDEO_DELAY)));
            inputField.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView unitView = new TextView(context);
            unitView.setText(config.unit());
            unitView.setTextColor(Color.GRAY);

            ShapeDrawable fieldBackground = new ShapeDrawable(new RoundRectShape(
                    Dim.roundedCorners(10), null, null));
            fieldBackground.getPaint().setColor(ThemeUtils.getEditTextBackground());

            LinearLayout fieldBox = new LinearLayout(context);
            fieldBox.setOrientation(LinearLayout.HORIZONTAL);
            fieldBox.setGravity(Gravity.CENTER_VERTICAL);
            fieldBox.setBackground(fieldBackground);
            fieldBox.setPadding(Dim.dp12, Dim.dp8, Dim.dp12, Dim.dp8);
            fieldBox.addView(inputField);
            fieldBox.addView(unitView);

            // Keeps the slider and the number field from updating each other in a loop.
            final boolean[] isUpdating = {false};

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || isUpdating[0]) return;
                    final int delayMs = SeekBarPreference.progressToValue(config, progress);
                    setDelay(delayMs);

                    isUpdating[0] = true;
                    inputField.setText(String.valueOf(delayMs));
                    inputField.setSelection(inputField.getText().length());
                    isUpdating[0] = false;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            inputField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdating[0]) return;
                    final int delayMs;
                    try {
                        delayMs = Math.max(config.min(), Math.min(config.max(),
                                Integer.parseInt(s.toString())));
                    } catch (NumberFormatException ex) {
                        return; // Empty field.
                    }
                    setDelay(delayMs);

                    isUpdating[0] = true;
                    slider.setProgress(SeekBarPreference.valueToProgress(config, delayMs));
                    isUpdating[0] = false;
                }
            });

            contentLayout.addView(titleView);
            contentLayout.addView(slider);
            contentLayout.addView(fieldBox);
            mainLayout.addView(contentLayout);

            SheetBottomDialog.SlideDialog dialog = SheetBottomDialog.createSlideDialog(
                    context, mainLayout, LegacyPlayerControlButton.fadeInDuration);

            // Shows the clamped value if a number outside the range was typed.
            dialog.setOnDismissListener(d -> updateButtonAppearance());

            PipDismissHelper.dismissOnPip(dialog);
            dialog.show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }
}
