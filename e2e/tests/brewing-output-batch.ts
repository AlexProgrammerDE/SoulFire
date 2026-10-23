import { Effect } from "effect";
import { BOT_NAME, check, type E2ETest, placeBot, rcon } from "../harness.ts";

export default {
  name: "brewing-output-batch",
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
        yield* rcon("setblock 3927 64 8 brewing_stand");
        yield* rcon(
          'data merge block 3927 64 8 {Items:[{Slot:4b,id:"minecraft:blaze_powder",count:1}]}',
        );
        yield* rcon(
          `give ${BOT_NAME} potion[minecraft:potion_contents={potion:"minecraft:water"}] 3`,
        );
        yield* rcon(`give ${BOT_NAME} nether_wart 1`);
        yield* Effect.sleep("300 millis");
        const task = yield* bot.tasks.brew(
          { itemIds: ["minecraft:potion"] },
          { itemIds: ["minecraft:nether_wart"] },
          3,
          { station: { x: 3927, y: 64, z: 8 } },
        );
        yield* Effect.addFinalizer(() => task.cancel("test cleanup").pipe(Effect.ignore));
        const result = yield* task.result({ call: { timeoutMs: 30_000 } });
        yield* check(
          result.outputs.length === 3 &&
            result.outputs.every(
              (item) => item.count === 1 && item.potion?.potionId === "minecraft:awkward",
            ),
          "The task did not confirm all three brewed potions",
        );
        const actual = yield* rcon(
          `clear ${BOT_NAME} potion[minecraft:potion_contents={potion:"minecraft:awkward"}] 0`,
        );
        yield* check(
          actual.includes("Found 3 "),
          "Vanilla did not deliver all three brewed potions",
        );
        const station = yield* rcon("data get block 3927 64 8 Items");
        yield* check(
          !station.includes("minecraft:potion"),
          "Completed brewing left a potion in the stand",
        );
      }),
    ),
} satisfies E2ETest;
