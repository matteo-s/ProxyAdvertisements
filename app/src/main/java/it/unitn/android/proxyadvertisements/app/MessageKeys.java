/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.app;


import it.unitn.android.proxyadvertisements.MainActivity;

public class MessageKeys {


    /*
    * Listeners
     */

    public static final String DEST_SERVICE = MainService.class.getName();
    public static final String DEST_ACTIVITY = MainActivity.class.getName();


    /*
    * Messages
    */

    public static final String TYPE = "type";

    /*
    * Service
     */
    public static final int SERVICE_START = 11;
    public static final int SERVICE_STOP = 12;
    public static final int SERVICE_CLOSE = 13;
    public static final int SERVICE_STATUS = 14;
    public static final int SERVICE_ERROR = 19;


    /*
    * Clock
     */
    public static final int CLOCK_RESET = 30;
    public static final int CLOCK_INCREMENT = 31;
    public static final int CLOCK_SYNC = 32;
    public static final int CLOCK_PERIOD = 33;
    public static final int CLOCK_BROADCAST = 34;
    public static final int CLOCK_RECEIVE = 35;
    public static final int CLOCK_INQUIRY = 36;
    public static final int CLOCK_SEND = 37;
    public static final int CLOCK_ERROR = 39;


    /*
    * Network
     */
    public static final int NETWORK_RESET = 40;
    public static final int NETWORK_START = 41;
    public static final int NETWORK_STOP = 42;
    public static final int NETWORK_INIT = 43;
    public static final int NETWORK_INFO = 44;
    public static final int NETWORK_SEND = 45;
    public static final int NETWORK_RECEIVE = 46;
    public static final int NETWORK_ACTIVE = 47;
    public static final int NETWORK_INACTIVE = 48;
    public static final int NETWORK_ERROR = 49;

    /*
* Proxy
 */
    public static final int PROXY_RECEIVE = 51;
    public static final int PROXY_STOP = 52;

    /*
    * Log
     */
    public static final int LOG_EXPORT = 61;
    public static final int LOG_CLEAR = 62;

}
