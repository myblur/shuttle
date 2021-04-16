package me.iblur.shuttle.conf;

/**
 * @since 2021-04-15 14:57
 */
public class Configuration {

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String DEBUG = "debug";
    public static final String DNS_SEVER = "dnsServer";
    public static final int DNS_PORT = 53;

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    private String host;

    private int port;

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    private String dnsServer;

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

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getDnsServer() {
        return dnsServer;
    }

    public void setDnsServer(final String dnsServer) {
        this.dnsServer = dnsServer;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
