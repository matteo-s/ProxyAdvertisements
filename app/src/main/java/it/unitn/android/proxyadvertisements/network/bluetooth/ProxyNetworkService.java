/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.network.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

import it.unitn.android.proxyadvertisements.app.MessageKeys;
import it.unitn.android.proxyadvertisements.app.ServiceConnector;
import it.unitn.android.proxyadvertisements.network.NetworkMessage;
import it.unitn.android.proxyadvertisements.network.ProxyService;


public class ProxyNetworkService implements ProxyService {

    /*
    * Constants
     */
    public static final UUID Service_UUID = UUID
            .fromString("0000b81d-0000-1000-8001-00805f9b34fb");


    /*
* Context
 */
    private Context mContext;
    private ServiceConnector mService = null;
    private Handler mHandler;

    /*
    * Bluetooth
     */
    private BluetoothAdapter mAdapter;


    private ProxyNetworkMessage mMessage;
    private ProxyServer mServer;

    private boolean isAvailable;
    private boolean isSupported;

     /*
    * Data
     */


    boolean isConnected = false;
    boolean isActive = false;
    boolean hasConnection = false;


    public ProxyNetworkService(Context context, ServiceConnector serviceConnector) {
        mAdapter = null;
        isAvailable = false;
        isSupported = false;
        isConnected = false;

        this.mMessage = null;

        this.mContext = context;
        this.mService = serviceConnector;

        //create an handler for delayed tasks
        mHandler = new Handler();
    }


    public boolean isConfigured() {
        return (mAdapter != null);
    }

    public boolean isActive() {
        return isActive;
    }

    public void init(Bundle bundle) {
        Log.v("ProxyNetworkService", "init");


        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mAdapter != null) {
            isSupported = true;

            //check if active
            if (mAdapter.isEnabled()) {
                isAvailable = true;

            } else {
                //set not active
                isAvailable = false;
            }
        }


    }


    public void destroy() {
        deactivate();

        // unbind or process might have crashes
        mService.unbindService();
    }

    public void activate() {
        Log.v("ProxyNetworkService", "activate");

        //bind
        mService.bindService();

        //start connection
        if (mServer == null) {
            mServer = new ProxyServer(mAdapter, mService);
        }

        //listen
        mServer.listen();

        //start indefinitely
        isActive = true;

        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

    }

    public void deactivate() {
        Log.v("ProxyNetworkService", "deactivate");

        //set inactive
        isActive = false;

        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception ex) {
        }

        if (mServer != null) {
            mServer.disconnect();
        }

        //clear status
        hasConnection = false;

        mService.sendMessage(MessageKeys.NETWORK_STOP, null);
    }

    /*
    * Broadcast
     */
    public void send(NetworkMessage msg) {
        //replace message
        mMessage = ProxyNetworkMessage.parse(msg);

        if (mServer != null && mServer.isConnected()) {
            mServer.send(mMessage);
        }
    }

     /*
    * Helpers - ble
     */

    protected String readAddress(BluetoothAdapter adapter) {
        return adapter.getAddress();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);

            if (state == BluetoothDevice.BOND_BONDED) {
                Log.v("ProxyNetworkService", "Device " + device + " PAIRED");
            } else if (state == BluetoothDevice.BOND_BONDING) {
                Log.v("ProxyNetworkService", "Device " + device + " pairing is in process...");
            } else if (state == BluetoothDevice.BOND_NONE) {
                Log.v("ProxyNetworkService", "Device " + device + " is unpaired");

                //check for active connection and restart
                if (isActive) {
                    if (mServer != null) {
                        mServer.disconnect();
                        mServer.listen();
                    }
                }


            } else {
                Log.v("ProxyNetworkService", "Device " + device + " is in undefined state");
            }
        }
    };

}
