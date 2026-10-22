import asyncio
from collections.abc import AsyncIterator
from typing import Any, cast

import pytest
from connectrpc.code import Code
from connectrpc.errors import ConnectError
from effect_py import acquire_release, gen, run_async, scoped, sync

from soulfire import SoulFire, SoulFireRpcError, SoulFireTimeoutError
from soulfire.bot_connect import BotServiceClient
from soulfire.bot_live_connect import BotLiveServiceClient
from soulfire.bot_live_pb2 import BotEvent, WatchBotEventsRequest
from soulfire.bot_pb2 import (
    BOT_DESIRED_STATE_RUNNING,
    BOT_DESIRED_STATE_STOPPED,
    BotInfoResponse,
    BotLiveState,
    BotStatus,
    SetBotsDesiredStateRequest,
    SetBotsDesiredStateResponse,
)
from soulfire.common_pb2 import MinecraftAccountProto
from soulfire.connection import ConnectionMetadata
from soulfire.instance_connect import InstanceServiceClient
from soulfire.instance_pb2 import (
    BOT_AUTHENTICATION_MICROSOFT,
    BOT_AUTHENTICATION_OFFLINE,
    InstanceDeleteRequest,
    InstanceDeleteResponse,
    InstanceGetOrCreateBotRequest,
    InstanceGetOrCreateBotResponse,
    InstanceGetOrCreateRequest,
    InstanceGetOrCreateResponse,
)
from soulfire.mc_auth_connect import MCAuthServiceClient
from soulfire.mc_auth_pb2 import DeviceCode, DeviceCodeAuthResponse
from soulfire.sdk_pb2 import SdkApiVersion, SdkCapability, SdkHandshakeResponse, SdkIdentity


class Server:
    def __init__(
        self, *, running: bool = False, snapshot: bool = True, cleanup_error: Code = Code.NOT_FOUND
    ) -> None:
        self.status = BotStatus(
            profile_id="bot-id",
            desired_state=BOT_DESIRED_STATE_RUNNING if running else BOT_DESIRED_STATE_STOPPED,
        )
        self.snapshot = snapshot
        self.instance_requests: list[InstanceGetOrCreateRequest] = []
        self.bot_requests: list[InstanceGetOrCreateBotRequest] = []
        self.states: list[int] = []
        self.logins = 0
        self.accounts = 0
        self.subscriptions = 0
        self.closed = False
        self.deleted = False
        self.cleanup_error = cleanup_error

    async def delete_instance(
        self, _: InstanceDeleteRequest, **_kwargs: object
    ) -> InstanceDeleteResponse:
        self.deleted = True
        return InstanceDeleteResponse()

    async def get_or_create_instance(
        self, request: InstanceGetOrCreateRequest, **_: object
    ) -> InstanceGetOrCreateResponse:
        self.instance_requests.append(request)
        return InstanceGetOrCreateResponse(id="instance-id")

    async def get_or_create_bot(
        self, request: InstanceGetOrCreateBotRequest, **_: object
    ) -> InstanceGetOrCreateBotResponse:
        copy = InstanceGetOrCreateBotRequest()
        copy.CopyFrom(request)
        self.bot_requests.append(copy)
        if (
            request.auth == BOT_AUTHENTICATION_MICROSOFT
            and not self.accounts
            and not request.HasField("account")
        ):
            raise ConnectError(Code.NOT_FOUND, "authenticate")
        self.accounts = 1
        return InstanceGetOrCreateBotResponse(bot_id="bot-id")

    async def get_bot_info(self, *_args: object, **_kwargs: object) -> BotInfoResponse:
        return BotInfoResponse(status=self.status)

    async def set_bots_desired_state(
        self, request: SetBotsDesiredStateRequest, **_: object
    ) -> SetBotsDesiredStateResponse:
        self.states.append(request.desired_state)
        if self.deleted:
            raise ConnectError(self.cleanup_error, "Cleanup rejected")
        self.status.desired_state = request.desired_state
        return SetBotsDesiredStateResponse(bots=[self.status])

    async def watch_bot_events(
        self, _: WatchBotEventsRequest, **_kwargs: object
    ) -> AsyncIterator[BotEvent]:
        self.subscriptions += 1
        try:
            yield BotEvent(status=self.status)
            if self.snapshot:
                yield BotEvent(snapshot=BotLiveState(health=20))
            await asyncio.Event().wait()
        finally:
            self.subscriptions -= 1

    async def login_device_code(
        self, *_args: object, **_kwargs: object
    ) -> AsyncIterator[DeviceCodeAuthResponse]:
        self.logins += 1
        yield DeviceCodeAuthResponse(
            device_code=DeviceCode(verification_uri="https://example.com", user_code="123")
        )
        yield DeviceCodeAuthResponse(
            account=MinecraftAccountProto(
                type=MinecraftAccountProto.MICROSOFT_JAVA_DEVICE_CODE,
                profile_id="profile-id",
                last_known_name="Player",
                online_chain_java_data=MinecraftAccountProto.OnlineChainJavaData(),
            )
        )

    def client(self) -> SoulFire:
        client = SoulFire("http://localhost")
        client.instance_service = cast(InstanceServiceClient, self)
        client.bot_service = cast(BotServiceClient, self)
        client.bot_live = cast(BotLiveServiceClient, self)
        client.mc_auth_service = cast(MCAuthServiceClient, self)
        client._connection = ConnectionMetadata.from_response(
            SdkHandshakeResponse(
                api_version=SdkApiVersion(major=1),
                identity=SdkIdentity(id="user-id"),
                capabilities=[SdkCapability(id="instance.provisioning.v1", revision=1)],
            )
        )
        return client


