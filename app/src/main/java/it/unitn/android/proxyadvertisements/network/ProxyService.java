/**
 * Created by mat - 2016
 */

package it.unitn.android.proxyadvertisements.network;

import android.os.Bundle;

public interface ProxyService {

    public abstract boolean isConfigured();

    public abstract boolean isActive();

    public abstract void init(Bundle bundle);

    public abstract void destroy();

    public abstract void activate();

    public abstract void deactivate();

    public abstract void send(NetworkMessage msg);

}

