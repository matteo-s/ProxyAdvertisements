/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Map;
import java.util.UUID;

import it.unitn.android.proxyadvertisements.MainActivity;
import it.unitn.android.proxyadvertisements.network.NetworkMessage;
import it.unitn.android.proxyadvertisements.network.ble.BLENetworkService;
import it.unitn.android.proxyadvertisements.network.proxy.ProxyNetworkService;
import matteos.unitn.eu.proxyadvertisements.R;

public class MainService extends Service {

    /*
    * Global
     */
    public static int SERVICE_ID = 1002;
    public boolean RUNNING = false;
    private String uuid;

 /*
    * Network
     */

    private BLENetworkService mNetworkService;
    private ProxyNetworkService mProxyService;


    /*
    * App
     */
    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastManager;
    private Messenger mMessenger;


    /*
  * Service
   */
    @Override
    public void onCreate() {

        super.onCreate();

        Log.v("MainService", "onCreate " + this);

        // An Android handler thread internally operates on a looper.
        mHandlerThread = new HandlerThread("MainService.HandlerThread");
        mHandlerThread.start();
        // An Android service handler is a handler running on a specific background thread.
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        // Get access to local broadcast manager
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //messenger for ipc
        mMessenger = new Messenger(mServiceHandler);

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }

        Log.v("MainService", "onCreate uuid " + uuid);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("MainService", "onStartCommand");


        // Keep service around "sticky"
        // Return "sticky" for services that are explicitly
        // started and stopped as needed by the app.
        return START_STICKY;
//        return START_NOT_STICKY;
//        return START_REDELIVER_INTENT;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("MainService", "onDestroy");

        //stop
        stop();

        // Cleanup service before destruction
        mHandlerThread.quit();
    }

    // Binding is another way to communicate between service and activity
    @Override
    public IBinder onBind(Intent intent) {
        Log.v("MainService", "onBind");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("MainService", "onUnbind");
        //call destroy
        destroy();

        return super.onUnbind(intent);
    }

    // Define how the handler will process messages
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        // Define how to handle any incoming messages here
        @Override
        public void handleMessage(Message msg) {
            // ...
            // When needed, stop the service with
            // stopSelf();
            Log.v("ServiceHandler", "ServiceHandler handleMessage " + msg.what);

            Bundle bundle = msg.getData();

            switch (msg.what) {
                case MessageKeys.SERVICE_START:

                    Log.v("MainService msg", "service start");

                    //start
                    start(bundle);
                    break;
                case MessageKeys.SERVICE_STOP:
                    Log.v("MainService msg", "service stop");

                    //stop
                    stop();
                    break;
                case MessageKeys.SERVICE_STATUS:
                    Log.v("MainService msg", "service status");

                    //status
                    status();
                    break;
                case MessageKeys.SERVICE_CLOSE:
                    //destroy service
                    destroy();
                    break;

                case MessageKeys.CLOCK_RECEIVE:
                    final NetworkMessage n = (NetworkMessage) bundle.getSerializable("n");
                    Log.v("MainService msg", "clock receive from " + n.sender);
                    //proxy
                    proxy(n);
                    break;

                case MessageKeys.PROXY_RECEIVE:
                    final NetworkMessage p = (NetworkMessage) bundle.getSerializable("n");
                    Log.v("MainService msg", "proxy receive from " + p.sender);
                    //ble
                    ble(p);
                    break;

                default:
                    super.handleMessage(msg);
            }


        }
    }


    /*
    * Network
     */


    public void proxy(NetworkMessage msg) {
        if (mProxyService != null) {
            mProxyService.send(msg);
        }
    }

    public void ble(NetworkMessage msg) {
        if (mNetworkService != null) {
            mNetworkService.broadcast(msg);
        }
    }

    /*
    * app
     */


    protected void start(Bundle bundle) {
        Log.v("MainService", "MainService start ");
        if (!RUNNING) {
            if (mNetworkService == null) {
                mNetworkService = new BLENetworkService(this, new ServiceConnector(this));
            }

            if (mProxyService == null) {
                mProxyService = new ProxyNetworkService(this, new ServiceConnector(this));
            }

            //init, if already initialized nothing happens
            mNetworkService.init(bundle);
            mProxyService.init(bundle);

            RUNNING = true;


            //start in foreground
            Intent notificationIntent = new Intent(this, MainActivity.class);
//        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_launcher);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("ProxyAdvertisement")
                    .setTicker("ProxyAdvertisement")
                    .setContentText("Service is running")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(
                            Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            startForeground(SERVICE_ID, notification);

            //start, will cause bind
            mNetworkService.activate();
            mProxyService.activate();

        }

        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.SERVICE_START);
                intent.putExtra("status", RUNNING);
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });

    }


    protected void status() {
        Log.v("MainService", "MainService status running " + String.valueOf(RUNNING));


        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.SERVICE_STATUS);
                intent.putExtra("status", RUNNING);
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });
    }

    protected void stop() {
        Log.v("MainService", "MainService stop ");
        RUNNING = false;


        //network
        if (mNetworkService != null) {
            mNetworkService.deactivate();
        }

        if (mProxyService != null) {
            mProxyService.deactivate();
        }

        //clear notification
        stopForeground(true);

        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.SERVICE_STOP);
                intent.putExtra("message", "MainService stopped");
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });

    }

    protected void destroy() {
        Log.v("MainService", "MainService destroy");

        //network
        if (mNetworkService != null) {
            mNetworkService.destroy();
        }
        if (mProxyService != null) {
            mProxyService.destroy();
        }

        Log.v("MainService", "MainService destroy, stop self");
        this.stopSelf();
    }

}
