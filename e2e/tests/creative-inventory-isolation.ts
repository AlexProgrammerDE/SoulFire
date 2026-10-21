import { randomUUID } from "node:crypto";
import type { MessageInitShape } from "@bufbuild/protobuf";
import type { SoulFireBot, SoulFireClient } from "@soulfiremc/sdk";
import type { PovFrame, PovInputEventSchema } from "@soulfiremc/sdk/generated/soulfire/pov_pb";
import { PovInputEvent_Kind as Kind, PovService } from "@soulfiremc/sdk/generated/soulfire/pov_pb";
import { Effect } from "effect";
import {
  BOT_NAME,
  CheckFailed,
  check,
  type E2ETest,
  eventually,
  placeBot,
  rcon,
} from "../harness.ts";

const OTHER = "E2ECreative2";

// Each native screen uses GUI scale 3 and logical dimensions 427x240.
const native = (
  client: SoulFireClient,
  bot: SoulFireBot,
  action: (input: {
    click: (x: number, y: number) => Promise<unknown>;
    type: (text: string) => Promise<unknown>;
    escape: () => Promise<unknown>;
  }) => Promise<void>,
) =>
  Effect.tryPromise({
    try: async () => {
      const service = client.service(PovService);
      const sessionId = randomUUID();
      const abort = new AbortController();
      let latest: PovFrame | undefined;
      let streamError: unknown;
      let sequence = 0n;
      let pending: Promise<unknown> = Promise.resolve();
      const watching = (async () => {
        try {
          for await (const frame of service.watch(
            {
              instanceId: bot.instanceId,
              botId: bot.id,
              sessionId,
              width: 1280,
              height: 720,
              maxFps: 15,
            },
            { signal: AbortSignal.any([abort.signal, AbortSignal.timeout(15_000)]) },
          ))
            latest = frame;
        } catch (error) {
          if (!abort.signal.aborted) streamError = error;
        }
      })();
      const send = (
        events: MessageInitShape<typeof PovInputEventSchema>[] = [],
        escape = false,
      ) => {
        const sent = pending.then(() =>
          service.input({
            sessionId,
            inputToken: latest!.inputToken,
            sequence: ++sequence,
            captured: true,
            width: 1280,
            height: 720,
            events,
            escape,
          }),
        );
        pending = sent;
        return sent;
      };
      const heartbeat = setInterval(() => {
        if (latest)
          void send().catch((error) => {
            streamError = error;
          });
      }, 500);
      try {
        const deadline = Date.now() + 10_000;
        while (!latest?.screenOpen) {
          if (streamError) throw streamError;
          if (Date.now() > deadline)
            throw new Error("The native creative inventory did not render");
          await new Promise((resolve) => setTimeout(resolve, 25));
        }
        await action({
          click: (x, y) =>
            send([
              { kind: Kind.MOVE, x: x / 427, y: y / 240 },
              { kind: Kind.BUTTON, code: 0, action: 1 },
              { kind: Kind.BUTTON, code: 0, action: 0 },
            ]),
          type: (text) =>
            send(
              Array.from(text, (character) => ({
                kind: Kind.CHARACTER,
                code: character.codePointAt(0)!,
              })),
            ),
          escape: async () => {
            await send([], true);
            const deadline = Date.now() + 5000;
            while (latest?.screenOpen !== false) {
              if (streamError) throw streamError;
              if (Date.now() > deadline) throw new Error("Escape did not return to the game view");
              await new Promise((resolve) => setTimeout(resolve, 25));
            }
          },
        });
        if (streamError) throw streamError;
      } finally {
        clearInterval(heartbeat);
        abort.abort();
        await watching;
        await pending.catch(() => {});
      }
    },
    catch: (cause) =>
      new CheckFailed({ message: `Native creative input failed: ${String(cause)}` }),
  });

export default {
  name: "creative-inventory-isolation",
  run: ({ bot, client }) =>
    Effect.gen(function* () {
      const run = Effect.runPromiseWith(yield* Effect.context());
      const instance = client.instance(bot.instanceId);
      let otherId: string | undefined;
      yield* Effect.addFinalizer(() =>
        otherId === undefined
          ? Effect.void
          : instance.removeAccounts([otherId]).pipe(Effect.ignore),
      );
      const other = yield* instance.getOrCreateBot(OTHER, { readyTimeoutMs: 30_000 });
      otherId = other.id;
      yield* rcon("forceload add 1728 -16 1760 16");
      yield* rcon("fill 1742 63 -2 1748 63 2 stone");
      yield* rcon("fill 1742 64 -2 1748 67 2 air");
      yield* placeBot(bot, { x: 1744.5, y: 64, z: 0.5 });
      yield* rcon(`clear ${OTHER}`, { check: false });
      yield* rcon(`tp ${OTHER} 1746.5 64 0.5`);
      yield* rcon(`gamemode creative ${BOT_NAME}`);
      yield* rcon(`gamemode creative ${OTHER}`);
      const firstItem = (target: SoulFireBot, itemId: string) =>
        eventually(
          target.inventoryState().pipe(
            Effect.flatMap((state) =>
              check(
                state.slots.some((slot) => slot.slot === 0 && slot.itemId === itemId),
                "The creative search results changed or did not load",
              ),
            ),
          ),
          "5 seconds",
          "100 millis",
        );
      yield* bot.openInventory();
      yield* native(client, bot, async (input) => {
        await input.click(298, 36);
        await input.type("diamond sword");
        await run(firstItem(bot, "minecraft:diamond_sword"));
      });
      yield* other.openInventory();
      yield* native(client, other, async (input) => {
        await input.click(298, 36);
        await input.type("nether star");
        await run(firstItem(other, "minecraft:nether_star"));
        await input.click(298, 204);
      });
      yield* firstItem(bot, "minecraft:diamond_sword");
      yield* native(client, bot, async (input) => {
        await input.click(133, 78);
        await input.click(133, 172);
        await input.escape();
      });
      yield* eventually(
        rcon(`data get entity ${BOT_NAME} Inventory`).pipe(
          Effect.flatMap((data) =>
            check(
              data.includes("minecraft:diamond_sword") && !data.includes("minecraft:nether_star"),
              "Another bot's creative search changed the item picked by the first bot",
            ),
          ),
        ),
        "5 seconds",
      );
      yield* native(client, other, async (input) => {
        await input.click(298, 36);
        await input.type("nether star");
        await run(firstItem(other, "minecraft:nether_star"));
        await input.click(133, 78);
        await input.click(133, 172);
        await input.escape();
      });
      yield* eventually(
        rcon(`data get entity ${OTHER} Inventory`).pipe(
          Effect.flatMap((data) =>
            check(
              data.includes("minecraft:nether_star") && !data.includes("minecraft:diamond_sword"),
              "The second bot did not retain its own creative selection",
            ),
          ),
        ),
        "5 seconds",
      );
    }).pipe(
      Effect.ensuring(bot.closeContainer().pipe(Effect.ignore)),
      Effect.ensuring(rcon(`gamemode survival ${BOT_NAME}`).pipe(Effect.ignore)),
      Effect.scoped,
    ),
} satisfies E2ETest;
