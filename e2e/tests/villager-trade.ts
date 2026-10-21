import { Hand } from "@soulfiremc/sdk/generated/soulfire/bot_live_pb";
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

const TAG = "sf_e2e_trade";

export default {
  name: "villager-trade",
  run: ({ bot, client }) =>
    Effect.gen(function* () {
      yield* rcon(`kill @e[type=minecraft:villager,tag=${TAG}]`, { check: false });
      yield* rcon("forceload add 480 -16 528 16");
      yield* rcon("fill 498 63 -2 503 63 2 minecraft:stone");
      yield* rcon("fill 498 64 -2 503 67 2 minecraft:air");
      yield* placeBot(bot, { x: 500.5, y: 64, z: 0.5 }, -90);
      yield* rcon(
        `summon minecraft:villager 501.5 64 0.5 {Tags:["${TAG}"],NoAI:1b,Invulnerable:1b,VillagerData:{profession:"minecraft:farmer",level:2,type:"minecraft:plains"},Offers:{Recipes:[{buy:{id:"minecraft:emerald",count:1},sell:{id:"minecraft:bread",count:1},maxUses:999},{buy:{id:"minecraft:emerald",count:1},sell:{id:"minecraft:apple",count:2},maxUses:999}]}}`,
      );
      yield* rcon(`give ${BOT_NAME} minecraft:emerald 3`);
      yield* eventually(
        bot.inventory
          .count({ selector: { itemIds: ["minecraft:emerald"] } })
          .pipe(
            Effect.flatMap((count) => check(count === 3n, "The emeralds never reached the bot")),
          ),
        "5 seconds",
      );
      const target = yield* eventually(
        bot.world
          .queryEntities({ radius: 4, selector: { entityTypes: ["minecraft:villager"] } })
          .pipe(
            Effect.map((result) => result.entities[0]?.reference),
            Effect.filterOrFail(
              (reference) => reference !== undefined,
              () => new CheckFailed({ message: "The villager never became observable" }),
            ),
          ),
        "5 seconds",
      );
      yield* bot.interactEntity({
        entityId: target.networkId,
        connectionEpoch: target.connectionEpoch,
        hand: Hand.MAIN,
      });
      const state = yield* eventually(
        bot.inventoryState().pipe(
          Effect.filterOrFail(
            (state) =>
              state.layout?.containerType === "merchant" && state.layout.buttons.length === 2,
            () => new CheckFailed({ message: "The villager trade menu did not open" }),
          ),
        ),
        "5 seconds",
      );
      const trade = state.layout?.buttons.find((button) => button.iconItemId === "minecraft:apple");
      if (!trade)
        return yield* Effect.fail(new CheckFailed({ message: "The apple trade is missing" }));
      const selected = yield* Effect.tryPromise({
        try: () =>
          client.service(BotService).clickContainerButton({
            instanceId: bot.instanceId,
            botId: bot.id,
            buttonId: trade.buttonId,
          }),
        catch: (cause) => new CheckFailed({ message: `Trade selection failed: ${String(cause)}` }),
      });
      yield* check(selected.success, "Trade selection was rejected");
      yield* eventually(
        bot.inventoryState().pipe(
          Effect.flatMap((state) =>
            check(
              state.slots.some(
                (slot) => slot.slot === 2 && slot.itemId === "minecraft:apple" && slot.count === 2,
              ),
              "The selected trade never filled the payment and result slots",
            ),
          ),
        ),
        "5 seconds",
      );
      yield* bot.clickInventory(2);
      yield* bot.clickInventory(3);
      yield* bot.closeContainer();
      yield* eventually(
        rcon(`data get entity ${BOT_NAME} Inventory`).pipe(
          Effect.flatMap((inventory) =>
            check(
              inventory.includes("minecraft:apple") && inventory.includes("minecraft:emerald"),
              "Vanilla did not retain the trade result and remaining payment",
            ),
          ),
        ),
        "5 seconds",
      );
      const uses = yield* rcon(
        `data get entity @e[type=minecraft:villager,tag=${TAG},limit=1] Offers.Recipes[1].uses`,
      );
      yield* check(
        /data: 1$/.test(uses),
        "Vanilla did not count exactly one use of the selected offer",
      );
    }).pipe(
      Effect.ensuring(
        bot
          .closeContainer()
          .pipe(
            Effect.ignore,
            Effect.andThen(
              rcon(`kill @e[type=minecraft:villager,tag=${TAG}]`, { check: false }).pipe(
                Effect.ignore,
              ),
            ),
          ),
      ),
    ),
} satisfies E2ETest;
