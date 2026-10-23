from typing import cast

import pytest
from effect_py import gen, run_async, scoped

from soulfire.bot_connect import BotServiceClient
from soulfire.bot_live_connect import BotLiveServiceClient
from soulfire.client import SoulFireInstance
from soulfire.common_pb2 import ProxyProto
from soulfire.instance_connect import InstanceServiceClient
from soulfire.instance_pb2 import (
    InstanceRemoveProxiesBatchRequest,
    InstanceRemoveProxiesBatchResponse,
    InstanceRemoveProxyEntriesRequest,
    InstanceRemoveProxyEntriesResponse,
)


class ProxyRemovalService:
    exact: InstanceRemoveProxyEntriesRequest | None = None
    addresses: InstanceRemoveProxiesBatchRequest | None = None

    async def remove_instance_proxy_entries(
        self, request: InstanceRemoveProxyEntriesRequest, **_kwargs: object
    ) -> InstanceRemoveProxyEntriesResponse:
        self.exact = request
        return InstanceRemoveProxyEntriesResponse()

    async def remove_instance_proxies_batch(
        self, request: InstanceRemoveProxiesBatchRequest, **_kwargs: object
    ) -> InstanceRemoveProxiesBatchResponse:
        self.addresses = request
        return InstanceRemoveProxiesBatchResponse()


@pytest.mark.asyncio
async def test_exact_removal_preserves_selectors_and_duplicate_counts() -> None:
    service = ProxyRemovalService()

    @gen
    def workflow():
        instance = SoulFireInstance(
            "instance-id",
            cast(BotServiceClient, object()),
            cast(BotLiveServiceClient, object()),
            cast(InstanceServiceClient, service),
        )
        proxy = ProxyProto(
            type=ProxyProto.SOCKS5,
            address="inet://127.0.0.1:1080",
            username="user",
            password="p:a-β",
        )
        yield from instance.remove_proxy_entries([proxy, proxy])
        yield from instance.remove_proxies([proxy.address])
        assert service.exact is not None
        assert service.exact.id == "instance-id"
        assert list(service.exact.proxies) == [proxy, proxy]
        assert service.addresses is not None
        assert list(service.addresses.addresses) == [proxy.address]

    await run_async(scoped(workflow).or_die())
