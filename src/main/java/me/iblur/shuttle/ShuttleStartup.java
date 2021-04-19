package me.iblur.shuttle;

import me.iblur.shuttle.conf.Configuration;
import me.iblur.shuttle.server.ShuttleProxyServer;

/**
 * @since 2021-04-15 14:48
 */
public class ShuttleStartup {

    public static void main(String[] args) throws InterruptedException {
        Configuration configuration = new Configuration();
        configuration.setHost(args[0]);
        configuration.setPort(Integer.parseInt(args[1]));
        configuration.setDebug(Boolean.parseBoolean(args[2]));
        configuration.setDnsServer(args[3]);
        ShuttleProxyServer shuttleProxyServer = new ShuttleProxyServer(configuration);
        shuttleProxyServer.start();
    }
}
