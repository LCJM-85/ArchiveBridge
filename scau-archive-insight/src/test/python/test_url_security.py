import socket
import sys
import unittest
from pathlib import Path


MODULE_ROOT = Path(__file__).resolve().parents[2] / "main" / "python" / "ai_assistant"
sys.path.insert(0, str(MODULE_ROOT))

from rag.url_security import (
    validate_browser_subresource_url,
    validate_public_http_url,
)
from rag.document_loader import (
    _NETWORK_API_BLOCK_SCRIPT,
    _guard_browser_request,
    load_url,
)


def resolver_for(*addresses):
    def resolve(host, port, *, type=0):
        return [
            (
                socket.AF_INET6 if ":" in address else socket.AF_INET,
                type or socket.SOCK_STREAM,
                socket.IPPROTO_TCP,
                "",
                (address, port, 0, 0) if ":" in address else (address, port),
            )
            for address in addresses
        ]

    return resolve


class PublicUrlSecurityTest(unittest.TestCase):
    def test_accepts_http_and_https_when_every_dns_answer_is_public(self):
        public_dns = resolver_for("93.184.216.34", "2606:2800:220:1:248:1893:25c8:1946")

        self.assertEqual(
            "https://example.test/docs?q=1#intro",
            validate_public_http_url(
                "https://example.test/docs?q=1#intro", resolver=public_dns
            ),
        )
        self.assertEqual(
            "http://example.test:8080/",
            validate_public_http_url(
                "http://example.test:8080/", resolver=public_dns
            ),
        )

    def test_rejects_non_http_schemes_and_userinfo(self):
        public_dns = resolver_for("93.184.216.34")

        for url in (
            "file:///etc/passwd",
            "gopher://example.test/resource",
            "javascript:alert(1)",
            "data:text/plain,secret",
            "blob:https://example.test/id",
            "about:blank",
            "https://user:password@example.test/",
        ):
            with self.subTest(url=url), self.assertRaises(ValueError):
                validate_public_http_url(url, resolver=public_dns)

    def test_rejects_every_non_global_literal_address(self):
        for url in (
            "http://127.0.0.1/",
            "http://10.0.0.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://0.0.0.0/",
            "http://224.0.0.1/",
            "http://[::1]/",
            "http://[fc00::1]/",
            "http://[fe80::1]/",
        ):
            with self.subTest(url=url), self.assertRaises(ValueError):
                validate_public_http_url(url, resolver=resolver_for("93.184.216.34"))

    def test_rejects_hostname_when_any_dns_answer_is_not_public(self):
        mixed_dns = resolver_for("93.184.216.34", "127.0.0.1")

        with self.assertRaises(ValueError):
            validate_public_http_url("https://example.test/", resolver=mixed_dns)

    def test_rejects_hostname_with_no_dns_answers(self):
        with self.assertRaises(ValueError):
            validate_public_http_url(
                "https://missing.example.test/", resolver=resolver_for()
            )

    def test_subresource_policy_allows_embedded_schemes_but_no_other_protocols(self):
        public_dns = resolver_for("93.184.216.34")

        for url in (
            "data:text/plain,hello",
            "blob:https://example.test/id",
            "about:blank",
        ):
            self.assertEqual(
                url,
                validate_browser_subresource_url(url, resolver=public_dns),
            )

        for url in (
            "file:///etc/passwd",
            "ftp://example.test/file",
            "ws://example.test/socket",
            "wss://example.test/socket",
        ):
            with self.subTest(url=url), self.assertRaises(ValueError):
                validate_browser_subresource_url(url, resolver=public_dns)

    def test_subresource_http_requests_use_the_same_dns_and_ip_checks(self):
        self.assertEqual(
            "https://cdn.example.test/app.js",
            validate_browser_subresource_url(
                "https://cdn.example.test/app.js",
                resolver=resolver_for("93.184.216.34"),
            ),
        )

        with self.assertRaises(ValueError):
            validate_browser_subresource_url(
                "http://169.254.169.254/latest/meta-data/",
                resolver=resolver_for("93.184.216.34"),
            )


class FakeRequest:
    def __init__(self, url):
        self.url = url


class FakeRoute:
    def __init__(self, url):
        self.request = FakeRequest(url)
        self.aborted = False
        self.continued = False

    async def abort(self):
        self.aborted = True

    async def continue_(self):
        self.continued = True


class BrowserRequestGuardTest(unittest.IsolatedAsyncioTestCase):
    def test_browser_script_disables_network_apis_that_bypass_http_routing(self):
        for api_name in (
            "WebSocket",
            "WebSocketStream",
            "EventSource",
            "Worker",
            "SharedWorker",
            "WebTransport",
            "RTCPeerConnection",
        ):
            with self.subTest(api_name=api_name):
                self.assertIn(api_name, _NETWORK_API_BLOCK_SCRIPT)

    async def test_main_navigation_is_rejected_before_playwright_is_needed(self):
        with self.assertRaises(ValueError):
            await load_url(
                "file:///etc/passwd",
                resolver=resolver_for("93.184.216.34"),
            )

    async def test_guard_continues_public_http_and_embedded_subresources(self):
        blocked_reasons = []
        public_route = FakeRoute("https://cdn.example.test/app.js")
        data_route = FakeRoute("data:text/plain,hello")

        await _guard_browser_request(
            public_route,
            resolver=resolver_for("93.184.216.34"),
            blocked_reasons=blocked_reasons,
        )
        await _guard_browser_request(
            data_route,
            resolver=resolver_for("93.184.216.34"),
            blocked_reasons=blocked_reasons,
        )

        self.assertTrue(public_route.continued)
        self.assertTrue(data_route.continued)
        self.assertFalse(blocked_reasons)

    async def test_guard_aborts_private_redirects_and_non_http_protocols(self):
        blocked_reasons = []
        private_route = FakeRoute("http://127.0.0.1/admin")
        file_route = FakeRoute("file:///etc/passwd")

        await _guard_browser_request(
            private_route,
            resolver=resolver_for("93.184.216.34"),
            blocked_reasons=blocked_reasons,
        )
        await _guard_browser_request(
            file_route,
            resolver=resolver_for("93.184.216.34"),
            blocked_reasons=blocked_reasons,
        )

        self.assertTrue(private_route.aborted)
        self.assertTrue(file_route.aborted)
        self.assertEqual(2, len(blocked_reasons))


if __name__ == "__main__":
    unittest.main()
