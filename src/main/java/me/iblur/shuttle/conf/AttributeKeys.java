package me.iblur.shuttle.conf;

import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.util.AttributeKey;

public final class AttributeKeys {

    private AttributeKeys() {
    }

    public static final AttributeKey<Configuration> CONFIGURATION_ATTR_KEY = AttributeKey.newInstance(
            "configuration");

    public static final AttributeKey<DnsAddressResolverGroup> ADDRESS_RESOLVER_GROUP_ATTR_KEY =
            AttributeKey.newInstance("dnsAddressResolverGroup");

}
