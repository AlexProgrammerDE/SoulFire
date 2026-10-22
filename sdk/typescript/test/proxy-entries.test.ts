import { create } from "@bufbuild/protobuf";
import { createClient, createRouterTransport } from "@connectrpc/connect";
import { Effect } from "effect";
import { expect, it } from "vitest";
import { SoulFireInstance } from "../src/client.js";
import { BotLiveService } from "../src/generated/soulfire/bot_live_pb.js";
import { BotService } from "../src/generated/soulfire/bot_pb.js";
import { ProxyProto_Type, ProxyProtoSchema } from "../src/generated/soulfire/common_pb.js";
import {
  InstanceService,
  type InstanceRemoveProxiesBatchRequest,
  type InstanceRemoveProxyEntriesRequest,
} from "../src/generated/soulfire/instance_pb.js";

it("preserves exact proxy selectors and duplicate counts without changing address removal", () =>
  Effect.runPromise(
    Effect.gen(function* () {
      let exact: InstanceRemoveProxyEntriesRequest | undefined;
      let addresses: InstanceRemoveProxiesBatchRequest | undefined;
      const transport = createRouterTransport(({ service }) => {
        service(InstanceService, {
          removeInstanceProxyEntries(request) {
            exact = request;
            return {};
          },
          removeInstanceProxiesBatch(request) {
            addresses = request;
            return {};
          },
        });
      });
      const instance = new SoulFireInstance(
        "instance-id",
        createClient(BotService, transport),
        createClient(BotLiveService, transport),
        createClient(InstanceService, transport),
      );
      const proxy = create(ProxyProtoSchema, {
        type: ProxyProto_Type.SOCKS5,
        address: "inet://127.0.0.1:1080",
        username: "user",
        password: "p:a-β",
      });

      yield* instance.removeProxyEntries([proxy, proxy]);
      yield* instance.removeProxies([proxy.address]);

      expect(exact?.id).toBe("instance-id");
      expect(exact?.proxies).toEqual([proxy, proxy]);
      expect(addresses?.addresses).toEqual([proxy.address]);
    }),
  ));
