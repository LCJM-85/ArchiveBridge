"""知识库网页导入的 URL 与目标地址安全校验。"""

from __future__ import annotations

import ipaddress
import socket
from collections.abc import Callable, Iterable
from urllib.parse import SplitResult, urlsplit


Resolver = Callable[..., Iterable[tuple]]
_HTTP_SCHEMES = frozenset(("http", "https"))
_LOCAL_SUBRESOURCE_SCHEMES = frozenset(("data", "blob", "about"))


def _split_http_url(url: str) -> tuple[str, SplitResult]:
    if not isinstance(url, str) or not url:
        raise ValueError("URL 不能为空")
    if url != url.strip() or any(ord(char) < 32 for char in url):
        raise ValueError("URL 包含非法空白或控制字符")

    try:
        parts = urlsplit(url)
        port = parts.port
    except ValueError as exc:
        raise ValueError("URL 格式无效") from exc

    if parts.scheme.lower() not in _HTTP_SCHEMES:
        raise ValueError("仅允许导入 HTTP 或 HTTPS 网页")
    if parts.username is not None or parts.password is not None:
        raise ValueError("URL 不允许包含用户凭据")
    if not parts.hostname:
        raise ValueError("URL 缺少有效主机名")
    if "%" in parts.hostname:
        raise ValueError("URL 不允许使用带作用域的地址")

    # 读取一次端口即可触发 urllib 对越界端口的校验。
    _ = port
    return url, parts


def _require_global_address(address: str) -> None:
    try:
        parsed = ipaddress.ip_address(address)
    except ValueError as exc:
        raise ValueError("DNS 返回了无效地址") from exc
    if not parsed.is_global or parsed.is_multicast:
        raise ValueError("禁止访问回环、私网、链路本地、保留或其他非公网地址")


def validate_public_http_url(
    url: str,
    *,
    resolver: Resolver = socket.getaddrinfo,
) -> str:
    """仅接受所有解析结果均为全局公网地址的 HTTP(S) URL。"""

    original_url, parts = _split_http_url(url)
    hostname = parts.hostname
    assert hostname is not None

    try:
        literal_address = ipaddress.ip_address(hostname)
    except ValueError:
        try:
            ascii_hostname = hostname.encode("idna").decode("ascii")
        except UnicodeError as exc:
            raise ValueError("URL 主机名无效") from exc

        port = parts.port or (443 if parts.scheme.lower() == "https" else 80)
        try:
            answers = list(resolver(ascii_hostname, port, type=socket.SOCK_STREAM))
        except (OSError, UnicodeError, ValueError) as exc:
            raise ValueError("URL 主机名解析失败") from exc
        if not answers:
            raise ValueError("URL 主机名没有可用地址")

        for answer in answers:
            try:
                address = answer[4][0]
            except (IndexError, TypeError) as exc:
                raise ValueError("DNS 返回了无效地址") from exc
            _require_global_address(address)
    else:
        if not literal_address.is_global or literal_address.is_multicast:
            raise ValueError("禁止访问回环、私网、链路本地、保留或其他非公网地址")

    return original_url


def validate_browser_subresource_url(
    url: str,
    *,
    resolver: Resolver = socket.getaddrinfo,
) -> str:
    """校验浏览器子请求；仅内嵌协议可免于公网目标解析。"""

    if not isinstance(url, str) or not url:
        raise ValueError("子请求 URL 不能为空")
    scheme = urlsplit(url).scheme.lower()
    if scheme in _LOCAL_SUBRESOURCE_SCHEMES:
        return url
    return validate_public_http_url(url, resolver=resolver)
