/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.network.ble;

import java.util.HashMap;

public class BLEGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String SERVICE_UUID = "0000b81d-0000-1001-8001-00805f9b34fd";
    public static String PROXY_SEND = "0000b81d-0000-1001-8002-00805f9b34fd";
    public static String PROXY_RECEIVE = "0000b81d-0000-1001-8003-00805f9b34fd";

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
