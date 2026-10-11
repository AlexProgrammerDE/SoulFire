import { create } from "@bufbuild/protobuf";
import { Code, ConnectError, createRouterTransport } from "@connectrpc/connect";
import { Effect } from "effect";
import { describe, expect, it, vi } from "vitest";
import { SoulFire as NodeSoulFire } from "../src/node.js";
import { SoulFire as BunSoulFire } from "../src/bun.js";
import * as LocalServer from "../src/local-server.js";
import { SoulFire } from "../src/client.js";
import { BotLiveService } from "../src/generated/soulfire/bot_live_pb.js";
import { BotDesiredState, BotService, BotStatusSchema } from "../src/generated/soulfire/bot_pb.js";
import { MinecraftAccountProto_AccountTypeProto } from "../src/generated/soulfire/common_pb.js";
import {
  BotAuthentication,
  InstanceService,
  type InstanceGetOrCreateBotRequest,
  type InstanceGetOrCreateRequest,
} from "../src/generated/soulfire/instance_pb.js";
import { MCAuthService } from "../src/generated/soulfire/mc-auth_pb.js";
import { SdkService } from "../src/generated/soulfire/sdk_pb.js";

function server(options: { running?: boolean; snapshot?: boolean } = {}) {
  const instanceRequests: InstanceGetOrCreateRequest[] = [];
  const botRequests: InstanceGetOrCreateBotRequest[] = [];
  const states: BotDesiredState[] = [];
  const status = create(BotStatusSchema, {
    profileId: "bot-id",
    desiredState: options.running ? BotDesiredState.RUNNING : BotDesiredState.STOPPED,
  });
  let accounts = 0;
  let logins = 0;
  let subscriptions = 0;
  const transport = createRouterTransport(({ service }) => {
    service(SdkService, {
      handshake: () => ({
        identity: { id: "user-id" },
        apiVersion: { major: 1, minor: 0, patch: 0 },
        capabilities: [{ id: "instance.provisioning.v1", revision: 1 }],
      }),
    });
    service(InstanceService, {
      getOrCreateInstance: (request) => {
        instanceRequests.push(request);
        return { id: "instance-id" };
      },
      getOrCreateBot: (request) => {
        botRequests.push(request);
        if (
          request.auth === BotAuthentication.MICROSOFT &&
          accounts === 0 &&
          request.account === undefined
        )
          throw new ConnectError("authenticate", Code.NotFound);
        accounts = 1;
        return { botId: "bot-id" };
      },
    });
    service(BotService, {
      getBotInfo: () => ({ status }),
      setBotsDesiredState: (request) => {
        states.push(request.desiredState);
        status.desiredState = request.desiredState;
        return { bots: [status] };
      },
    });
    service(BotLiveService, {
      async *watchBotEvents(_, context) {
        subscriptions += 1;
        let active = true;
        const release = () => {
          if (active) {
            active = false;
            subscriptions -= 1;
          }
        };
        context.signal.addEventListener("abort", release, { once: true });
        try {
          yield { event: { case: "status", value: status } };
          if (options.snapshot !== false)
            yield { event: { case: "snapshot", value: { health: 20 } } };
          await new Promise<void>((resolve) => {
            if (context.signal.aborted) resolve();
            else context.signal.addEventListener("abort", () => resolve(), { once: true });
          });
        } finally {
          release();
        }
      },
    });
    service(MCAuthService, {
      async *loginDeviceCode() {
        logins += 1;
        yield {
          data: {
            case: "deviceCode",
            value: { verificationUri: "https://example.com/login", userCode: "123" },
          },
        };
        yield {
          data: {
            case: "account",
            value: {
              type: MinecraftAccountProto_AccountTypeProto.MICROSOFT_JAVA_DEVICE_CODE,
              profileId: "profile-id",
              lastKnownName: "Player",
              accountData: { case: "onlineChainJavaData", value: {} },
            },
          },
        };
      },
    });
  });
  return {
    connect: SoulFire.connect({ baseUrl: "http://localhost", transport }),
    instanceRequests,
    botRequests,
    states,
    get logins() {
      return logins;
    },
    get subscriptions() {
      return subscriptions;
    },
  };
}

