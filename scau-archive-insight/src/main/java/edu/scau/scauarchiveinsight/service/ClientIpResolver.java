package edu.scau.scauarchiveinsight.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class ClientIpResolver {

    private static final int MAX_FORWARDED_HEADER_LENGTH = 1024;
    private final List<IpAddressMatcher> trustedProxies;

    @Autowired
    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String trustedProxyCidrs) {
        this.trustedProxies = parseTrustedProxies(trustedProxyCidrs);
    }

    public String resolve(HttpServletRequest request) {
        Optional<String> normalizedPeer = normalizeIpLiteral(request.getRemoteAddr());
        if (normalizedPeer.isEmpty()) return "unknown";

        String immediatePeer = normalizedPeer.get();
        if (!isTrusted(immediatePeer)) return immediatePeer;

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            if (forwardedFor.length() > MAX_FORWARDED_HEADER_LENGTH) return immediatePeer;
            String[] chain = forwardedFor.split(",", -1);
            String farthestForwardedAddress = null;
            for (int i = chain.length - 1; i >= 0; i--) {
                Optional<String> address = normalizeIpLiteral(chain[i]);
                if (address.isEmpty()) return immediatePeer;
                farthestForwardedAddress = address.get();
                if (!isTrusted(address.get())) return address.get();
            }
            // 代理链全部可信时，保留链中最左侧、离当前服务最远的已知来源。
            return farthestForwardedAddress == null ? immediatePeer : farthestForwardedAddress;
        }

        Optional<String> realIp = normalizeIpLiteral(request.getHeader("X-Real-IP"));
        return realIp.orElse(immediatePeer);
    }

    private boolean isTrusted(String address) {
        return trustedProxies.stream().anyMatch(matcher -> matcher.matches(address));
    }

    private static List<IpAddressMatcher> parseTrustedProxies(String configuredCidrs) {
        if (configuredCidrs == null || configuredCidrs.isBlank()) return List.of();

        List<IpAddressMatcher> matchers = new ArrayList<>();
        for (String rawEntry : configuredCidrs.split(",")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) continue;

            String[] cidrParts = entry.split("/", -1);
            if (cidrParts.length > 2) {
                throw new IllegalArgumentException("无效的可信代理 CIDR: " + entry);
            }
            String address = normalizeIpLiteral(cidrParts[0])
                    .orElseThrow(() -> new IllegalArgumentException("无效的可信代理 IP: " + entry));
            String normalizedEntry = address;
            if (cidrParts.length == 2) {
                int maxBits = address.contains(":") ? 128 : 32;
                final int prefixLength;
                try {
                    prefixLength = Integer.parseInt(cidrParts[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("无效的可信代理 CIDR: " + entry, e);
                }
                if (prefixLength < 0 || prefixLength > maxBits) {
                    throw new IllegalArgumentException("无效的可信代理 CIDR: " + entry);
                }
                normalizedEntry += "/" + prefixLength;
            }
            matchers.add(new IpAddressMatcher(normalizedEntry));
        }
        return List.copyOf(matchers);
    }

    private static Optional<String> normalizeIpLiteral(String rawAddress) {
        if (rawAddress == null) return Optional.empty();
        String candidate = rawAddress.trim();
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.isEmpty() || candidate.length() > 45) return Optional.empty();

        boolean ipv6 = candidate.indexOf(':') >= 0;
        if (ipv6) {
            if (!candidate.matches("[0-9A-Fa-f:.]+")) return Optional.empty();
        } else {
            if (!candidate.matches("[0-9.]+")) return Optional.empty();
            String[] octets = candidate.split("\\.", -1);
            if (octets.length != 4) return Optional.empty();
            for (String octet : octets) {
                if (octet.isEmpty() || octet.length() > 3) return Optional.empty();
                int value;
                try {
                    value = Integer.parseInt(octet);
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
                if (value > 255) return Optional.empty();
            }
        }

        try {
            InetAddress parsed = InetAddress.getByName(candidate);
            if (ipv6 && !(parsed instanceof Inet6Address) && !(parsed instanceof Inet4Address)) {
                return Optional.empty();
            }
            if (!ipv6 && !(parsed instanceof Inet4Address)) return Optional.empty();
            return Optional.of(parsed.getHostAddress().toLowerCase(Locale.ROOT));
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }
}
