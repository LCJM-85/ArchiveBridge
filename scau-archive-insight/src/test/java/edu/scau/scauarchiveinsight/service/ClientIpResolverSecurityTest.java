package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverSecurityTest {

    @Test
    void trustedProxyTakesRightmostUntrustedAddressInsteadOfSpoofedLeftmostValue() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.99, 198.51.100.24");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.24");
    }

    @Test
    void trustedProxyChainIsRemovedFromRightToLeft() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8,192.168.0.0/16");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "198.51.100.20, 192.168.1.3");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void malformedForwardedChainFallsBackToImmediatePeer() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "198.51.100.20, attacker.example");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void trustedProxyMayUseValidRealIpWhenForwardedChainIsAbsent() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Real-IP", "198.51.100.20");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void broadContainerProxyRangeDoesNotCollapsePrivateLanClientsIntoOneKey() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12,192.168.0.0/16");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.4");
        request.addHeader("X-Forwarded-For", "192.168.1.25");

        assertThat(resolver.resolve(request)).isEqualTo("192.168.1.25");
    }

    @Test
    void fullyTrustedForwardedChainReturnsFarthestKnownClient() {
        ClientIpResolver resolver = new ClientIpResolver(
                "10.0.0.0/8,172.16.0.0/12,192.168.0.0/16"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.4");
        request.addHeader("X-Forwarded-For", "192.168.1.25, 10.0.0.7");

        assertThat(resolver.resolve(request)).isEqualTo("192.168.1.25");
    }

    @Test
    void equivalentIpv6FormsProduceOneCanonicalRateLimitKey() {
        ClientIpResolver resolver = new ClientIpResolver("fd00::/8");
        MockHttpServletRequest compressed = new MockHttpServletRequest();
        compressed.setRemoteAddr("fd00::1");
        compressed.addHeader("X-Forwarded-For", "2001:db8::1");
        MockHttpServletRequest expanded = new MockHttpServletRequest();
        expanded.setRemoteAddr("fd00:0:0:0:0:0:0:1");
        expanded.addHeader("X-Forwarded-For", "2001:0db8:0:0:0:0:0:1");

        assertThat(resolver.resolve(compressed))
                .isEqualTo("2001:db8:0:0:0:0:0:1")
                .isEqualTo(resolver.resolve(expanded));
    }
}
