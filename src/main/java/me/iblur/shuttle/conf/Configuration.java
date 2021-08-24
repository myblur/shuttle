package me.iblur.shuttle.conf;

import picocli.CommandLine;

/**
 * @since 2021-04-15 14:57
 */
@CommandLine.Command(name = "Configuration", description = "Basic configuration of proxy server",
        customSynopsis = {"java -jar shuttle.jar -h 127.0.0.1 -p 12000 --debug"}, showDefaultValues = true,
        sortOptions = false)
public class Configuration {

    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 1080;

    private static final int DEFAULT_BACK_LOG = 256;

    public static final int DNS_PORT = 53;

    public static final int DEFAULT_DNS_OVER_TLS_PORT = 853;

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    @CommandLine.Option(names = {"-h", "--host"}, description = "Proxy server bind host", defaultValue = DEFAULT_HOST)
    private String host = DEFAULT_HOST;

    @CommandLine.Option(names = {"-p", "--port"}, description = "Proxy server bind port", defaultValue =
            DEFAULT_PORT + "")
    private int port = DEFAULT_PORT;

    @CommandLine.Option(names = {"-ct", "--connect-timeout"}, description = "The proxy server connects to the remote " +
            "server timeout time, ms", defaultValue = DEFAULT_CONNECT_TIMEOUT + "")
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    @CommandLine.Option(names = {"--backlog"}, description = "Proxy server backlog", defaultValue = DEFAULT_BACK_LOG + "")
    private int backlog = DEFAULT_BACK_LOG;

    @CommandLine.Option(names = {"-d", "--dns"}, description = "The DNS used by the proxy service to connect to the " +
            "remote server client")
    private String dns;

    @CommandLine.Option(names = {"-dot"}, description = "The DNS over TLS server address")
    private String dot;

    @CommandLine.Option(names = {"--debug"}, description = "Display transmit information")
    private boolean debug;

    @CommandLine.Option(names = {"--help"}, usageHelp = true)
    private boolean help;

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(final int backlog) {
        this.backlog = backlog;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(final String dns) {
        this.dns = dns;
    }

    public String getDot() {
        return dot;
    }

    public void setDot(final String dot) {
        this.dot = dot;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(final boolean help) {
        this.help = help;
    }
}
