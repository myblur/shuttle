package me.iblur.shuttle;

import me.iblur.shuttle.conf.Configuration;
import me.iblur.shuttle.server.ShuttleProxyServer;
import picocli.CommandLine;

import javax.net.ssl.SSLException;

/**
 * @since 2021-04-15 14:48
 */
public class ShuttleStartup {

    public static void main(String[] args) throws InterruptedException, SSLException {
        Configuration configuration = new Configuration();
        try {
            CommandLine.populateCommand(configuration, args);
            if (configuration.isHelp()) {
                CommandLine.usage(configuration, System.out);
                return;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            CommandLine.usage(configuration, System.out);
            return;
        }
        ShuttleProxyServer shuttleProxyServer = new ShuttleProxyServer(configuration);
        shuttleProxyServer.start();
    }

}
