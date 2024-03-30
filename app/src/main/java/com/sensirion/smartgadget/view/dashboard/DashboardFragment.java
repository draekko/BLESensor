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
package com.sensirion.smartgadget.view.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sensirion.smartgadget.R;
import com.sensirion.smartgadget.peripheral.rht_sensor.RHTSensorFacade;
import com.sensirion.smartgadget.peripheral.rht_sensor.RHTSensorListener;
import com.sensirion.smartgadget.peripheral.rht_sensor.internal.RHTInternalSensorManager;
import com.sensirion.smartgadget.utils.Converter;
import com.sensirion.smartgadget.utils.DeviceModel;
import com.sensirion.smartgadget.utils.Settings;
import com.sensirion.smartgadget.utils.section_manager.SectionManagerMobile;
import com.sensirion.smartgadget.utils.view.ParentFragment;
import com.sensirion.smartgadget.view.MainActivity;
import com.sensirion.smartgadget.view.dashboard.adapter.ConnectedDeviceAdapter;
import com.sensirion.smartgadget.view.preference.SmartgadgetPreferenceFragment;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends ParentFragment
        implements
            RHTSensorListener,
            SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = DashboardFragment.class.getSimpleName();

    //BUTTON STATES
    private static final int BUTTON_STATE_TEMPERATURE = 0;
    private static final int DEFAULT_STATE_BUTTON = BUTTON_STATE_TEMPERATURE;
    private static final int BUTTON_STATE_HUMIDITY = 1;
    private static final int BUTTON_STATE_DEW_POINT = 2;
    private static final int BUTTON_STATE_HEAT_INDEX = 3;

    //BUTTON VALUE FORMAT
    private static final String BUTTON_VALUE_FORMAT = "%s%s %s";

    //VIEWS
    ScrollView mTotalView;
    ListView mConnectedDeviceView;
    Button mTemperatureButton;
    Button mHumidityButton;
    Button mDewPointButton;
    Button mHeatIndexButton;
    TextView mTemperatureValueTextView;
    TextView mHumidityValueTextView;
    TextView mDewPointValueTextView;
    TextView mHeatIndexValueTextView;
    Button mFindGadgetButton;

    //Extracted attributes from the XML
    boolean IS_TABLET;
    String FAHRENHEIT_UNIT;
    String CELSIUS_UNIT;
    String HUMIDITY_UNIT;
    String EMPTY_TEMPERATURE_LABEL;
    String EMPTY_HUMIDITY_LABEL;
    String EMPTY_HEAT_INDEX_LABEL;
    String TYPEFACE_CONDENSED_LOCATION;
    String TYPEFACE_BOLD_LOCATION;

    //Temperature state
    private boolean mIsFahrenheit;

    //Connection Adapter
    private ConnectedDeviceAdapter mConnectedDeviceAdapter;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);

        IS_TABLET = mContext.getResources().getBoolean(R.bool.is_tablet);
        FAHRENHEIT_UNIT = mContext.getResources().getString(R.string.unit_fahrenheit);
        CELSIUS_UNIT = mContext.getResources().getString(R.string.unit_celsius);
        HUMIDITY_UNIT = mContext.getResources().getString(R.string.unit_humidity);
        EMPTY_TEMPERATURE_LABEL = mContext.getResources().getString(R.string.label_empty_t);
        EMPTY_HUMIDITY_LABEL = mContext.getResources().getString(R.string.label_empty_rh);
        EMPTY_HEAT_INDEX_LABEL = mContext.getResources().getString(R.string.label_empty_heat_index);
        TYPEFACE_CONDENSED_LOCATION = mContext.getResources().getString(R.string.typeface_condensed);
        TYPEFACE_BOLD_LOCATION = mContext.getResources().getString(R.string.typeface_bold);
    }

    private void updateListView() {
        if (isAdded()) {
            Log.i(TAG, "updateListView()");
            final List<DeviceModel> connectedDevices = RHTSensorFacade.getInstance().getConnectedSensors();
            final ConnectedDeviceAdapter adapter = ((ConnectedDeviceAdapter) mConnectedDeviceView.getAdapter());
            final Activity parent = getParent();
            if (parent == null) {
                Log.e(TAG, "updateListView() -> Received a null activity.");
            } else {
                parent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.update(connectedDevices);
                    }
                });
            }
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        Log.i(TAG, "OnCreateView()");
        final View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mTotalView = view.findViewById(R.id.dashboard_content_layout);
        mConnectedDeviceView = view.findViewById(R.id.dashboard_connected_device_nested_list_view);
        mTemperatureButton = view.findViewById(R.id.dashboard_temperature_button);
        mHumidityButton = view.findViewById(R.id.dashboard_humidity_button);
        mDewPointButton = view.findViewById(R.id.dashboard_dew_point_button);
        mHeatIndexButton = view.findViewById(R.id.dashboard_heat_index_button);
        mTemperatureValueTextView = view.findViewById(R.id.dashboard_temperature_value);
        mHumidityValueTextView = view.findViewById(R.id.dashboard_humidity_value);
        mDewPointValueTextView = view.findViewById(R.id.dashboard_dew_point_value);
        mHeatIndexValueTextView = view.findViewById(R.id.dashboard_heat_index_value);
        mFindGadgetButton = view.findViewById(R.id.button_find_gadget);

        mTotalView.requestFocus();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated()");
        final AssetManager assets = getContext().getAssets();
        final Typeface typefaceNormal = Typeface.createFromAsset(assets, TYPEFACE_CONDENSED_LOCATION);
        final Typeface typefaceBold = Typeface.createFromAsset(assets, TYPEFACE_BOLD_LOCATION);
        initButtons(typefaceNormal, typefaceBold);
        initListView();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetViewValues();
        updateViewForSelectedTemperatureUnit();
        updateListView();
        RHTSensorFacade.getInstance().registerListener(this);
        Settings.getInstance().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        RHTSensorFacade.getInstance().unregisterListener(this);
        Settings.getInstance().unregisterOnSharedPreferenceChangeListener(this);
    }

    @UiThread
    @SuppressLint("CommitPrefEdits")
    //Using apply instead of commit can cause a Parallel writing Exception.
    private void initButtons(@NonNull final Typeface typefaceNormal,
                             @NonNull final Typeface typefaceBold) {

        mTemperatureValueTextView.setTypeface(typefaceBold);
        mHumidityValueTextView.setTypeface(typefaceBold);
        mDewPointValueTextView.setTypeface(typefaceBold);
        mHeatIndexValueTextView.setTypeface(typefaceBold);

        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());

        mTemperatureButton.setTypeface(typefaceNormal);
        prefs.edit().putInt(String.valueOf(mTemperatureButton.getId()), BUTTON_STATE_TEMPERATURE).commit();
        addButtonListener(mTemperatureButton);

        mHumidityButton.setTypeface(typefaceNormal);
        prefs.edit().putInt(String.valueOf(mHumidityButton.getId()), BUTTON_STATE_HUMIDITY).commit();
        addButtonListener(mHumidityButton);

        mDewPointButton.setTypeface(typefaceNormal);
        prefs.edit().putInt(String.valueOf(mDewPointButton.getId()), BUTTON_STATE_DEW_POINT).commit();
        addButtonListener(mDewPointButton);

        mHeatIndexButton.setTypeface(typefaceNormal);
        prefs.edit().putInt(String.valueOf(mHeatIndexButton.getId()), BUTTON_STATE_HEAT_INDEX).commit();
        addButtonListener(mHeatIndexButton);

        mFindGadgetButton.setTypeface(typefaceBold);
        if (IS_TABLET) {
            mFindGadgetButton.setVisibility(View.GONE);
        }
        mFindGadgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Smart Gadgets Scanning View
                final MainActivity activity = (MainActivity) getParent();
                if (activity != null) {
                    activity.onMobileTabSelected(SectionManagerMobile.POSITION_SETTINGS,
                            SmartgadgetPreferenceFragment.REMOTE_INSTRUCTION_OPEN_SCAN_FRAGMENT);
                }

            }
        });
    }

    private void initListView() {
        mConnectedDeviceAdapter = new ConnectedDeviceAdapter(getContext().getApplicationContext());
        mConnectedDeviceView.setAdapter(mConnectedDeviceAdapter);
        mConnectedDeviceView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull final AdapterView<?> adapterView,
                                    @NonNull final View arg1,
                                    final int position,
                                    final long arg3) {
                final String clickedAddress = mConnectedDeviceAdapter.getItem(position).getAddress();
                final String selectedAddress = Settings.getInstance().getSelectedAddress();
                if (selectedAddress.equals(clickedAddress)) {
                    return;
                }
                Settings.getInstance().setSelectedAddress(clickedAddress);
                mConnectedDeviceAdapter.notifyDataSetChanged();
                resetViewValues();
                RHTSensorFacade.getInstance().notifyCachedSensorData(DashboardFragment.this);
            }
        });
        updateListView();
    }

    private void addButtonListener(@NonNull final Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (IS_TABLET) {
                    final MainActivity activity = (MainActivity) getParent();
                    if (activity == null) {
                        Log.e(TAG, "addButtonListener.onClick -> Cannot toogle menu with a null activity.");
                    } else {
                        activity.toggleTabletMenu();
                    }
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGadgetConnectionChanged(@Nullable final String deviceAddress,
                                          final boolean deviceIsConnected) {
        if (isAdded()) {
            updateListView();
            if (deviceIsConnected) {
                Log.i(TAG, String.format(
                        "onGadgetConnectionChanged() -> Sensor with address %s was connected.",
                        deviceAddress
                        )
                );
            } else {
                resetViewValues();
                RHTSensorFacade.getInstance().notifyCachedSensorData(DashboardFragment.this);
                Log.i(TAG, String.format(
                        "onGadgetConnectionChanged() -> Sensor with address %s was disconnected.",
                        deviceAddress
                        )
                );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewRHTSensorData(final float temperature,
                                   final float relativeHumidity,
                                   @Nullable final String deviceAddress) {
        if (deviceAddress == null) {
            updateViewValues(temperature, relativeHumidity, RHTInternalSensorManager.INTERNAL_SENSOR_ADDRESS);
        } else {
            updateViewValues(temperature, relativeHumidity, deviceAddress);
        }
    }

    private void resetViewValues() {
        final Activity parent = getParent();
        if (parent == null) {
            Log.e(TAG, "updateViewValues -> Received null parent.");
            return;
        }
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTemperatureValueTextView.setText(EMPTY_TEMPERATURE_LABEL);
                mHumidityValueTextView.setText(EMPTY_HUMIDITY_LABEL);
                mDewPointValueTextView.setText(EMPTY_TEMPERATURE_LABEL);
                mHeatIndexValueTextView.setText(EMPTY_TEMPERATURE_LABEL);
            }
        });
    }

    private void updateViewValues(final float temperature,
                                  final float humidity,
                                  @NonNull final String deviceAddress) {
        if (isAdded()) {
            final String selectedAddress = Settings.getInstance().getSelectedAddress();
            if (!deviceAddress.equals(selectedAddress)) {
                return;
            }
            final Activity parent = getParent();
            if (parent == null) {
                Log.e(TAG, "updateViewValues -> Received null parent.");
                return;
            }
            parent.runOnUiThread(new Runnable() {
                private final SharedPreferences mPreferences =
                        PreferenceManager.getDefaultSharedPreferences(getParent().getApplicationContext());

                @Override
                public void run() {
                    final String selectedAddress = Settings.getInstance().getSelectedAddress();
                    if (deviceAddress.equals(selectedAddress)) {
                        changeButtonValue(mTemperatureButton, mTemperatureValueTextView);
                        changeButtonValue(mHumidityButton, mHumidityValueTextView);
                        changeButtonValue(mDewPointButton, mDewPointValueTextView);
                        changeButtonValue(mHeatIndexButton, mHeatIndexValueTextView);
                    }
                }

                private void changeButtonValue(@NonNull final Button button,
                                               @NonNull final TextView textView) {
                    final String unit;
                    if (mIsFahrenheit) {
                        unit = FAHRENHEIT_UNIT;
                    } else {
                        unit = CELSIUS_UNIT;
                    }
                    final String buttonId = String.valueOf(button.getId());
                    final int buttonState = mPreferences.getInt(buttonId, DEFAULT_STATE_BUTTON);

                    final NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                    nf.setMaximumFractionDigits(1);
                    nf.setMinimumFractionDigits(1);

                    final String signGap;

                    switch (buttonState) {
                        case BUTTON_STATE_HUMIDITY:
                            signGap = " ";
                            textView.setText(String.format(BUTTON_VALUE_FORMAT, signGap, nf.format(humidity), HUMIDITY_UNIT));
                            break;
                        case BUTTON_STATE_TEMPERATURE:
                            signGap = temperature < 0 ? "" : " ";
                            final String fixedTemperature = nf.format(mIsFahrenheit ? Converter.convertToF(temperature) : temperature);
                            textView.setText(String.format(BUTTON_VALUE_FORMAT, signGap, fixedTemperature, unit));
                            break;
                        case BUTTON_STATE_DEW_POINT:
                            final float dewPoint = Converter.calcDewPoint(humidity, temperature);
                            final float fixedDewPoint = mIsFahrenheit ? Converter.convertToF(dewPoint) : dewPoint;
                            signGap = fixedDewPoint < 0 ? "" : " ";
                            textView.setText(String.format(BUTTON_VALUE_FORMAT, signGap, nf.format(fixedDewPoint), unit));
                            break;
                        case BUTTON_STATE_HEAT_INDEX:
                            final float heatIndex;
                            if (mIsFahrenheit) {
                                heatIndex = Converter.calculateHeatIndexFahrenheit(humidity, Converter.convertToF(temperature));
                            } else {
                                heatIndex = Converter.calculateHeatIndexCelsius(humidity, temperature);
                            }
                            if (Float.isNaN(heatIndex)) {
                                textView.setText(String.format(" %s", EMPTY_HEAT_INDEX_LABEL));
                            } else {
                                signGap = heatIndex < 0 ? "" : " ";
                                textView.setText(String.format(BUTTON_VALUE_FORMAT, signGap, nf.format(heatIndex), unit));
                            }
                            break;
                        default:
                            Log.e(TAG, String.format("changeButtonValue() -> Unknown button state: %s", buttonState));
                            break;
                    }
                }
            });
        }
    }

    /**
     * Updates the Fragment to show temperature in Fahrenheit or Celsius
     * if the temperature Unit was changed by the user.
     */
    @Override
    public void onSharedPreferenceChanged(@NonNull final SharedPreferences sharedPreferences,
                                          @NonNull final String key) {
        if (key.equals(Settings.KEY_SELECTED_TEMPERATURE_UNIT)) {
            updateViewForSelectedTemperatureUnit();
        }
    }

    private void updateViewForSelectedTemperatureUnit() {
        mIsFahrenheit = Settings.getInstance().isTemperatureUnitFahrenheit();
        updateListView();
    }
}
