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
package com.sensirion.smartgadget.view.comfort_zone;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sensirion.smartgadget.R;
import com.sensirion.smartgadget.peripheral.rht_sensor.RHTSensorFacade;
import com.sensirion.smartgadget.peripheral.rht_sensor.RHTSensorListener;
import com.sensirion.smartgadget.peripheral.rht_sensor.internal.RHTInternalSensorManager;
import com.sensirion.smartgadget.utils.Converter;
import com.sensirion.smartgadget.utils.DeviceModel;
import com.sensirion.smartgadget.utils.Settings;
import com.sensirion.smartgadget.utils.view.ParentFragment;
import com.sensirion.smartgadget.view.comfort_zone.graph.XyPlotView;
import com.sensirion.smartgadget.view.comfort_zone.graph.XyPoint;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.sensirion.smartgadget.utils.XmlFloatExtractor.getFloatValueFromId;

/**
 * A fragment representing the ComfortZone view
 */
public class ComfortZoneFragment extends ParentFragment 
        implements 
            OnTouchListener, 
            RHTSensorListener, 
            SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = ComfortZoneFragment.class.getSimpleName();

    int GRAPH_MIN_Y_VALUE;
    int GRAPH_MAX_Y_VALUE;
    int GRAPH_Y_GRID_SIZE;
    int GRAPH_MIN_X_VALUE;
    int GRAPH_MAX_X_VALUE;
    int GRAPH_X_GRID_SIZE_CELSIUS;
    String GRAPH_X_LABEL_CELSIUS;
    String GRAPH_X_LABEL_FAHRENHEIT;
    XyPlotView mPlotView;
    TextView mTextViewLeft;
    TextView mTextViewTop;
    TextView mTextViewRight;
    TextView mTextViewBottom;
    TextView mSensorNameTextView;
    TextView mSensorAmbientTemperatureTextView;
    TextView mSensorRelativeHumidity;
    String DEFAULT_SENSOR_NAME;
    String EMPTY_TEMPERATURE_STRING;
    String EMPTY_RELATIVE_HUMIDITY_STRING;
    ViewGroup mParentFrame;
    String PERCENTAGE_CHARACTER;
    int GRAPH_X_GRID_SIZE_FAHRENHEIT;

    private final Map<String, XyPoint> mActiveSensorViews = Collections.synchronizedMap(new LinkedHashMap<String, XyPoint>());

    private boolean mIsFahrenheit;

    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        GRAPH_MIN_Y_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_min_y_axis_value);
        GRAPH_MAX_Y_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_max_y_axis_value);
        GRAPH_Y_GRID_SIZE = mContext.getResources().getInteger(R.integer.comfort_zone_y_axis_grid_size);
        GRAPH_MIN_X_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_min_x_axis_value);
        GRAPH_MAX_X_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_max_x_axis_value);
        GRAPH_X_GRID_SIZE_CELSIUS = mContext.getResources().getInteger(R.integer.comfort_zone_x_axis_grid_size_celsius);
        GRAPH_X_GRID_SIZE_FAHRENHEIT = mContext.getResources().getInteger(R.integer.comfort_zone_x_axis_grid_size_fahrenheit);
        GRAPH_X_LABEL_CELSIUS = mContext.getResources().getString(R.string.graph_label_temperature_celsius);
        GRAPH_X_LABEL_FAHRENHEIT = mContext.getResources().getString(R.string.graph_label_temperature_fahrenheit);
        DEFAULT_SENSOR_NAME = mContext.getResources().getString(R.string.text_sensor_name_default);
        EMPTY_TEMPERATURE_STRING = mContext.getResources().getString(R.string.label_empty_t);
        EMPTY_RELATIVE_HUMIDITY_STRING = mContext.getResources().getString(R.string.label_empty_rh);
        PERCENTAGE_CHARACTER = mContext.getResources().getString(R.string.char_percent);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_comfortzone, container, false);

        mPlotView = view.findViewById(R.id.plotview);
        mTextViewLeft = view.findViewById(R.id.textview_left);
        mTextViewTop = view.findViewById(R.id.textview_top);
        mTextViewRight = view.findViewById(R.id.textview_right);
        mTextViewBottom = view.findViewById(R.id.textview_bottom);
        mSensorNameTextView = view.findViewById(R.id.tv_sensor_name);
        mSensorAmbientTemperatureTextView = view.findViewById(R.id.text_amb_temp);
        mSensorRelativeHumidity = view.findViewById(R.id.text_rh);
        mParentFrame = view.findViewById(R.id.parentframe);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActiveSensorViews.clear();
        final Activity parent = getParent();
        if (parent == null) {
            Log.e(TAG, "onViewCreated -> Received null activity");
        } else {
            initXyPlotView();
        }
    }

    private void initXyPlotView() {
        mPlotView.setYAxisScale(GRAPH_MIN_Y_VALUE, GRAPH_MAX_Y_VALUE, GRAPH_Y_GRID_SIZE);
        mPlotView.setXAxisScale(GRAPH_MIN_X_VALUE, GRAPH_MAX_X_VALUE, GRAPH_X_GRID_SIZE_CELSIUS);
        mPlotView.setXAxisLabel(GRAPH_X_LABEL_CELSIUS);

        mTextViewLeft.bringToFront();
        mTextViewTop.bringToFront();
        mTextViewRight.bringToFront();
        mTextViewBottom.bringToFront();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        RHTSensorFacade.getInstance().registerListener(this);

        updateSensorViews();

        updateViewForSelectedSeason();
        updateViewForSelectedTemperatureUnit();
        updateTextViewName();
        touchSelectedSensorView();

        Settings.getInstance().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        RHTSensorFacade.getInstance().unregisterListener(this);
        Settings.getInstance().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateSensorViews() {
        final Activity parent = getParent();
        if (parent == null) {
            Log.e(TAG, "updateViewForSelectedSeason -> obtained null activity when calling parent.");
            return;
        }
        getParent().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (mActiveSensorViews) {
                    for (final String key : mActiveSensorViews.keySet()) {
                        mParentFrame.removeView(mActiveSensorViews.get(key));
                    }
                    mActiveSensorViews.clear();
                }
            }
        });

        final Iterable<DeviceModel> connectedModels = RHTSensorFacade.getInstance().getConnectedSensors();

        for (final DeviceModel model : connectedModels) {
            createNewSensorViewFor(model);
        }
    }

    private void createNewSensorViewFor(@NonNull final DeviceModel model) {
        final Activity parent = getParent();
        if (parent == null) {
            Log.e(TAG, "updateViewForSelectedSeason -> obtained null activity when calling parent.");
            return;
        }
        final String address = model.getAddress();
        try {
            final XyPoint sensorPoint = new XyPoint(mContext.getApplicationContext());
            sensorPoint.setVisibility(View.INVISIBLE);
            sensorPoint.setTag(address);
            sensorPoint.setRadius(
                    getDipFor(getResources().getInteger(R.integer.comfort_zone_radius_sensor_point))
            );
            sensorPoint.setOutlineRadius(
                    getDipFor(
                            getResources().getInteger(R.integer.comfort_zone_radius_sensor_point) +
                                    getFloatValueFromId(getContext(), R.dimen.comfort_zone_outline_radius_offset)
                    )
            );
            sensorPoint.setInnerColor(model.getColor());
            sensorPoint.setOnTouchListener(this);
            mActiveSensorViews.put(address, sensorPoint);
            parent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mParentFrame.addView(sensorPoint);
                }
            });
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "createNewSensorViewFor -> The following exception was thrown: ", e);
        }
    }

    private void updateViewForSelectedSeason() {
        final Activity parent = getParent();
        if (parent == null) {
            Log.e(TAG, "updateViewForSelectedSeason -> obtained null activity when calling parent.");
            return;
        }
        final boolean isSeasonWinter = Settings.getInstance().isSeasonWinter();
        Log.i(TAG,
                String.format(
                        "updateViewForSelectedSeason(): Season %s was selected.",
                        isSeasonWinter ? "Winter" : "Summer"
                )
        );
        getParent().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlotView.updateComfortZone();
                mPlotView.invalidate();
            }
        });
    }

    @UiThread
    private void updateViewForSelectedTemperatureUnit() {
        final Activity parent = getParent();
        if (parent == null) {
            Log.e(TAG, "updateViewForSelectedTemperatureUnit -> obtained null activity when calling parent.");
            return;
        }
        final String mAxisLabel;
        final int gridSize;
        float minXAxisValue = GRAPH_MIN_X_VALUE;
        float maxXAxisValue = GRAPH_MAX_X_VALUE;

        mIsFahrenheit = Settings.getInstance().isTemperatureUnitFahrenheit();

        if (mIsFahrenheit) {
            minXAxisValue = Converter.convertToF(minXAxisValue);
            maxXAxisValue = Converter.convertToF(maxXAxisValue);
            gridSize = GRAPH_X_GRID_SIZE_FAHRENHEIT;
            mAxisLabel = GRAPH_X_LABEL_FAHRENHEIT;
        } else {
            gridSize = GRAPH_X_GRID_SIZE_CELSIUS;
            mAxisLabel = GRAPH_X_LABEL_CELSIUS;
        }

        mPlotView.setXAxisLabel(mAxisLabel);
        mPlotView.setXAxisScale(minXAxisValue, maxXAxisValue, gridSize);

        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlotView.updateComfortZone();
                mPlotView.invalidate();
            }
        });
    }

    private void touchSelectedSensorView() {
        if (isAdded()) {
            final String selectedAddress = Settings.getInstance().getSelectedAddress();
            if (selectedAddress.equals(Settings.SELECTED_NONE)) {
                return;
            }
            final XyPoint point = mActiveSensorViews.get(selectedAddress);
            if (point == null) {
                Log.e(TAG,
                        String.format(
                                "touchSelectedSensorView() -> could not find XyPoint for address: %s",
                                selectedAddress
                        )
                );
            } else {
                selectSensor(selectedAddress);
                final Activity parent = getParent();
                if (parent == null) {
                    Log.e(TAG, "touchSelectedSensorView -> obtained null activity when calling parent.");
                    return;
                }
                getParent().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        point.animateTouch();
                    }
                });
            }
        }
    }

    @Override
    public boolean onTouch(@NonNull final View view, @NonNull final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (view instanceof XyPoint) {
                final Object tag = view.getTag();
                if (tag != null) {
                    selectSensor(tag.toString());
                }
                final Activity parent = getParent();
                if (parent == null) {
                    Log.e(TAG, "onTouch -> obtained null activity when calling parent.");
                    return false;
                }
                parent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((XyPoint) view).animateTouch();
                    }
                });
            }
        }
        return view.performClick();
    }

    private void selectSensor(@NonNull final String selectedAddress) {
        synchronized (mActiveSensorViews) {
            for (final Map.Entry<String, XyPoint> activeSensorView : mActiveSensorViews.entrySet()) {
                final XyPoint point = activeSensorView.getValue();
                if (selectedAddress.equals(activeSensorView.getKey())) {
                    Settings.getInstance().setSelectedAddress(selectedAddress);
                    point.setOutlineColor(Color.WHITE);
                    updateTextViewName();
                } else {
                    point.setOutlineColor(Color.TRANSPARENT);
                }
                point.postInvalidate();
            }
        }
    }

    private void updateTextViewName() {
        try {
            final String selectedAddress = Settings.getInstance().getSelectedAddress();
            final DeviceModel model = RHTSensorFacade.getInstance().getDeviceModel(selectedAddress);
            if (model == null) {
                mActiveSensorViews.remove(selectedAddress);
                return;
            }
            final Activity parent = getParent();
            if (parent == null) {
                Log.e(TAG, "updateTextViewName -> obtained null activity when calling parent.");
                return;
            }
            parent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    XyPoint selectedPoint = mActiveSensorViews.get(selectedAddress);
                    if (selectedPoint != null) {
                        mSensorNameTextView.setTextColor(selectedPoint.getInnerColor());
                    } else {
                        Log.e(TAG,
                                String.format(
                                        "updateTextViewName() -> mActiveSensorViews does not contain selected address: %s",
                                        selectedAddress
                                )
                        );
                        mSensorNameTextView.setTextColor(model.getColor());
                    }
                    mSensorNameTextView.setText(model.getUserDeviceName());
                }
            });
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "updateTextViewName(): The following exception was produced -> ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGadgetConnectionChanged(@NonNull final String deviceAddress,
                                          final boolean deviceIsConnected) {
        if (isAdded()) {
            if (deviceIsConnected) {
                Log.d(TAG,
                        String.format(
                                "onGadgetConnectionChanged() -> Device %s was connected.",
                                deviceAddress
                        )
                );
            } else {
                Log.d(TAG,
                        String.format(
                                "onGadgetConnectionChanged() -> Device %s was disconnected. ",
                                deviceAddress
                        )
                );
                if (getView() == null) {
                    throw new NullPointerException(
                            String.format(
                                    "%s: onGadgetConnectionChanged -> It was impossible to obtain the view.",
                                    TAG
                            )
                    );
                }
                removeSensorView(deviceAddress);
                if (RHTSensorFacade.getInstance().hasConnectedDevices()) {
                    touchSelectedSensorView();
                } else {
                    final Activity parent = getParent();
                    if (parent == null) {
                        Log.e(TAG, "onGadgetConnectionChanged -> Received null activity.");
                        return;
                    }
                    parent.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSensorNameTextView.setText(DEFAULT_SENSOR_NAME);
                            mSensorAmbientTemperatureTextView.setText(EMPTY_TEMPERATURE_STRING);
                            mSensorRelativeHumidity.setText(EMPTY_RELATIVE_HUMIDITY_STRING);
                        }
                    });
                }
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
            updateViewValues(
                    RHTInternalSensorManager.INTERNAL_SENSOR_ADDRESS,
                    temperature,
                    relativeHumidity
            );
        } else {
            updateViewValues(deviceAddress, temperature, relativeHumidity);
        }
    }

    private void removeSensorView(final String deviceAddress) {
        synchronized (mActiveSensorViews) {
            if (mActiveSensorViews.containsKey(deviceAddress)) {
                final Activity parent = getParent();
                if (parent == null) {
                    Log.e(TAG, "removeSensorView() -> Obtained null when calling the activity.");
                    return;
                }
                final XyPoint stalePoint = mActiveSensorViews.get(deviceAddress);
                parent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mParentFrame.removeView(stalePoint);
                    }
                });
                mActiveSensorViews.remove(deviceAddress);
            }
        }
    }

    public void updateViewValues(@NonNull final String address,
                                 final float temperature,
                                 final float relativeHumidity) {
        if (isAdded()) {
            final Activity parent = getParent();
            if (parent == null) {
                Log.e(TAG, "updateViewValues() -> Obtained null when calling the activity.");
                return;
            }

            if (!address.equals(RHTInternalSensorManager.INTERNAL_SENSOR_ADDRESS) &&
                    !mActiveSensorViews.containsKey(address)) {
                Log.w(TAG, String.format(
                        "updateViewValues() -> Received value from inactive device %s. Updating views.",
                        address
                        )
                );
                updateSensorViews();
            }
            parent.runOnUiThread(new Runnable() {
                float newTemperature = temperature;
                String unit;

                @Override
                public void run() {
                    Log.v(TAG,
                            String.format(
                                    "updateViewValues(): address = %s | temperature = %f | relativeHumidity = %f",
                                    address,
                                    temperature,
                                    relativeHumidity
                            )
                    );
                    if (mIsFahrenheit) {
                        newTemperature = Converter.convertToF(temperature);
                        unit = getString(R.string.unit_fahrenheit);
                    } else {
                        newTemperature = temperature;
                        unit = getString(R.string.unit_celsius);
                    }
                    updateTextViewRHT(address, newTemperature, relativeHumidity, unit);

                    final PointF newPos = new PointF(newTemperature, relativeHumidity);
                    boolean isClipped = false;
                    if (mPlotView.isOutsideGrid(newPos)) {
                        isClipped = true;
                    }
                    final XyPoint selectedPoint = mActiveSensorViews.get(address);
                    if (selectedPoint != null) {
                        updateViewPositionFor(selectedPoint, newPos, isClipped);
                    }
                }
            });
        }
    }

    private void updateTextViewRHT(@NonNull final String address,
                                   final float temperature,
                                   final float humidity,
                                   final String unit) {
        if (address.equals(Settings.getInstance().getSelectedAddress())) {
            if (getView() == null) {
                throw new NullPointerException(
                        String.format("%s: updateTextViewRHT -> It was impossible to obtain the view.",
                                TAG
                        )
                );
            }
            final NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
            nf.setMaximumFractionDigits(1);
            nf.setMinimumFractionDigits(1);
            mSensorAmbientTemperatureTextView.setText(String.format("%s %s", nf.format(temperature), unit));
            mSensorRelativeHumidity.setText(String.format("%s %s", nf.format(humidity), getString(R.string.unit_humidity)));
        }
    }

    private void updateViewPositionFor(@NonNull final XyPoint selectedPoint,
                                       @NonNull final PointF p,
                                       final boolean isClipped) {
        final PointF canvasPosition;
        if (isClipped) {
            final PointF clippedPoint = mPlotView.getClippedPoint();
            if (clippedPoint == null) {
                Log.e(TAG, "updateViewPositionFor -> Cannot obtain the clipped point");
                return;
            } else {
                canvasPosition = mPlotView.coordinates(mPlotView.getClippedPoint());
            }
        } else {
            canvasPosition = mPlotView.coordinates(p);
        }
        animateSensorViewPointTo(selectedPoint, canvasPosition.x, canvasPosition.y);
    }

    private void animateSensorViewPointTo(@NonNull final XyPoint selectedPoint, final float x, final float y) {
        final Activity parent = getParent();
        if (parent == null) {
            Log.e(TAG, "animateSensorViewPointTo() -> Obtained null when calling the activity.");
            return;
        }
        final float relativeX =
                x - (getDipFor(getResources().getInteger(R.integer.comfort_zone_radius_sensor_point) +
                        getFloatValueFromId(parent, R.dimen.comfort_zone_outline_radius_offset)));
        final float relativeY =
                y - (getDipFor(getResources().getInteger(R.integer.comfort_zone_radius_sensor_point) +
                        getFloatValueFromId(parent, R.dimen.comfort_zone_outline_radius_offset)));
        parent.runOnUiThread(new Runnable() {
            @Override
            @UiThread
            public void run() {
                selectedPoint.animateMove(relativeX, relativeY);
            }
        });
    }

    private float getDipFor(final float px) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
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
        } else if (key.equals(Settings.KEY_SELECTED_SEASON)) {
            updateViewForSelectedSeason();
        }
    }
}
