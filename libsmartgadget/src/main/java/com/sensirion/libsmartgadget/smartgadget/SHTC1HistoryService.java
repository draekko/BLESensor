package com.sensirion.libsmartgadget.smartgadget;

import android.os.Handler;
import androidx.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.utils.LittleEndianExtractor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class SHTC1HistoryService extends SmartGadgetHistoryService {
    private static final String TAG = SHTC1HistoryService.class.getSimpleName();

    public static final String SERVICE_UUID = "0000fa20-0000-1000-8000-00805f9b34fb";

    private static final String LOGGING_STATE_CHARACTERISTIC_UUID = "0000fa21-0000-1000-8000-00805f9b34fb";
    private static final String LOGGING_INTERVAL_S_CHARACTERISTIC_UUID = "0000fa22-0000-1000-8000-00805f9b34fb";
    private static final String CURRENT_POINTER_CHARACTERISTIC_UUID = "0000fa23-0000-1000-8000-00805f9b34fb";
    private static final String START_POINTER_CHARACTERISTIC_UUID = "0000fa24-0000-1000-8000-00805f9b34fb";
    private static final String END_POINTER_CHARACTERISTIC_UUID = "0000fa25-0000-1000-8000-00805f9b34fb";
    private static final String LOGGED_DATA_CHARACTERISTIC_UUID = "0000fa26-0000-1000-8000-00805f9b34fb";
    private static final String USER_DATA_CHARACTERISTIC_UUID = "0000fa27-0000-1000-8000-00805f9b34fb";

    private static final int GADGET_RING_BUFFER_SIZE = 16384;
    private static final int DATA_POINT_SIZE = 4;
    private static final long SHTC1_SPECIFIC_READ_AFTER_WRITE_DELAY_MS = 1000;

    private DownloadState mDownloadState;
    private Date mDownloadStartTimestamp;
    private boolean mLoggerStateBeforeDownload;

    private int mCurrentPointer;
    private int mStartPointer;
    private int mEndPointer;
    private int mLoggingEnabledTimestamp;

    /**
     * {@inheritDoc}
     */
    public SHTC1HistoryService(@NonNull final ServiceListener serviceListener,
                               @NonNull final BleConnector bleConnector,
                               @NonNull final String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, new String[]{
                SERVICE_UUID,
                LOGGING_STATE_CHARACTERISTIC_UUID,
                LOGGING_INTERVAL_S_CHARACTERISTIC_UUID,
                CURRENT_POINTER_CHARACTERISTIC_UUID,
                START_POINTER_CHARACTERISTIC_UUID,
                END_POINTER_CHARACTERISTIC_UUID,
                LOGGED_DATA_CHARACTERISTIC_UUID,
                USER_DATA_CHARACTERISTIC_UUID
        });
        mDownloadState = DownloadState.IDLE;
    }

    /*
        Implementation of {@link GadgetDownloadService}
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDownloading() {
        return !mDownloadState.equals(DownloadState.IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGadgetLoggingStateEditable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGadgetLoggingEnabled(final boolean enabled) {
        if (enabled) {
            writeLoggingStartTimestamp();
        }
        writeValueToCharacteristic(LOGGING_STATE_CHARACTERISTIC_UUID, (byte) ((enabled) ? 1 : 0), FORMAT_UINT8, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLoggerInterval(final int loggerIntervalMs) {
        final int loggerIntervalS = loggerIntervalMs / 1000;
        return writeValueToCharacteristic(LOGGING_INTERVAL_S_CHARACTERISTIC_UUID, loggerIntervalS, FORMAT_UINT16, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLoggerInterval() {
        return mLoggerIntervalMs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestValueUpdate() {
        readLoggerInterval();
        readLoggingState();
        readCurrentPointer();
        readStartPointer();
        readEndPointer();
        readUserData();
    }

    /*
        Implementation of {@link BleConnectorCallback}
     */

    @Override
    public void onFail(final String characteristicUuid, final byte[] data,
                       final boolean isWriteFailure) {
        if (!isUuidSupported(characteristicUuid)) {
            return;
        }

        if (isDownloading()) {
            mDownloadState = DownloadState.IDLE;
            mDownloadProgress = -1;
            mServiceListener.onDownloadFailed(this);
            return;
        }

        if (isWriteFailure) {
            switch (characteristicUuid) {
                case LOGGING_INTERVAL_S_CHARACTERISTIC_UUID:
                    mServiceListener.onSetLoggerIntervalFailed(this);
                    break;
                case LOGGING_STATE_CHARACTERISTIC_UUID:
                    mServiceListener.onSetGadgetLoggingEnabledFailed(this);
                    break;
            }
        }
        // ignore read failures if not currently downloading
    }

    /*
        Private Methods
     */

    @Override
    protected void handleDataReceived(final String characteristicUuid, final byte[] rawData) {
        switch (characteristicUuid) {
            case LOGGING_INTERVAL_S_CHARACTERISTIC_UUID:
                mLoggerIntervalMs = 1000 * LittleEndianExtractor.extractShort(rawData);
                mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), mLoggerIntervalMs, LOGGER_INTERVAL_UNIT)};
                mServiceListener.onSetLoggerIntervalSuccess();
                break;
            case LOGGING_STATE_CHARACTERISTIC_UUID:
                mLoggerStateEnabled = ((int) rawData[0] > 0);
                Log.d(TAG, "Received LOGGING_STATE_CHARACTERISTIC_UUID data: " + mLoggerStateEnabled);
                continueDownloadProtocol();
                break;
            case CURRENT_POINTER_CHARACTERISTIC_UUID:
                mCurrentPointer = LittleEndianExtractor.extractInteger(rawData);
                Log.d(TAG, "Received CURRENT_POINTER_CHARACTERISTIC_UUID data: " + mCurrentPointer);
                continueDownloadProtocol();
                break;
            case START_POINTER_CHARACTERISTIC_UUID:
                mStartPointer = LittleEndianExtractor.extractInteger(rawData);
                Log.d(TAG, "Received START_POINTER_CHARACTERISTIC_UUID data: " + mStartPointer);
                continueDownloadProtocol();
                break;
            case END_POINTER_CHARACTERISTIC_UUID:
                mEndPointer = LittleEndianExtractor.extractInteger(rawData);
                Log.d(TAG, "Received END_POINTER_CHARACTERISTIC_UUID data: " + mEndPointer);
                continueDownloadProtocol();
                break;
            case USER_DATA_CHARACTERISTIC_UUID:
                mLoggingEnabledTimestamp = LittleEndianExtractor.extractInteger(rawData);
                Log.d(TAG, "Received USER_DATA_CHARACTERISTIC_UUID data: " + mLoggingEnabledTimestamp);
                break;
            case LOGGED_DATA_CHARACTERISTIC_UUID:
                Log.d(TAG, "Received LOGGED_DATA_CHARACTERISTIC_UUID data");
                handleDownloadedData(rawData);
                if (mDownloadState.equals(DownloadState.RUNNING)) {
                    mBleConnector.readCharacteristic(mDeviceAddress, LOGGED_DATA_CHARACTERISTIC_UUID);
                }
                break;
        }
    }

    @Override
    protected void handleDataWritten(final String characteristicUuid) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (characteristicUuid) {
                    case LOGGING_INTERVAL_S_CHARACTERISTIC_UUID:
                        readLoggerInterval();
                        break;
                    case LOGGING_STATE_CHARACTERISTIC_UUID:
                        readLoggingState();
                        break;
                    case START_POINTER_CHARACTERISTIC_UUID:
                        readStartPointer();
                        break;
                    case END_POINTER_CHARACTERISTIC_UUID:
                        readEndPointer();
                        break;
                    case USER_DATA_CHARACTERISTIC_UUID:
                        readUserData();
                        break;
                }
            }
        }, SHTC1_SPECIFIC_READ_AFTER_WRITE_DELAY_MS);
    }

    @Override
    protected boolean initiateDownloadProtocol() {
        mDownloadState = DownloadState.INIT;
        mLoggerStateBeforeDownload = isGadgetLoggingEnabled();
        mDownloadStartTimestamp = new Date();
        setGadgetLoggingEnabled(false);
        return true;
    }

    private void continueDownloadProtocol() {
        switch (mDownloadState) {
            case INIT:
                mDownloadState = DownloadState.CURRENT_POINTER;
                readCurrentPointer();
                break;
            case CURRENT_POINTER:
                mDownloadState = DownloadState.START_POINTER;
                writeStartPointer();
                break;
            case START_POINTER:
                mDownloadState = DownloadState.END_POINTER;
                writeEndPointer();
                break;
            case END_POINTER:
                mDownloadState = DownloadState.RUNNING;
                mNrOfElementsDownloaded = 0;
                mDownloadProgress = 0;
                mNrOfElementsToDownload = calculateValuesToDownload();
                if (mNrOfElementsToDownload <= 0) {
                    Log.d(TAG, "No data to download");
                    onNoDataAvailable();
                    return;
                }
                // Start downloading... this reads the first download packet.
                mBleConnector.readCharacteristic(mDeviceAddress, LOGGED_DATA_CHARACTERISTIC_UUID);
                break;
            case RUNNING:
            case IDLE:
            default:
                // No download running
                break;
        }
    }

    private int calculateValuesToDownload() {
        final int totalNumberOfValues = mCurrentPointer - mStartPointer;
        return (totalNumberOfValues > GADGET_RING_BUFFER_SIZE) ? GADGET_RING_BUFFER_SIZE : totalNumberOfValues;
    }

    private void handleDownloadedData(final byte[] rawData) {
        if (rawData.length == 0) {
            onDownloadComplete();
            return;
        }

        final List<GadgetValue> downloadedValues = parseDownloadData(rawData);

        mServiceListener.onGadgetDownloadDataReceived(this,
                downloadedValues.toArray(new GadgetValue[downloadedValues.size()]),
                mDownloadProgress);
    }

    @NonNull
    private List<GadgetValue> parseDownloadData(final byte[] rawData) {
        final List<GadgetValue> downloadedValues = new ArrayList<>();
        for (int i = 0; i < rawData.length; i += DATA_POINT_SIZE) {
            final byte[] dataPoint = new byte[DATA_POINT_SIZE];
            final short[] humidityAndTemperature = new short[DATA_POINT_SIZE / 2];
            System.arraycopy(rawData, i, dataPoint, 0, DATA_POINT_SIZE);
            ByteBuffer.wrap(dataPoint).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

            final float temperature = ((float) humidityAndTemperature[0]) / 100f;
            final float humidity = ((float) humidityAndTemperature[1]) / 100f;
            final long timestamp = mLoggingEnabledTimestamp * 1000L + (mStartPointer + mNrOfElementsDownloaded) * (long) mLoggerIntervalMs;

            // NOTE:
            // The altTimestamp would enable us to do log downloads without the requirement
            //  to write the log enabled timestamp to user data. But it would require us to
            //  only be able to download data if logging is enabled. Hence, downloading
            //  would only be possible once, and on a disconnect, the data would be lost.
            //
            // Example:
            //
            // final long altTimestamp = mDownloadStartTimestamp.getTime() - (mLoggerIntervalMs *
            //      (mNrOfElementsToDownload - mNrOfElementsDownloaded));
            // Log.i(TAG, "DOWNLOADING DATA: timestamp comparison: legecy: " + timestamp +
            //      " vs. alternative: " + altTimestamp);

            downloadedValues.add(new SmartGadgetValue(new Date(timestamp), temperature, SHTC1TemperatureAndHumidityService.UNIT_T));
            downloadedValues.add(new SmartGadgetValue(new Date(timestamp), humidity, SHTC1TemperatureAndHumidityService.UNIT_RH));

            updateDownloadProgress();
        }
        return downloadedValues;
    }

    private void updateDownloadProgress() {
        mNrOfElementsDownloaded++;
        mDownloadProgress = (int) Math.ceil(100 * (mNrOfElementsDownloaded / (float) mNrOfElementsToDownload));
        if (mNrOfElementsDownloaded >= mNrOfElementsToDownload) {
            mDownloadProgress = 100;
        }
    }

    private void onDownloadComplete() {
        Log.i(TAG, "On Download Complete");
        mDownloadProgress = 100;
        mDownloadState = DownloadState.IDLE;
        setGadgetLoggingEnabled(mLoggerStateBeforeDownload);
        mServiceListener.onDownloadCompleted(this);
    }

    private void onNoDataAvailable() {
        mDownloadProgress = 0;
        mDownloadState = DownloadState.IDLE;
        setGadgetLoggingEnabled(mLoggerStateBeforeDownload);
        mServiceListener.onDownloadNoData(this);
    }

    private void readLoggerInterval() {
        mBleConnector.readCharacteristic(mDeviceAddress, LOGGING_INTERVAL_S_CHARACTERISTIC_UUID);
    }

    private void readLoggingState() {
        mBleConnector.readCharacteristic(mDeviceAddress, LOGGING_STATE_CHARACTERISTIC_UUID);
    }

    private void readCurrentPointer() {
        mBleConnector.readCharacteristic(mDeviceAddress, CURRENT_POINTER_CHARACTERISTIC_UUID);
    }

    private void readStartPointer() {
        mBleConnector.readCharacteristic(mDeviceAddress, START_POINTER_CHARACTERISTIC_UUID);
    }

    private void writeStartPointer() {
        writeValueToCharacteristic(START_POINTER_CHARACTERISTIC_UUID, calculateSmallestPossibleStartPointer(), FORMAT_UINT32, 0);
    }

    private void readEndPointer() {
        mBleConnector.readCharacteristic(mDeviceAddress, END_POINTER_CHARACTERISTIC_UUID);
    }

    private void writeEndPointer() {
        writeValueToCharacteristic(END_POINTER_CHARACTERISTIC_UUID, mCurrentPointer, FORMAT_UINT32, 0);
    }

    private void readUserData() {
        mBleConnector.readCharacteristic(mDeviceAddress, USER_DATA_CHARACTERISTIC_UUID);
    }

    /*
     * The User data is used to store the time stamp when logging was enabled. This enables us to
     * recreate the timestamps of downloaded samples by multiplying the current pointer with the
     * logger interval, giving us the timestamp of the first downloaded sample.
     * The C1 gadget sets the current pointer to 0 when re-enabling the logger state. Hence, we can
     * only download the data once, as we disable the logging before downloading the data and then
     * re-enable it.
     * When it fails, the data is lost. We'd need to not re-enable the download on failed downloads
     */
    private void writeLoggingStartTimestamp() {
        writeValueToCharacteristic(USER_DATA_CHARACTERISTIC_UUID,
                (int) (System.currentTimeMillis() / 1000L), FORMAT_UINT32, 0);
    }

    private int calculateSmallestPossibleStartPointer() {
        if (mCurrentPointer > GADGET_RING_BUFFER_SIZE) {
            return mCurrentPointer - GADGET_RING_BUFFER_SIZE;
        }
        return 1;
    }

    enum DownloadState {
        IDLE, INIT, CURRENT_POINTER, START_POINTER, END_POINTER, RUNNING
    }
}
