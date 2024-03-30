package com.sensirion.libsmartgadget.smartgadget;

import androidx.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.BuildConfig;
import com.sensirion.libsmartgadget.GadgetValue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class BTHomeTuyaTemperatureHumidtyService extends SmartGadgetMultiNotificationService  {

    private static final String TAG = BTHomeTuyaHumidityService.class.getSimpleName();
    public static final String UNIT_T = "°C";
    public static final String UNIT_RH = "%";

    public static final String SERVICE_UUID = "0000181a-0000-1000-8000-00805f9b34fb";

    private static final String T_NOTIFICATIONS_UUID = "00002a6e-0000-1000-8000-00805f9b34fb";
    private static final String RH_NOTIFICATIONS_UUID = "00002a6f-0000-1000-8000-00805f9b34fb";

    /**
     * {@inheritDoc}
     */
    public BTHomeTuyaTemperatureHumidtyService(@NonNull final ServiceListener serviceListener,
                                               @NonNull final BleConnector bleConnector,
                                               @NonNull final String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress,
                SERVICE_UUID,
                T_NOTIFICATIONS_UUID,
                RH_NOTIFICATIONS_UUID,
                UNIT_T,
                UNIT_RH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionStateChanged(final boolean connected) {
        if (connected) {
            requestValueUpdate();
        }
    }

    private float humidity = 9999.0f;
    private float temperature = 9999.0f;
    /**
     * {@inheritDoc}
     */
    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            String unit = "!!";
            float unit_value = (float)(((rawData[1] & 0xff) * 256) + (rawData[0] & 0xff)) / 100f;
            if (characteristicUuid.contains(RH_NOTIFICATIONS_UUID)) {
                unit = UNIT_RH;
                humidity = unit_value;
            }
            if (characteristicUuid.contains(T_NOTIFICATIONS_UUID)) {
                unit = UNIT_T;
                temperature = unit_value;
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "TUYA SENSOR: " + unit_value + unit);
            }
            if (temperature != 9999.0f && humidity != 9999.0) {
                final Date timestamp = new Date();
                mLastValues = new GadgetValue[]{
                        new SmartGadgetValue(timestamp, temperature, UNIT_T),
                        new SmartGadgetValue(timestamp, humidity, UNIT_RH)
                };
                mServiceListener.onGadgetValuesReceived(this, mLastValues);
            }
        }
    }
}
