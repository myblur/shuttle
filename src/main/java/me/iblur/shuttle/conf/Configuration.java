package me.iblur.shuttle.conf;

/**
 * @since 2021-04-15 14:57
 */
public class Configuration {

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String DEBUG = "debug";

    private String host;

    private int port;

    private boolean debug;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
