import { create } from "@bufbuild/protobuf";
import { createClient, createRouterTransport } from "@connectrpc/connect";
import { Effect } from "effect";
import { expect, it } from "vitest";
import { SoulFireInstance } from "../src/client.js";
import { BotLiveService } from "../src/generated/soulfire/bot_live_pb.js";
import { BotService } from "../src/generated/soulfire/bot_pb.js";
import { MinecraftAccountProtoSchema } from "../src/generated/soulfire/common_pb.js";
import {
  InstanceService,
  type InstanceReplaceAccountsRequest,
} from "../src/generated/soulfire/instance_pb.js";

it("sends complete and empty account replacements through the atomic RPC", () =>
  Effect.runPromise(
    Effect.gen(function* () {
      const requests: InstanceReplaceAccountsRequest[] = [];
      const transport = createRouterTransport(({ service }) => {
        service(InstanceService, {
          replaceInstanceAccounts(request) {
            requests.push(request);
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
      const account = create(MinecraftAccountProtoSchema, {
        profileId: "042fb57f-64c9-3bb8-9c47-703af94a3751",
        lastKnownName: "AccountQA_1",
        accountData: { case: "offlineJavaData", value: {} },
      });
      yield* instance.replaceAccounts([account]);
      yield* instance.replaceAccounts([]);
      expect(requests.map((r) => ({ id: r.id, accounts: r.accounts }))).toEqual([
        { id: instance.id, accounts: [account] },
        { id: instance.id, accounts: [] },
      ]);
    }),
  ));
