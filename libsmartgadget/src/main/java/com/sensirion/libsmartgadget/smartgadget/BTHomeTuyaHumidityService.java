package com.sensirion.libsmartgadget.smartgadget;

import androidx.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.GadgetValue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class BTHomeTuyaHumidityService  extends SmartGadgetNotificationService  {
    private static final String TAG = BTHomeTuyaHumidityService.class.getSimpleName();

    public static final String UNIT = "%";

    public static final String SERVICE_UUID = "0000181a-0000-1000-8000-00805f9b34fb";

    private static final String NOTIFICATIONS_UUID = "00002a6f-0000-1000-8000-00805f9b34fb";

    /**
     * {@inheritDoc}
     */
    public BTHomeTuyaHumidityService(@NonNull final ServiceListener serviceListener,
                                        @NonNull final BleConnector bleConnector,
                                        @NonNull final String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, SERVICE_UUID, NOTIFICATIONS_UUID, UNIT);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            final float humidity = (float)((rawData[1] << 8) | rawData[1]) / 100f;
            mLastValues = new GadgetValue[]{
                    new SmartGadgetValue(new Date(), humidity, UNIT)};
            mServiceListener.onGadgetValuesReceived(this, mLastValues);
            Log.i(TAG, "Humidity: " + humidity + UNIT);
        }
    }
}