describe("named bot setup", () => {
  it.each([NodeSoulFire, BunSoulFire])(
    "installs automatically and releases the managed server",
    async (entry) => {
      const fixture = server();
      let closed = false;
      const close = Effect.sync(() => {
        closed = true;
      });
      const controller: LocalServer.LocalServerHandle = {
        info: {
          baseUrl: "http://localhost",
          directory: ".soulfire",
          jarPath: "server.jar",
          javaPath: "java",
          pid: 1,
          runDirectory: ".soulfire/server",
          version: "test",
        },
        token: "token",
        isRunning: () => !closed,
        logs: () => [],
        restart: () => Effect.void,
        stop: () => close,
        close: () => close,
      };
      const install = vi
        .spyOn(LocalServer, "installLocalServer")
        .mockReturnValue(
          Effect.acquireRelease(Effect.succeed(controller), (handle) => handle.close()),
        );
      const connect = vi.spyOn(SoulFire, "connectManaged").mockReturnValue(fixture.connect);
      try {
        await Effect.runPromise(
          Effect.scoped(
            Effect.gen(function* () {
              const bot = yield* entry.createBot({
                server: "localhost:25565",
                username: "Builder",
              });
              expect(bot.state.player?.health).toBe(20);
              yield* bot.connect();
              expect(fixture.subscriptions).toBe(1);
            }),
          ),
        );
        expect(install).toHaveBeenCalledOnce();
        expect(fixture.states).toEqual([BotDesiredState.RUNNING, BotDesiredState.STOPPED]);
        expect(closed).toBe(true);
      } finally {
        install.mockRestore();
        connect.mockRestore();
      }
    },
  );

  it("configures an offline bot and owns its start and observation", async () => {
    const fixture = server();
    await Effect.runPromise(
      Effect.scoped(
        Effect.gen(function* () {
          const client = yield* fixture.connect;
          const bot = yield* client.createBot({ server: "localhost:25565", username: "Builder" });
          expect(bot.state.player?.health).toBe(20);
          expect(fixture.subscriptions).toBe(1);
          expect(yield* bot.observe()).toBe(yield* bot.observe());
          expect(fixture.instanceRequests[0]).toMatchObject({
            name: "localhost:25565",
            server: "localhost:25565",
          });
          expect(fixture.botRequests[0]).toMatchObject({
            name: "Builder",
            username: "Builder",
            auth: BotAuthentication.OFFLINE,
          });
        }),
      ),
    );
    expect(fixture.states).toEqual([BotDesiredState.RUNNING, BotDesiredState.STOPPED]);
    expect(fixture.subscriptions).toBe(0);
  });

  it("keeps an already running bot running after scope exit", async () => {
    const fixture = server({ running: true });
    await Effect.runPromise(
      Effect.scoped(
        Effect.gen(function* () {
          const client = yield* fixture.connect;
          yield* client.createBot({ server: "localhost:25565", username: "Builder" });
        }),
      ),
    );
    expect(fixture.states).toEqual([]);
    expect(fixture.subscriptions).toBe(0);
  });

  it("can provision accounts without connecting them", async () => {
    const fixture = server();
    await Effect.runPromise(
      Effect.scoped(
        Effect.gen(function* () {
          const client = yield* fixture.connect;
          const instance = yield* client.getOrCreateInstance("farm");
          const bots = yield* Effect.all(
            [
              instance.getOrCreateBot("Builder", { start: false }),
              instance.getOrCreateBot("Builder", { start: false }),
            ],
            { concurrency: 2 },
          );
          expect(bots[0].id).toBe(bots[1].id);
        }),
      ),
    );
    expect(fixture.states).toEqual([]);
    expect(fixture.subscriptions).toBe(0);
  });

  it("times out missing initial state and releases the started bot", async () => {
    const fixture = server({ snapshot: false });
    const error = await Effect.runPromise(
      Effect.scoped(
        Effect.gen(function* () {
          const client = yield* fixture.connect;
          return yield* client.createBot({
            server: "localhost:25565",
            username: "Builder",
            readyTimeoutMs: 20,
          });
        }),
      ).pipe(Effect.flip),
    );
    expect(error._tag).toBe("SoulFireTimeoutError");
    expect(fixture.states).toEqual([BotDesiredState.RUNNING, BotDesiredState.STOPPED]);
    expect(fixture.subscriptions).toBe(0);
  });

  it("authenticates new Microsoft accounts once and reuses the saved account", async () => {
    const fixture = server();
    let codes = 0;
    await Effect.runPromise(
      Effect.scoped(
        Effect.gen(function* () {
          const client = yield* fixture.connect;
          const instance = yield* client.getOrCreateInstance("farm");
          const options = {
            auth: "microsoft",
            start: false,
            onDeviceCode: () =>
              Effect.sync(() => {
                codes += 1;
              }),
          } as const;
          yield* instance.getOrCreateBot("main-account", options);
          yield* instance.getOrCreateBot("main-account", options);
        }),
      ),
    );
    expect(codes).toBe(1);
    expect(fixture.logins).toBe(1);
    expect(fixture.botRequests[1]?.account?.lastKnownName).toBe("Player");
  });
});
