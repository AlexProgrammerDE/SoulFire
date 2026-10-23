import { Effect } from "effect";
import { BOT_NAME, check, type E2ETest, placeBot, rcon } from "../harness.ts";

export default {
  name: "smelting-output-storage",
  run: ({ bot }) =>
    Effect.scoped(
      Effect.gen(function* () {
        yield* rcon("forceload add 3920 0 3935 15");
        yield* Effect.addFinalizer(() =>
          Effect.gen(function* () {
            yield* bot.closeContainer().pipe(Effect.ignore);
            yield* rcon(`gamemode creative ${BOT_NAME}`, { check: false });
            yield* placeBot(bot, { x: 0.5, y: 64, z: 0.5 }).pipe(Effect.ignore);
            yield* rcon("data modify block 3927 64 8 Items set value []", { check: false });
            yield* rcon("fill 3926 63 7 3930 66 9 air", { check: false });
            yield* rcon("forceload remove 3920 0 3935 15", { check: false });
          }).pipe(Effect.ignore),
        );
        yield* rcon(`gamemode creative ${BOT_NAME}`);
        yield* rcon("fill 3926 63 7 3930 63 9 stone");
        yield* placeBot(bot, { x: 3928.5, y: 64, z: 8.5 });
        yield* rcon("setblock 3927 64 8 furnace");
        yield* rcon('data merge block 3927 64 8 {Items:[{Slot:1b,id:"minecraft:coal",count:2}]}');
        for (let slot = 0; slot < 36; slot++) {
          const target = slot < 27 ? `inventory.${slot}` : `hotbar.${slot - 27}`;
          const item =
            slot === 0 || slot === 2 ? "iron_ingot 63" : slot === 1 ? "iron_ore 64" : "stone 64";
          yield* rcon(`item replace entity ${BOT_NAME} ${target} with minecraft:${item}`);
        }
        yield* Effect.sleep("300 millis");
        // No slot is empty. The two ingots must be stored across two partial
        // stacks, with each cursor reduction confirmed before the next click.
        const task = yield* bot.tasks.smelt({ itemIds: ["minecraft:iron_ore"] }, 2, {
          station: { x: 3927, y: 64, z: 8 },
          fuel: { itemIds: ["minecraft:coal"] },
        });
        yield* Effect.addFinalizer(() => task.cancel("test cleanup").pipe(Effect.ignore));
        const result = yield* task.result({ call: { timeoutMs: 30_000 } });
        yield* check(
          result.outputs.length === 1 && result.outputs[0]?.count === 2,
          "The task did not confirm both smelted outputs",
        );
        for (const [item, count] of [
          ["iron_ingot", 128],
          ["iron_ore", 62],
        ] as const) {
          const actual = yield* rcon(`clear ${BOT_NAME} minecraft:${item} 0`);
          yield* check(actual.includes(`Found ${count} `), `Vanilla has the wrong ${item} count`);
        }
        const contents = yield* rcon("data get block 3927 64 8 Items");
        yield* check(
          !contents.includes("iron_"),
          "Completed smelting left input or output in the furnace",
        );
        yield* check(
          contents.includes("minecraft:coal"),
          "Completed smelting removed unused station fuel",
        );
      }),
    ),
} satisfies E2ETest;
