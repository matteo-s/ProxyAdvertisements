/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.network.proxy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import it.unitn.android.proxyadvertisements.app.MessageKeys;
import it.unitn.android.proxyadvertisements.app.ServiceConnector;

public class ProxyServer {
    private BluetoothAdapter mAdapter;

    private boolean isActive = false;
    private boolean isConnected = false;

    private Handler mHandler;

    private ServiceConnector mService = null;

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    private AcceptThread mServer;
    private ConnectedThread mConnection;


    public ProxyServer(BluetoothAdapter adapter, ServiceConnector serviceConnector) {
        this.mAdapter = adapter;
        this.mService = serviceConnector;

        this.mDevice = null;
        this.mSocket = null;
        this.mConnection = null;

        //create an handler for delayed tasks
        mHandler = new Handler();


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
            // Cancel discovery because it will slow down the connection
            mAdapter.cancelDiscovery();

            BluetoothSocket tmp = null;


            mServer = new AcceptThread();
            //start thread
            mServer.start();

            isActive = true;
        }

        Log.v("ProxyServer", "listening");

    }

    public void disconnect() {
        Log.v("ProxyServer", "disconnect");
        isActive = false;
        isConnected = false;

        if (mConnection != null) {
            //stop handler
            mConnection.cancel();

            mConnection = null;
        }

        if (mServer != null) {
            //stop handler
            mServer.cancel();

            mServer = null;
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException closeException) {
            }

            mSocket = null;
        }

        if (mDevice != null) {
            mDevice = null;
        }


    }

    public void reset() {
        Log.v("ProxyServer", "reset");
        boolean restart = (isActive && isConnected);

        disconnect();

        if (restart) {
            listen();
        }
    }


    public void send(ProxyNetworkMessage m) {
        if (isConnected && mConnection != null) {

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


            mConnection.write(bytes);
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();

                //reset
                reset();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int length; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    length = mmInStream.read(buffer);

                    if (length > 0) {
                        //parse bytes
                        byte[] bytes = Arrays.copyOf(buffer, length);

                        //dump
                        Log.v("ProxyServer", "receive data on socket " + ProxyNetworkMessage.byteArrayToString(bytes));

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

                } catch (IOException e) {
                    break;
                }
            }

            //disconnect
            reset();
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                //disconnect
                reset();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mAdapter.listenUsingRfcommWithServiceRecord("advertisements", ProxyNetworkService.Service_UUID);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {

            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    //set handler
                    mConnection = new ConnectedThread(socket);
                    mConnection.start();

                    isConnected = true;

                    //close server socket
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                    }
                    break;
                }
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

}
