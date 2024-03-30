/*
 * Copyright (c) 2017, Sensirion AG
 * Copyright (c) 2024, Draekko RAND
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Sensirion AG nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.sensirion.smartgadget.view.device_management;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.sensirion.libsmartgadget.Gadget;
import com.sensirion.libsmartgadget.GadgetDataPoint;
import com.sensirion.libsmartgadget.GadgetDownloadService;
import com.sensirion.libsmartgadget.GadgetListener;
import com.sensirion.libsmartgadget.GadgetService;
import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.smartgadget.BatteryService;
import com.sensirion.smartgadget.R;
import com.sensirion.smartgadget.peripheral.rht_sensor.external.RHTHumigadgetSensorManager;
import com.sensirion.smartgadget.persistence.device_name_database.DeviceNameDatabaseManager;
import com.sensirion.smartgadget.utils.DeviceModel;
import com.sensirion.smartgadget.utils.Interval;
import com.sensirion.smartgadget.utils.TimeFormatter;
import com.sensirion.smartgadget.utils.download.LoggerInterval;
import com.sensirion.smartgadget.utils.view.ParentFragment;
import com.sensirion.smartgadget.view.MainActivity;

import java.util.List;
import java.util.Locale;

// TODO: Requires improved initUiElements calling behavior...
public class ManageDeviceFragment
        extends ParentFragment
        implements GadgetListener {
    private static final String TAG = ManageDeviceFragment.class.getSimpleName();
    public static final int UNKNOWN_BATTERY_LEVEL = -1;
    private static final int UNKNOWN_LOGGING_INTERVAL = -1;
    private static final int DOWNLOAD_COMPLETE_RESET_DELAY_MS = 2000;

    private Gadget mSelectedGadget;
    private DeviceModel mSelectedDeviceModel;
    private Runnable mDownloadButtonReset;

    // XML Resources
    boolean IS_TABLET;
    String ENABLE_LOGGING_STRING;
    String GADGET_ENABLE_ADVICE_STRING;
    String INTERVAL_MODIFICATION_TITLE;
    String INTERVAL_MODIFICATION_MESSAGE;
    String YES_STRING;
    String NO_STRING;
    String TYPEFACE_CONDENSED_LOCATION;
    String TYPEFACE_BOLD_LOCATION;

    // XML Views
    Button mBatteryBoardBoardView;
    Button mLoggingBoardBoardView;
    Button mIntervalBoardView;
    Button mDownloadBoardView;

    EditText mGadgetNameEditText;
    Button mDisconnectButton;
    Button mLoggingIntervalButton;
    TextView mBatteryLevelValue;
    ProgressBar mBatteryBar;
    Switch mLoggingToggle;
    RelativeLayout mBatteryLevelLayout;
    RelativeLayout mLoggingLayout;
    TextView mDownloadButtonText;
    ProgressBar mDownloadProgressBar;
    TextView mGadgetType;

    int mColorSensirionGreenDarkened;
    int mColorYellow;
    int mColorOrange;
    int mColorRed;
    int mColorLightGray;
    Drawable mDownloadProgressDrawable;
    ColorStateList mDeviceButtonColors;

    private Context mContext;

    public ManageDeviceFragment() {
    }

    /*
     * Initialization
     */

    /**
     * This method should be called before starting the class.
     *
     * @param deviceAddress device of the device that the users wants to manage.
     */
    public void init(@NonNull final String deviceAddress) {
        mSelectedDeviceModel = RHTHumigadgetSensorManager.getInstance().getConnectedDevice(deviceAddress);
        mSelectedGadget = RHTHumigadgetSensorManager.getInstance().getConnectedGadget(deviceAddress);
    }

    /*
     * Lifecycle Methods
     */

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IS_TABLET = mContext.getResources().getBoolean(R.bool.is_tablet);
        TYPEFACE_CONDENSED_LOCATION = mContext.getResources().getString(R.string.typeface_condensed);
        TYPEFACE_BOLD_LOCATION = mContext.getResources().getString(R.string.typeface_bold);

        ENABLE_LOGGING_STRING = mContext.getResources().getString(R.string.enable_logging);
        GADGET_ENABLE_ADVICE_STRING = mContext.getResources().getString(R.string.label_advice_logging_enable);
        INTERVAL_MODIFICATION_TITLE = mContext.getResources().getString(R.string.interval_modification);
        INTERVAL_MODIFICATION_MESSAGE = mContext.getResources().getString(R.string.interval_modification_message);
        YES_STRING = mContext.getResources().getString(R.string.yes);
        NO_STRING = mContext.getResources().getString(R.string.no);

        mColorSensirionGreenDarkened = ContextCompat.getColor(mContext, R.color.blegadget_blue_darkened);
        mColorYellow = ContextCompat.getColor(mContext, R.color.yellow);
        mColorOrange = ContextCompat.getColor(mContext, R.color.orange);
        mColorRed = ContextCompat.getColor(mContext, R.color.red);
        mColorLightGray = ContextCompat.getColor(mContext, R.color.light_gray);

        mDownloadProgressDrawable = ContextCompat.getDrawable(mContext, R.drawable.download_progress);
        mDeviceButtonColors = ContextCompat.getColorStateList(mContext, R.color.manage_device_button);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_manage_device, container, false);

        mBatteryBoardBoardView = view.findViewById(R.id.dashboard_battery_bar);
        mLoggingBoardBoardView = view.findViewById(R.id.dashboard_gadget_logging);
        mIntervalBoardView = view.findViewById(R.id.dashboard_logging_interval);
        mDownloadBoardView = view.findViewById(R.id.dashboard_download_progress);
        mGadgetNameEditText = view.findViewById(R.id.manage_device_gadget_name_edit_field);
        mDisconnectButton = view.findViewById(R.id.manage_device_button_disconnect);
        mLoggingIntervalButton = view.findViewById(R.id.manage_device_button_logging_interval);
        mBatteryLevelValue = view.findViewById(R.id.manage_device_battery_level_value);
        mBatteryBar = view.findViewById(R.id.manage_device_battery_bar);
        mLoggingToggle = view.findViewById(R.id.manage_device_switch_toggle_logger);
        mBatteryLevelLayout = view.findViewById(R.id.manage_device_battery_bar_layout);
        mLoggingLayout = view.findViewById(R.id.manage_device_gadget_logging_layout);
        mDownloadButtonText = view.findViewById(R.id.manage_device_download_progress);
        mDownloadProgressBar = view.findViewById(R.id.manage_device_download_progress_bar);
        mGadgetType = view.findViewById(R.id.manage_device_gdaget_type);

        view.setOnTouchListener(new OnTouchOpenTabletMenuListener());
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSelectedDeviceAvailable()) {
            mSelectedGadget.addListener(this);
            initUiElements();
        } else {
            closeScreen();
        }
    }

    @Override
    public void onPause() {
        if (isSelectedDeviceAvailable()) {
            assert mSelectedGadget != null;
            mSelectedGadget.removeListener(this);
        }
        if (mDownloadButtonReset != null) {
            mDownloadButtonText.removeCallbacks(mDownloadButtonReset);
        }
        super.onPause();
    }

    private boolean isSelectedDeviceAvailable() {
        return mSelectedDeviceModel != null && mSelectedGadget != null && mSelectedGadget.isConnected();
    }

    private void closeScreen() {
        if (mGadgetNameEditText.hasFocus()) {
            mGadgetNameEditText.clearFocus();
        }
        final Activity activity = getParent();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).changeFragment(new ScanDeviceFragment());
        }
    }

    /*
     * Implementation of {@link GadgetListener}
     */

    @Override
    public void onGadgetConnected(@NonNull Gadget gadget) {
        // ignore... the screen shouldn't even open if not connected -> see onResume()
    }

    @Override
    public void onGadgetDisconnected(@NonNull Gadget gadget) {
        closeScreen();
    }

    @Override
    public void onGadgetValuesReceived(@NonNull Gadget gadget, @NonNull GadgetService service, @NonNull GadgetValue[] values) {
        if (service instanceof BatteryService) {
            updateBatteryLevel();
        }
    }

    @Override
    public void onGadgetDownloadDataReceived(@NonNull Gadget gadget, @NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress) {

    }

    @Override
    public void onGadgetNewDataPoint(@NonNull Gadget gadget, @NonNull GadgetService service, GadgetDataPoint dataPoint) {
        // Ignore...
    }

    @Override
    public void onGadgetDownloadNewDataPoints(@NonNull Gadget gadget, @NonNull GadgetDownloadService service, @NonNull GadgetDataPoint[] dataPoints) {
        // Ignore...
    }

    @Override
    public void onGadgetDownloadProgress(@NonNull Gadget gadget, @NonNull GadgetDownloadService service, int progress) {
        mDownloadProgressBar.setProgressDrawable(mDownloadProgressDrawable);
        mDownloadButtonText.setText(String.format(Locale.GERMAN, getString(R.string.manage_device_download_progress), progress));
        mDownloadProgressBar.setProgress(progress);
    }

    @Override
    public void onSetGadgetLoggingEnabledFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        initUiElements();
    }

    @Override
    public void onSetLoggerIntervalSuccess(@NonNull final Gadget gadget) {
        final int valueInMilliseconds = getLoggerInterval(gadget);
        if (valueInMilliseconds == UNKNOWN_LOGGING_INTERVAL) {
            return;
        }
        final int intervalSeconds = valueInMilliseconds / Interval.ONE_SECOND.getNumberMilliseconds();
        mLoggingIntervalButton.setText(new TimeFormatter(intervalSeconds).getShortTime(getContext().getApplicationContext()));
        if (!isDownloading(gadget)) {
            mLoggingIntervalButton.setEnabled(true);
        }
    }

    @Override
    public void onSetLoggerIntervalFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        initUiElements();
        mLoggingIntervalButton.setTextColor(mColorOrange);
    }

    @Override
    public void onDownloadFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        mDownloadButtonText.setTextColor(mColorOrange);
        resetAfterDownload(isLoggingStateEditable(gadget), R.string.manage_device_download_failed_retry);
    }

    @Override
    public void onDownloadCompleted(@NonNull final Gadget gadget, @NonNull GadgetDownloadService service) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetAfterDownload(isLoggingStateEditable(gadget), R.string.manage_device_download_completed);
            }
        });
    }

    @Override
    public void onDownloadNoData(@NonNull final Gadget gadget, @NonNull final GadgetDownloadService service) {
        mDownloadButtonText.setTextColor(mColorOrange);
        resetAfterDownload(isLoggingStateEditable(gadget), R.string.manage_device_download_no_data);
    }

    /*
     * Private helpers
     */

    private void resetAfterDownload(boolean isLoggingStateEditable, int stringId) {
        mDownloadProgressBar.setProgress(0);
        mDownloadButtonText.setText(stringId);

        if (isLoggingStateEditable) {
            mLoggingToggle.setEnabled(true);
            mLoggingIntervalButton.setEnabled(!mLoggingToggle.isChecked());
        } else {
            mLoggingIntervalButton.setEnabled(true);
        }

        mDownloadButtonReset = new Runnable() {
            @Override
            public void run() {
                mDownloadProgressBar.setVisibility(View.GONE);
                mDownloadButtonText.setText(R.string.label_download);
                mDownloadButtonText.setEnabled(true);
                mDownloadButtonText.setTextColor(mDeviceButtonColors);
            }
        };
        mDownloadButtonText.postDelayed(mDownloadButtonReset, DOWNLOAD_COMPLETE_RESET_DELAY_MS);
    }

    /*
     * Manage Gadget Methods
     */

    private void setLoggerInterval(@NonNull final Gadget gadget, final int valueInMilliseconds) {
        final GadgetDownloadService downloadService = getDownloadService(gadget);
        if (downloadService == null) {
            return;
        }
        downloadService.setLoggerInterval(valueInMilliseconds);
        mLoggingIntervalButton.setEnabled(false);
        mLoggingIntervalButton.setText(R.string.label_logging_interval_updating);
    }

    private int getLoggerInterval(Gadget gadget) {
        final GadgetDownloadService downloadService = getDownloadService(gadget);
        if (downloadService == null) {
            return UNKNOWN_LOGGING_INTERVAL;
        }
        return downloadService.getLoggerInterval();
    }

    private boolean isLoggingStateEnabled(Gadget gadget) {
        final GadgetDownloadService downloadService = getDownloadService(gadget);
        return downloadService != null && downloadService.isGadgetLoggingEnabled();
    }

    private boolean isLoggingStateEditable(Gadget gadget) {
        final GadgetDownloadService downloadService = getDownloadService(gadget);
        return downloadService != null && downloadService.isGadgetLoggingStateEditable();
    }

    private void setLoggingStateEnabled(Gadget gadget, boolean enabled) {
        final GadgetDownloadService downloadService = getDownloadService(gadget);
        if (downloadService == null) {
            return;
        }
        downloadService.setGadgetLoggingEnabled(enabled);
    }

    private boolean isDownloadingEnabled(final Gadget gadget) {
        return getDownloadService(gadget) != null;
    }

    private boolean isDownloading(final Gadget gadget) {
        final GadgetDownloadService downloadService = getDownloadService(gadget);
        return downloadService != null && downloadService.isDownloading();
    }

    private void downloadLog(Gadget gadget) {
        final GadgetDownloadService downloadService = getDownloadService(gadget);
        if (downloadService == null) {
            return;
        }
        mDownloadButtonText.setText(R.string.manage_device_download_start);
        mDownloadProgressBar.setProgress(0);
        mDownloadProgressBar.setVisibility(View.VISIBLE);
        downloadService.download();
    }

    private int getBatteryLevel(Gadget gadget) {
        final GadgetService batteryService = getServiceOfType(gadget, BatteryService.class);
        if (batteryService == null) {
            return UNKNOWN_BATTERY_LEVEL;
        }
        final GadgetValue[] lastValues = batteryService.getLastValues();
        return (lastValues.length > 0) ? lastValues[0].getValue().intValue() : UNKNOWN_BATTERY_LEVEL;
    }

    private GadgetDownloadService getDownloadService(@NonNull final Gadget gadget) {
        return (GadgetDownloadService) getServiceOfType(gadget, GadgetDownloadService.class);
    }

    private GadgetService getServiceOfType(@NonNull final Gadget gadget,
                                           @NonNull final Class<? extends GadgetService> gadgetServiceClass) {
        final List<GadgetService> services = gadget.getServicesOfType(gadgetServiceClass);
        if (services.size() == 0) {
            return null;
        }

        if (services.size() > 1) {
            Log.w(TAG, String.format("Multiple services of type %s available - Application can only handle one", gadgetServiceClass));
        }

        return services.get(0);
    }

    /*
     * UI Elements
     */
    private void initUiElements() {
        initCustomFonts();
        initIntervalChooser();
        initLoggingSwitch();
        initBatteryBar();
        initDownloadButton();
        initDisconnectButton();
        initGadgetName();
    }

    private void initCustomFonts() {
        final AssetManager assets = getContext().getAssets();
        final Typeface typefaceNormal = Typeface.createFromAsset(assets, TYPEFACE_CONDENSED_LOCATION);
        final Typeface typefaceBold = Typeface.createFromAsset(assets, TYPEFACE_BOLD_LOCATION);
        mBatteryLevelValue.setTypeface(typefaceNormal);
        mGadgetNameEditText.setTypeface(typefaceNormal);
        mGadgetType.setTypeface(typefaceNormal);
        mDisconnectButton.setTypeface(typefaceBold);
        mDownloadButtonText.setTypeface(typefaceNormal);
        mLoggingIntervalButton.setTypeface(typefaceNormal);
        mBatteryBoardBoardView.setTypeface(typefaceNormal);
        mLoggingBoardBoardView.setTypeface(typefaceNormal);
        mIntervalBoardView.setTypeface(typefaceNormal);
        mDownloadBoardView.setTypeface(typefaceNormal);
    }

    private void initIntervalChooser() {
        mLoggingIntervalButton.setTextColor(mColorLightGray);

        final int loggerIntervalMs = getLoggerInterval(mSelectedGadget);
        if (loggerIntervalMs == UNKNOWN_LOGGING_INTERVAL) {
            return;
        }
        final int intervalSeconds = loggerIntervalMs / Interval.ONE_SECOND.getNumberMilliseconds();
        mLoggingIntervalButton.setText(new TimeFormatter(intervalSeconds).getShortTime(getContext().getApplicationContext()));
        mLoggingIntervalButton.setTextColor(mDeviceButtonColors);
        mLoggingIntervalButton.setEnabled(!(isLoggingStateEditable(mSelectedGadget) && isLoggingStateEnabled(mSelectedGadget)) &&
                !isDownloading(mSelectedGadget));
        mLoggingIntervalButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                showAdviceWhenModifyingInterval();
            }
        });
    }

    private void initDisconnectButton() {
        mDisconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                if (mSelectedGadget != null) {
                    mSelectedGadget.disconnect();
                }
                closeScreen();
            }
        });
    }

    private void initGadgetName() {
        final String deviceAddress = mSelectedGadget.getAddress();
        final String deviceName = DeviceNameDatabaseManager.getInstance().readDeviceName(deviceAddress);
        mGadgetNameEditText.setText(deviceName);
        mGadgetNameEditText.setEnabled(true);
        mSelectedDeviceModel.setDisplayName(deviceName);
        mGadgetType.setText(mSelectedGadget.getName());
        mGadgetNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (mGadgetNameEditText.getText().toString().isEmpty()) {
                        mGadgetNameEditText.setText(deviceAddress);
                    }
                    mGadgetNameEditText.clearFocus(); // triggers a focus change
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mGadgetNameEditText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        mGadgetNameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (mGadgetNameEditText.getText().toString().equals(deviceAddress)) {
                        mGadgetNameEditText.getText().clear();
                    }
                } else {
                    String displayName = mGadgetNameEditText.getText().toString();
                    mSelectedDeviceModel.setDisplayName(displayName);
                    DeviceNameDatabaseManager.getInstance().updateDeviceName(deviceAddress, displayName);
                }
            }
        });
    }

    private void initLoggingSwitch() {
        mLoggingToggle.setOnCheckedChangeListener(null);

        if (isLoggingStateEditable(mSelectedGadget)) {
            mLoggingToggle.setChecked(isLoggingStateEnabled(mSelectedGadget));
            mLoggingToggle.setEnabled(isDownloadingEnabled(mSelectedGadget) && !isDownloading(mSelectedGadget));

            mLoggingToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    if (isChecked) {
                        showLoggingAdviceAlert();
                        mLoggingIntervalButton.setEnabled(false);
                    } else {
                        setLoggingStateEnabled(mSelectedGadget, false);
                        mLoggingIntervalButton.setEnabled(true);
                    }
                }
            });
        } else {
            mLoggingLayout.setVisibility(View.GONE);
        }
    }

    private void initBatteryBar() {
        updateBatteryLevel();
    }

    private void initDownloadButton() {
        mDownloadProgressBar.setProgressDrawable(mDownloadProgressDrawable);
        if (isDownloading(mSelectedGadget)) {
            mDownloadProgressBar.setVisibility(View.VISIBLE);
        } else {
            mDownloadProgressBar.setVisibility(View.GONE);
        }
        mDownloadButtonText.setEnabled(isDownloadingEnabled(mSelectedGadget) && !isDownloading(mSelectedGadget));
        mDownloadButtonText.setText(R.string.label_download);
        mDownloadButtonText.setTextColor(mDeviceButtonColors);
        mDownloadButtonText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Activity parent = getParent();
                if (parent == null) {
                    Log.e(TAG, "initDownloadButton -> Parent is null");
                    return;
                }
                mLoggingToggle.setEnabled(false);
                mLoggingIntervalButton.setEnabled(false);
                mDownloadButtonText.setEnabled(false);
                downloadLog(mSelectedGadget);
            }
        });
    }

    private void updateBatteryLevel() {
        final int batteryLevel = getBatteryLevel(mSelectedGadget);
        if (batteryLevel == UNKNOWN_BATTERY_LEVEL) {
            mBatteryBar.setVisibility(View.GONE);
            mBatteryLevelValue.setText(R.string.label_battery_loading);
        } else {
            mBatteryLevelLayout.setVisibility(View.VISIBLE);
            mBatteryLevelValue.setText(String.format(Locale.GERMAN, "%d%%", batteryLevel));
            if (batteryLevel > 40) {
                mBatteryLevelValue.setTextColor(mColorSensirionGreenDarkened);
            } else if (batteryLevel > 20) {
                mBatteryLevelValue.setTextColor(mColorYellow);
            } else if (batteryLevel > 10) {
                mBatteryLevelValue.setTextColor(mColorOrange);
            } else {
                mBatteryLevelValue.setTextColor(mColorRed);
            }
            mBatteryBar.setVisibility(View.VISIBLE);
            mBatteryBar.setProgress(batteryLevel);
            mBatteryBar.setEnabled(false);
        }
    }

    /*
     * Dialogs
     */
    private void showAdviceWhenModifyingInterval() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(INTERVAL_MODIFICATION_TITLE);
        builder.setCancelable(false);
        builder.setMessage(INTERVAL_MODIFICATION_MESSAGE);
        builder.setPositiveButton(YES_STRING, new DialogInterface.OnClickListener() {
            public void onClick(@NonNull final DialogInterface dialog, final int which) {
                dialog.cancel();
                showIntervalSelector();
            }
        });
        builder.setNegativeButton(NO_STRING, new DialogInterface.OnClickListener() {
            public void onClick(@NonNull final DialogInterface dialog, final int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void showLoggingAdviceAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getParent());

        builder.setTitle(ENABLE_LOGGING_STRING);
        builder.setCancelable(false);
        builder.setMessage(GADGET_ENABLE_ADVICE_STRING);
        builder.setPositiveButton(YES_STRING, new DialogInterface.OnClickListener() {
            public void onClick(@NonNull final DialogInterface dialog, final int which) {
                dialog.cancel();
                setLoggingStateEnabled(mSelectedGadget, true);
            }
        });
        builder.setNegativeButton(NO_STRING, new DialogInterface.OnClickListener() {
            public void onClick(@NonNull final DialogInterface dialog, final int which) {
                dialog.cancel();
                mLoggingToggle.setChecked(false);
            }
        });
        builder.show();
    }

    private void showIntervalSelector() {
        final AlertDialog.Builder builder = new Builder(getContext());
        builder.setCancelable(false)
                .setTitle(R.string.title_button_choice)
                .setItems(R.array.array_interval_choices, new DialogInterface.OnClickListener() {
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        final LoggerInterval interval = LoggerInterval.fromNumberElement(which);
                        if (interval == null) {
                            throw new IllegalStateException("Invalid logger interval selected");
                        }
                        setLoggerInterval(mSelectedGadget, interval.getValueInMilliseconds());
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
     * Tablet Mode
     */

    public class OnTouchOpenTabletMenuListener implements View.OnTouchListener {
        public boolean onTouch(@NonNull final View v, @NonNull final MotionEvent event) {
            if (IS_TABLET) {
                final MainActivity parent = (MainActivity) getParent();
                if (parent == null) {
                    final String mainActivityName = MainActivity.class.getSimpleName();
                    Log.e(TAG, String.format("onCreateView -> Cannot obtain the %s", mainActivityName));
                } else {
                    parent.toggleTabletMenu();
                }
            }
            return true;
        }
    }
}
