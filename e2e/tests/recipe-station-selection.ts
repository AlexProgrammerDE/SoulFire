import { Effect } from "effect";
import { BotTaskStatus } from "@soulfiremc/sdk/generated/soulfire/task_pb";
import { BOT_NAME, check, type E2ETest, placeBot, rcon } from "../harness.ts";

export default {
  name: "recipe-station-selection",
  run: ({ bot }) =>
    Effect.scoped(
      Effect.gen(function* () {
        yield* rcon("forceload add 3904 0 3919 15");
        yield* Effect.addFinalizer(() =>
          Effect.gen(function* () {
            yield* bot.closeContainer().pipe(Effect.ignore);
            yield* rcon(`gamemode creative ${BOT_NAME}`, { check: false });
            yield* placeBot(bot, { x: 0.5, y: 64, z: 0.5 }).pipe(Effect.ignore);
            yield* rcon("fill 3904 63 0 3919 66 15 air", { check: false });
            yield* rcon("forceload remove 3904 0 3919 15", { check: false });
          }).pipe(Effect.ignore),
        );
        yield* rcon("fill 3904 63 0 3919 63 15 stone");
        for (const kind of ["smelt", "brew"]) {
          for (const explicit of [true, false]) {
            yield* bot.closeContainer().pipe(Effect.ignore);
            yield* rcon(`gamemode creative ${BOT_NAME}`);
            yield* placeBot(bot, { x: 3909.5, y: 64, z: 8.5 }, -90);
            const brewing = kind === "brew";
            const x = brewing ? 3911 : 3907;
            const previous = { x, y: 64, z: 7 };
            const requested = { x, y: 64, z: 10 };
            for (const z of [7, 10]) {
              yield* rcon(`setblock ${x} 64 ${z} air`, { check: false });
              yield* rcon(`setblock ${x} 64 ${z} ${brewing ? "brewing_stand" : "furnace"}`);
            }
            yield* rcon(
              `data merge block ${x} 64 7 {Items:[{Slot:${brewing ? 3 : 0}b,id:"minecraft:${brewing ? "stick" : "cobblestone"}",count:1}]}`,
            );
            for (const item of brewing
              ? [
                  'potion[minecraft:potion_contents={potion:"minecraft:water"}]',
                  "nether_wart",
                  "blaze_powder",
                ]
              : ["iron_ore", "coal"]) {
              yield* rcon(`give ${BOT_NAME} minecraft:${item}`);
            }
            // With no explicit station, use the empty menu that is already open.
            // Otherwise the task must switch away from the occupied old station.
            yield* bot.inventory.open(explicit ? previous : requested);
            const originalContents = yield* rcon(`data get block ${x} 64 7 Items`);
            if (explicit) {
              const station = { ...requested, dimension: "minecraft:the_nether" };
              const invalid = yield* brewing
                ? bot.tasks.brew(
                    { itemIds: ["minecraft:potion"] },
                    { itemIds: ["minecraft:nether_wart"] },
                    1,
                    { station },
                  )
                : bot.tasks.smelt({ itemIds: ["minecraft:iron_ore"] }, 1, { station });
              const rejected = yield* invalid.wait({ call: { timeoutMs: 5000 } });
              yield* check(
                rejected.status === BotTaskStatus.FAILED &&
                  rejected.failure?.code === "invalid_argument",
                `The ${kind} task ignored an explicit station dimension`,
              );
              yield* check(
                (yield* rcon(`data get block ${x} 64 7 Items`)) === originalContents,
                `The rejected ${kind} task changed the other station`,
              );
            }
            const options = explicit ? { station: requested } : {};
            const task = yield* brewing
              ? bot.tasks.brew(
                  { itemIds: ["minecraft:potion"] },
                  { itemIds: ["minecraft:nether_wart"] },
                  1,
                  options,
                )
              : bot.tasks.smelt({ itemIds: ["minecraft:iron_ore"] }, 1, {
                  ...options,
                  fuel: { itemIds: ["minecraft:coal"] },
                });
            yield* Effect.addFinalizer(() => task.cancel("test cleanup").pipe(Effect.ignore));
            yield* task.result({ call: { timeoutMs: 30_000 } });
            const inventory = yield* rcon(`data get entity ${BOT_NAME} Inventory`);
            yield* check(
              inventory.includes(brewing ? "minecraft:awkward" : "minecraft:iron_ingot"),
              `The requested ${kind} operation produced no native output`,
            );
            yield* check(
              (yield* rcon(`data get block ${x} 64 7 Items`)) === originalContents,
              `The ${kind} task changed the other station's contents`,
            );
            yield* check(
              (yield* rcon(`data get block ${x} 64 10 Items`)).endsWith(": []"),
              `The ${kind} task left items in the requested station`,
            );
          }
        }
      }),
    ),
} satisfies E2ETest;