async def test_managed_offline_bot_owns_its_start_and_observation(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    server = Server()
    client = server.client()

    def release() -> None:
        server.closed = True

    def install(_cls: type[SoulFire], **_options: Any):
        return acquire_release(sync(lambda: client), lambda _, _exit: sync(release))

    monkeypatch.setattr(SoulFire, "install", classmethod(install))

    @gen
    def program():
        bot = yield from SoulFire.create_bot(server="localhost:25565", username="Builder")
        assert bot.state.player is not None
        assert bot.state.player.health == 20
        assert server.subscriptions == 1
        assert (yield from bot.observe()) is (yield from bot.observe())

    await run_async(scoped(program).or_die())
    assert server.instance_requests[0].name == "localhost:25565"
    assert server.bot_requests[0].auth == BOT_AUTHENTICATION_OFFLINE
    assert server.states == [BOT_DESIRED_STATE_RUNNING, BOT_DESIRED_STATE_STOPPED]
    assert server.subscriptions == 0
    assert server.closed


async def test_existing_running_bot_keeps_running() -> None:
    server = Server(running=True)
    client = server.client()

    @gen
    def program():
        instance = yield from client.get_or_create_instance("farm")
        yield from instance.get_or_create_bot("Builder")

    await run_async(scoped(program).or_die())
    assert not server.states
    assert server.subscriptions == 0


@pytest.mark.parametrize("code", [Code.NOT_FOUND, Code.PERMISSION_DENIED, Code.UNAVAILABLE])
async def test_managed_bot_cleanup_after_instance_deletion(code: Code) -> None:
    server = Server(cleanup_error=code)
    client = server.client()

    @gen
    def program():
        instance = yield from client.get_or_create_instance("farm")
        bot = yield from instance.get_or_create_bot("Builder")
        yield from instance.delete()
        yield from bot.stop()

    # Explicit stop still reports a missing bot; only managed cleanup is idempotent.
    with pytest.raises(SoulFireRpcError) as caught:
        await run_async(scoped(program).or_die())
    assert caught.value.code == code
    assert server.subscriptions == 0

    server = Server(cleanup_error=code)
    client = server.client()

    @gen
    def delete_connected_instance():
        instance = yield from client.get_or_create_instance("farm")
        yield from instance.get_or_create_bot("Builder")
        yield from instance.delete()

    if code == Code.NOT_FOUND:
        await run_async(scoped(delete_connected_instance).or_die())
    else:
        with pytest.raises(SoulFireRpcError) as cleanup_failure:
            await run_async(scoped(delete_connected_instance).or_die())
        assert cleanup_failure.value.code == code
    assert server.states == [BOT_DESIRED_STATE_RUNNING, BOT_DESIRED_STATE_STOPPED]
    assert server.subscriptions == 0


async def test_missing_snapshot_times_out_and_releases_bot() -> None:
    server = Server(snapshot=False)
    client = server.client()

    @gen
    def program():
        instance = yield from client.get_or_create_instance("farm")
        yield from instance.get_or_create_bot("Builder", ready_timeout=0.02)

    with pytest.raises(SoulFireTimeoutError):
        await run_async(scoped(program).or_die())
    assert server.states == [BOT_DESIRED_STATE_RUNNING, BOT_DESIRED_STATE_STOPPED]
    assert server.subscriptions == 0


async def test_microsoft_authentication_runs_only_for_new_accounts() -> None:
    server = Server()
    client = server.client()
    codes: list[DeviceCode] = []

    @gen
    def program():
        instance = yield from client.get_or_create_instance("farm")
        for _ in range(2):
            yield from instance.get_or_create_bot(
                "main-account",
                auth="microsoft",
                start=False,
                on_device_code=lambda code: sync(lambda: codes.append(code)),
            )

    await run_async(scoped(program).or_die())
    assert len(codes) == 1
    assert server.logins == 1
    assert server.bot_requests[1].account.last_known_name == "Player"
    assert not server.states
