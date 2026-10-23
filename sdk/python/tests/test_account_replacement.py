from typing import cast

import pytest
from effect_py import gen, run_async, scoped

from soulfire.bot_connect import BotServiceClient
from soulfire.bot_live_connect import BotLiveServiceClient
from soulfire.client import SoulFireInstance
from soulfire.common_pb2 import MinecraftAccountProto
from soulfire.instance_connect import InstanceServiceClient
from soulfire.instance_pb2 import InstanceReplaceAccountsRequest, InstanceReplaceAccountsResponse


class ReplacementService:
    def __init__(self) -> None:
        self.requests: list[InstanceReplaceAccountsRequest] = []

    async def replace_instance_accounts(
        self, request: InstanceReplaceAccountsRequest, **_kwargs: object
    ) -> InstanceReplaceAccountsResponse:
        self.requests.append(request)
        return InstanceReplaceAccountsResponse()


@pytest.mark.asyncio
async def test_complete_and_empty_replacement_use_the_atomic_rpc() -> None:
    service = ReplacementService()

    @gen
    def workflow():
        instance = SoulFireInstance(
            "instance-id",
            cast(BotServiceClient, object()),
            cast(BotLiveServiceClient, object()),
            cast(InstanceServiceClient, service),
        )
        account = MinecraftAccountProto(
            profile_id="042fb57f-64c9-3bb8-9c47-703af94a3751",
            last_known_name="AccountQA_1",
            offline_java_data=MinecraftAccountProto.OfflineJavaData(),
        )
        yield from instance.replace_accounts([account])
        yield from instance.replace_accounts([])
        assert [(r.id, list(r.accounts)) for r in service.requests] == [
            (instance.id, [account]),
            (instance.id, []),
        ]

    await run_async(scoped(workflow).or_die())
