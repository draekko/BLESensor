package com.sensirion.libsmartgadget.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.GadgetNotificationService;
import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.utils.LittleEndianExtractor;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SmartGadgetMultiNotificationService implements GadgetNotificationService, BleConnectorCallback {
    private static final String TAG = SmartGadgetMultiNotificationService.class.getSimpleName();
    protected static final String NOTIFICATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    protected final BleConnector mBleConnector;
    protected final ServiceListener mServiceListener;
    protected final String mDeviceAddress;

    protected final String mNotificationsUuid1;
    protected final String mUnit1;
    protected final String mNotificationsUuid2;
    protected final String mUnit2;

    protected final Set<String> mSupportedUuids;
    protected GadgetValue[] mLastValues;
    protected boolean mSubscribed;

    /**
     * {@inheritDoc}
     */
    public SmartGadgetMultiNotificationService(@NonNull final ServiceListener serviceListener,
                                          @NonNull final BleConnector bleConnector,
                                          @NonNull final String deviceAddress,
                                          @NonNull final String serviceUuid,
                                          @NonNull final String notificationsUuid1,
                                          @NonNull final String notificationsUuid2,
                                          @NonNull final String unit1,
                                          @NonNull final String unit2) {
        mServiceListener = serviceListener;
        mBleConnector = bleConnector;
        mDeviceAddress = deviceAddress;
        mNotificationsUuid1 = notificationsUuid1;
        mNotificationsUuid2 = notificationsUuid2;
        mUnit1 = unit1;
        mUnit2 = unit2;
        mLastValues = new GadgetValue[0];
        mSubscribed = false;

        mSupportedUuids = new HashSet<>();
        mSupportedUuids.add(serviceUuid);
        mSupportedUuids.add(notificationsUuid1);
        mSupportedUuids.add(notificationsUuid2);
        mSupportedUuids.add(NOTIFICATION_DESCRIPTOR_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe() {
        final Map<String, BluetoothGattCharacteristic> characteristics1 =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuid1));

        subscribeNotifications(mDeviceAddress, characteristics1.get(mNotificationsUuid1), true);

        final Map<String, BluetoothGattCharacteristic> characteristics2 =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuid2));

        subscribeNotifications(mDeviceAddress, characteristics2.get(mNotificationsUuid2), true);
        mSubscribed = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe() {
        mBleConnector.readCharacteristic(mDeviceAddress, mNotificationsUuid1);
        final Map<String, BluetoothGattCharacteristic> characteristics1 =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuid1));
        subscribeNotifications(mDeviceAddress, characteristics1.get(mNotificationsUuid1), false);

        mBleConnector.readCharacteristic(mDeviceAddress, mNotificationsUuid2);
        final Map<String, BluetoothGattCharacteristic> characteristics2 =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuid2));
        subscribeNotifications(mDeviceAddress, characteristics2.get(mNotificationsUuid2), false);

        mSubscribed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribed() {
        return mSubscribed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestValueUpdate() {
        mBleConnector.readCharacteristic(mDeviceAddress, mNotificationsUuid1);
        mBleConnector.readCharacteristic(mDeviceAddress, mNotificationsUuid1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GadgetValue[] getLastValues() {
        return mLastValues;
    }

    /*
     * Implementation of {@link BleConnectorCallback}
     */
    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            if (rawData.length <= 8) {
                handleLiveValue(characteristicUuid, rawData);
            }
        }
    }

    @Override
    public void onDataWritten(final String characteristicUuid) {
        // ignore ... nothing written here
    }

    @Override
    public void onConnectionStateChanged(final boolean connected) {
        // ignore ... nothing to do here for the service
    }

    @Override
    public void onFail(final String characteristicUuid, final byte[] data, final boolean isWriteFailure) {
        if (!isUuidSupported(characteristicUuid)) {
            return;
        }

        if (characteristicUuid.contains(mNotificationsUuid1)) {
            if (isSubscribed()) {
                subscribe(); // failed to subscribe... retry
            } else {
                unsubscribe(); // failed to unsubscribe... retry
            }
        }

        if (characteristicUuid.contains(mNotificationsUuid2)) {
            if (isSubscribed()) {
                subscribe(); // failed to subscribe... retry
            } else {
                unsubscribe(); // failed to unsubscribe... retry
            }
        }
    }

    /*
     *  Private Helper Methods
     */

    private synchronized void subscribeNotifications(@NonNull final String deviceAddress,
                                                     @NonNull final BluetoothGattCharacteristic characteristic,
                                                     final boolean enable) {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(NOTIFICATION_DESCRIPTOR_UUID));
        if (descriptor == null) {
            Log.w(TAG, "Null Descriptor when subscribing gadget " + deviceAddress);
            return;
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBleConnector.setCharacteristicNotification(deviceAddress, characteristic, descriptor, enable);
    }

    protected void handleLiveValue(final String characteristicUuid, final byte[] rawData) {
        String unit = mUnit1;
        if (characteristicUuid.contains(mNotificationsUuid2)) {
            unit = mUnit2;
        }
        final float value = LittleEndianExtractor.extractFloat(rawData, 0);
        mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), value, unit)};
        mServiceListener.onGadgetValuesReceived(this, mLastValues);
    }

    protected boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }
}
