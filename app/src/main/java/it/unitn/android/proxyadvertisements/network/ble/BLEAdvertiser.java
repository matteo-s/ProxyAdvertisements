/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.network.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import it.unitn.android.proxyadvertisements.app.ServiceConnector;


public class BLEAdvertiser {


    private BluetoothAdapter mAdapter;
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiseSettings mSettings;
    private AdvertiseCallback mCallback;


    private boolean isActive = false;
    private Handler mHandler;
    private ServiceConnector mService = null;


    public BLEAdvertiser(BluetoothAdapter adapter,  ServiceConnector serviceConnector) {
        mAdapter = adapter;
        mService = serviceConnector;

        //create an handler for delayed tasks
        mHandler = new Handler();

        //get advertiser
        mAdvertiser = mAdapter.getBluetoothLeAdvertiser();

        mSettings = buildAdvertiseSettings();

        mCallback = new BLECallback(new ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int var1) {

            }
        });

    }


        /*
    * Advertiser
    * callback listener will be called after start+duration+stop cycle
     */


    public void advertise(final BLENetworkMessage m, final int duration, final ActionListener listener) {
        Log.v("BLEAdvertiser", "advertise for " + String.valueOf(duration));

        if (isActive) {
            //call stop with callback
            stop(new ActionListener() {

                @Override
                public void onSuccess() {
                    //call advertise now
                    advertise(m, duration, listener);
                }

                @Override
                public void onFailure(int error) {
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        } else {

            //use service data
//            final AdvertiseData advertiseData = m.buildServiceData();

            //use manufacturer data
            final AdvertiseData advertiseData = m.buildManufacturerData();

//            byte[] bytes = advertiseData.getServiceData().get(BLENetworkService.Service_UUID);
//            if (bytes != null) {
//                Log.v("BLEAdvertiser", "service data length " + String.valueOf(bytes.length) + ": " + BLENetworkMessage.byteArrayToString(bytes));
//            }
            SparseArray<byte[]> manufacturer = advertiseData.getManufacturerSpecificData();

            for (int q = 0; q < manufacturer.size(); q++) {
                byte[] mata = manufacturer.get(q);
                if (mata != null) {
                    Log.v("BLEAdvertiser", "manufacturer data " + String.valueOf(q) + " length " + String.valueOf(mata.length) + ": " + BLENetworkMessage.byteArrayToString(mata));
                }
            }

            StringBuilder vector = new StringBuilder();
            for (int i = 1; i <= BLENetworkMessage.SLOTS; i++) {
                if (m.clocks.containsKey(i)) {
                    vector.append(Short.toString(m.clocks.get(i)));
                } else {
                    vector.append("0");
                }
            }

            Log.v("BLEAdvertiser", "advertise data : " + vector.toString());


            //start
            start(advertiseData, mSettings, new ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = true;
                    Log.v("BLEAdvertiser", "started advertiser");

                    //use handler for stopping after duration - 1 shot
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLEAdvertiser", "advertiser expired");
                            stop(listener);
                        }
                    }, duration);

                }

                @Override
                public void onFailure(int error) {
                    isActive = false;

                    Log.v("BLEAdvertiser", "fail advertise");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }
    }

    public void start(AdvertiseData data, AdvertiseSettings settings, final ActionListener listener) {
        if (isActive) {
            stop(null);
        }
        mCallback = new BLECallback(listener);

        mAdvertiser.startAdvertising(settings, data, mCallback);
    }


    public void stop(final ActionListener listener) {

        mAdvertiser.stopAdvertising(mCallback);
        isActive = false;

        if (listener != null) {
            listener.onSuccess();
        }
    }


    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    public AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
//        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
//        settingsBuilder.setConnectable(false);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTimeout(BLENetworkService.ADVERTISE_DURATION);
        return settingsBuilder.build();
    }
}
