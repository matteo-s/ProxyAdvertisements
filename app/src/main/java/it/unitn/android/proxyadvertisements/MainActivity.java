package it.unitn.android.proxyadvertisements;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import it.unitn.android.proxyadvertisements.app.MainService;
import it.unitn.android.proxyadvertisements.app.MessageKeys;
import matteos.unitn.eu.proxyadvertisements.R;

public class MainActivity extends Activity {
    final static String NAMESPACE = "it.unitn.android.proxyadvertisements.app";
    final static int REQUEST_ENABLE_BT = 102;

    boolean isBound = false;
    Messenger mMessenger;

    boolean isRunning = false;


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;

            // Create the Messenger object
            mMessenger = new Messenger(service);

            //require update from service
            refreshMainService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // unbind or process might have crashes
            mMessenger = null;
            isBound = false;
        }
    };

    protected void sendMessage(int key, Bundle bundle) {
        if (isBound) {
            // Create a Message
            Message msg = Message.obtain(null, key, 0, 0);

//        // Create a bundle with the data
//        Bundle bundle = new Bundle();
//        bundle.putString("hello", "world");

            // Set the bundle data to the Message
            if (bundle != null) {
                msg.setData(bundle);
            }
            // Send the Message to the Service (in another process)
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("ProxyAdvertisements");

        Log.v("MainActivity", "onCreate");

        Intent i = new Intent(this, MainService.class);
        // Start the service
        startService(i);
        //bind service
        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);

        activityMain();
    }

    @Override
    protected void onDestroy() {

        //kill service if not active
        if (!isRunning && isBound) {
            sendMessage(MessageKeys.SERVICE_CLOSE, null);
        }

        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.v("MainActivity", "onResume");

        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(MessageKeys.DEST_ACTIVITY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        // or `registerReceiver(testReceiver, filter)` for a normal broadcast
        if (isBound) {
            refreshMainService();
        }
    }

    @Override
    protected void onPause() {
        Log.v("MainActivity", "onPause");

        super.onPause();
        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        // or `unregisterReceiver(testReceiver)` for a normal broadcast
    }

    protected void activityMain() {
        //bind buttons
        final Button mainButtonService = (Button) findViewById(R.id.button_service);

        if (isRunning) {
            mainButtonService.setText("Stop");
        } else {
            mainButtonService.setText("Start");
        }

        mainButtonService.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isRunning) {
                    // stop
                    stopMainService();
                } else {
                    startMainService();
                }
            }
        });

        final Button mainButtonDiscoverable = (Button) findViewById(R.id.button_discoverable);
        mainButtonDiscoverable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent discoverableIntent = new
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
                startActivity(discoverableIntent);
            }
        });


    }

    public void startMainService() {
        Log.v("MainActivity", "startService");

        //set device discoverable for 180s
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
        startActivity(discoverableIntent);

        Bundle bundle = new Bundle();
        sendMessage(MessageKeys.SERVICE_START, bundle);
    }

    public void stopMainService() {
        Log.v("MainActivity", "stopService");

        sendMessage(MessageKeys.SERVICE_STOP, null);
    }

    public void refreshMainService() {
        Log.v("MainActivity", "refreshService");
        sendMessage(MessageKeys.SERVICE_STATUS, null);
    }

    // Define the callback for what to do when message is received
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra("type", 0);
            final Button mainButtonService = (Button) findViewById(R.id.button_service);

            switch (type) {
                case MessageKeys.SERVICE_START:
                    Log.v("MainActivity", "receive start");
                    isRunning = true;
                    mainButtonService.setText("Stop");
                    break;
                case MessageKeys.SERVICE_STOP:
                    Log.v("MainActivity", "receive stop");
                    isRunning = false;
                    mainButtonService.setText("Start");
                    break;
                case MessageKeys.SERVICE_STATUS:
                    Log.v("MainActivity", "receive status");
                    isRunning = intent.getBooleanExtra("status", false);
                    if (isRunning) {
                        mainButtonService.setText("Stop");
                    } else {
                        mainButtonService.setText("Start");
                    }
                    break;
//                default:
//                    String result = intent.getStringExtra("message");
//                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
//                    break;
            }


        }
    };


}
