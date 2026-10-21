import { BotService } from "@soulfiremc/sdk/generated/soulfire/bot_pb";
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

const NAME = "Anvil QA sword";
const ANVIL = { x: 301, y: 64, z: 0 };

export default {
  name: "anvil-rename",
  run: ({ bot, client }) =>
    Effect.gen(function* () {
      yield* rcon("forceload add 288 -16 320 16");
      yield* rcon("fill 298 63 -2 303 63 2 minecraft:stone");
      yield* rcon("fill 298 64 -2 303 67 2 minecraft:air");
      yield* rcon(`setblock ${ANVIL.x} ${ANVIL.y} ${ANVIL.z} minecraft:anvil`);
      yield* placeBot(bot, { x: 300.5, y: 64, z: 0.5 }, -90);
      yield* rcon(`experience set ${BOT_NAME} 5 levels`);
      yield* rcon(`give ${BOT_NAME} minecraft:iron_sword 1`);
      yield* bot.inventory.open(ANVIL);

      const sword = yield* eventually(
        bot.inventoryState().pipe(
          Effect.map((state) => state.slots.find((slot) => slot.itemId === "minecraft:iron_sword")),
          Effect.filterOrFail(
            (slot) => slot !== undefined,
            () => new CheckFailed({ message: "The sword never reached the open anvil menu" }),
          ),
        ),
        "5 seconds",
      );
      yield* bot.clickInventory(sword.slot);
      yield* bot.clickInventory(0);
      yield* eventually(
        bot.inventoryState().pipe(
          Effect.flatMap((state) =>
            check(
              state.slots.some((slot) => slot.slot === 0 && slot.itemId === "minecraft:iron_sword"),
              "The sword never reached the anvil input",
            ),
          ),
        ),
        "5 seconds",
      );

      const response = yield* Effect.tryPromise({
        try: () =>
          client.service(BotService).setContainerText({
            instanceId: bot.instanceId,
            botId: bot.id,
            fieldId: "item_name",
            text: NAME,
          }),
        catch: (cause) => new CheckFailed({ message: `Rename request failed: ${String(cause)}` }),
      });
      yield* check(response.success, response.error ?? "The rename request was rejected");
      yield* eventually(
        bot.inventoryState().pipe(
          Effect.flatMap((state) =>
            check(
              state.slots.some((slot) => slot.slot === 2 && slot.displayName === NAME),
              "The rename was accepted but no renamed anvil output arrived",
            ),
          ),
        ),
        "5 seconds",
      );
      yield* bot.transferInventorySlot(2);
      yield* eventually(
        rcon(`data get entity ${BOT_NAME} Inventory`).pipe(
          Effect.flatMap((inventory) =>
            check(
              inventory.includes(NAME),
              "Vanilla did not retain the renamed sword after taking the output",
            ),
          ),
        ),
        "5 seconds",
      );
    }).pipe(Effect.ensuring(bot.closeContainer().pipe(Effect.ignore))),
} satisfies E2ETest;
