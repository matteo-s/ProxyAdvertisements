/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.network.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import it.unitn.android.proxyadvertisements.app.MessageKeys;
import it.unitn.android.proxyadvertisements.app.ServiceConnector;

public class ProxyServer {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mAdapter;

    private boolean isActive = false;
    private boolean isConnected = false;

    private Handler mHandler;

    private Context mContext;
    private ServiceConnector mService = null;

    private BluetoothDevice mDevice;
    private BluetoothLeAdvertiser mAdvertiser;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattService mBluetoothGattService;

    private BluetoothGattCharacteristic mReceive;
    private BluetoothGattCharacteristic mSend;


    public ProxyServer(BluetoothAdapter adapter, BluetoothManager manager, Context context, ServiceConnector serviceConnector) {
        this.mAdapter = adapter;
        this.mBluetoothManager = manager;

        this.mContext = context;
        this.mService = serviceConnector;

        this.mDevice = null;
        this.mBluetoothGatt = null;
        this.mBluetoothGattServer = null;
        this.mBluetoothGattService = null;

        this.mReceive = null;
        this.mSend = null;

        //create an handler for delayed tasks
        mHandler = new Handler();

        //init now
        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        //get advertiser
        mAdvertiser = mAdapter.getBluetoothLeAdvertiser();


        isActive = false;
        isConnected = false;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isConnected() {
        return isConnected;
    }


    public void listen() {
        if (!isActive) {

            //register gatt
            mBluetoothGattService = new BluetoothGattService(
                    UUID.fromString(BLEGattAttributes.SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

            Log.v("ProxyServer", "gatt service uuid " + mBluetoothGattService.getUuid().toString());

            ProxyNetworkMessage message = new ProxyNetworkMessage();

            mReceive = new BluetoothGattCharacteristic(
                    UUID.fromString(BLEGattAttributes.PROXY_RECEIVE),
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            mReceive.setValue(message.buildData());
            mBluetoothGattService.addCharacteristic(mReceive);

            mSend = new BluetoothGattCharacteristic(
                    UUID.fromString(BLEGattAttributes.PROXY_SEND),
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            mSend.setValue(message.buildData());
//            mSend.setValue(1234, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            byte[] bytes = mSend.getValue();
            Log.v("ProxyServer", "send bytes length " + String.valueOf(bytes.length) + ": " + ProxyNetworkMessage.byteArrayToString(bytes));


            mBluetoothGattService.addCharacteristic(mSend);

            mBluetoothGattServer.addService(mBluetoothGattService);

            mAdvertiser.startAdvertising(buildAdvertiseSettings(), buildAdvertiseData(), mAdvertiseCallback);

            isActive = true;

        }
        Log.v("ProxyServer", "listening");

    }

    public void disconnect() {
        Log.v("ProxyServer", "disconnect");
        isActive = false;

        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.clearServices();
//            mBluetoothGattServer.close();
//
//            mBluetoothGattServer = null;

        }

        if (mAdvertiser != null) {
            mAdvertiser.stopAdvertising(mAdvertiseCallback);
        }

        mBluetoothGattService = null;
        this.mReceive = null;
        this.mSend = null;
    }


    public void send(ProxyNetworkMessage m) {
        if (isActive && mSend != null) {
            StringBuilder vector = new StringBuilder();
            for (int i = 1; i <= ProxyNetworkMessage.SLOTS; i++) {
                if (m.clocks.containsKey(i)) {
                    vector.append(Short.toString(m.clocks.get(i)));
                } else {
                    vector.append("0");
                }
            }
            Log.v("ProxyServer", "write data : " + vector.toString());

            byte[] bytes = m.buildData();

            Log.v("ProxyServer", "write bytes length " + String.valueOf(bytes.length) + ": " + ProxyNetworkMessage.byteArrayToString(bytes));

            mSend.setValue(bytes);

            //notify connected devices
            List<BluetoothDevice> devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            for (BluetoothDevice device : devices) {
                try {
                    mBluetoothGattServer.notifyCharacteristicChanged(device, mSend, false);
                } catch (Exception ex) {
                    //ignore
                }
            }

        }
    }


    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.v("ProxyServer", "start advertising");

            //call super
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            String reason = "UNKNOWN";
            //from AdvertiseCallback
            switch (errorCode) {
                case 1:
                    reason = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                    break;
                case 2:
                    reason = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                    break;
                case 3:
                    reason = "ADVERTISE_FAILED_ALREADY_STARTED";
                    break;
                case 4:
                    reason = "ADVERTISE_FAILED_INTERNAL_ERROR";
                    break;
                case 5:
                    reason = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                    break;
            }
            Log.v("ProxyServer", "start advertising failure " + String.valueOf(errorCode) + ": " + reason);

            //call super
            super.onStartFailure(errorCode);
        }
    };


    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d("ProxyServer", "gatt server connection state changed, new state " + String.valueOf(newState));
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("ProxyServer", "gatt server service was added.");
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d("ProxyServer", "gatt characteristic was read.");
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            //send response
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("ProxyServer", "We have received a write request for one of our hosted characteristics, response needed " + String.valueOf(responseNeeded));
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            //check if msg
            if (characteristic.getUuid().equals(UUID.fromString(BLEGattAttributes.PROXY_RECEIVE))) {
                byte[] bytes = Arrays.copyOf(value, value.length);
                //dump
                Log.v("ProxyServer", "receive data on characteristic " + ProxyNetworkMessage.byteArrayToString(bytes));

                //get message from service data bytes
                ProxyNetworkMessage n = ProxyNetworkMessage.parseData(bytes);

                StringBuilder vector = new StringBuilder();
                for (int i = 1; i <= ProxyNetworkMessage.SLOTS; i++) {
                    if (n.clocks.containsKey(i)) {
                        vector.append(Short.toString(n.clocks.get(i)));
                    } else {
                        vector.append("0");
                    }
                }


                Log.v("ProxyServer", "received msg : " + vector.toString());


                //directly send to service
                Bundle bundle = new Bundle();
                bundle.putSerializable("n", n);

                mService.sendMessage(MessageKeys.PROXY_RECEIVE, bundle);
            }

        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d("ProxyServer", "Our gatt server descriptor was read.");
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("ProxyServer", "Our gatt server descriptor was written.");
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d("ProxyServer", "Our gatt server on execute write.");
            super.onExecuteWrite(device, requestId, execute);


        }
    };

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    public AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
//        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTimeout(180000);
        return settingsBuilder.build();
    }

    public AdvertiseData buildAdvertiseData() {
        ParcelUuid uuid = ParcelUuid.fromString(BLEGattAttributes.SERVICE_UUID);
        byte[] bytes = new byte[1];


        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false);
        dataBuilder.setIncludeDeviceName(true);
//        dataBuilder.addServiceUuid(uuid);
        return dataBuilder.build();
    }

}
