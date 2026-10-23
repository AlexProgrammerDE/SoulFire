import { goals } from "@soulfiremc/sdk";
import {
  BotTaskConflictPolicy,
  BotTaskPriority,
  BotTaskStatus,
} from "@soulfiremc/sdk/generated/soulfire/task_pb";
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

const TABLE = { x: 4039, y: 64, z: 8 };
const FURNACE = { x: 4039, y: 64, z: 10 };
const URGENT = {
  priority: BotTaskPriority.HIGH,
  conflictPolicy: BotTaskConflictPolicy.SUSPEND_LOWER_PRIORITY,
};

export default {
  name: "crafting-task-resume",
  run: ({ bot }) =>
    Effect.scoped(
      Effect.gen(function* () {
        const forced: number[] = [];
        for (const x of [4032, 4048]) {
          if ((yield* rcon(`forceload query ${x} 0`, { check: false })).includes("not marked")) {
            forced.push(x);
          }
        }
        yield* rcon("forceload add 4032 0 4063 15");
        yield* Effect.addFinalizer(() =>
          Effect.gen(function* () {
            yield* rcon(`clear ${BOT_NAME}`, { check: false });
            yield* bot.closeContainer().pipe(Effect.ignore);
            yield* rcon(`gamemode creative ${BOT_NAME}`, { check: false });
            yield* placeBot(bot, { x: 0.5, y: 64, z: 0.5 });
            yield* rcon("data modify block 4039 64 10 Items set value []", { check: false });
            yield* rcon("fill 4038 63 7 4055 66 11 air", { check: false });
            for (const x of forced) yield* rcon(`forceload remove ${x} 0 ${x + 15} 15`);
          }).pipe(Effect.ignore),
        );
        yield* rcon("fill 4038 63 7 4055 63 11 stone");
        yield* rcon("setblock 4039 64 8 crafting_table");
        for (const table of [true, false]) {
          yield* Effect.scoped(
            Effect.gen(function* () {
              yield* bot.closeContainer();
              yield* rcon(`gamemode creative ${BOT_NAME}`);
              yield* placeBot(bot, { x: 4040.5, y: 64, z: 8.5 }, 90);
              yield* rcon("setblock 4039 64 10 air", { check: false });
              yield* rcon("setblock 4039 64 10 furnace");
              for (const item of [table ? "oak_planks 32" : "oak_log 4", "iron_ore", "coal"]) {
                yield* rcon(`give ${BOT_NAME} ${item}`);
              }
              const item = table ? "minecraft:chest" : "minecraft:oak_planks";
              const recipe = yield* eventually(
                bot.recipes.list({ resultItemId: item }).pipe(
                  Effect.map((response) => response.recipes[0]),
                  Effect.filterOrFail(
                    (value) => value !== undefined,
                    () => new CheckFailed({ message: "The crafting recipe did not load" }),
                  ),
                ),
                "5 seconds",
              );
              const craft = yield* bot.tasks.craft(
                recipe.recipeId,
                4,
                table ? { station: TABLE } : {},
              );
              yield* Effect.addFinalizer(() => craft.cancel("test cleanup").pipe(Effect.ignore));
              yield* eventually(
                bot
                  .inventoryState()
                  .pipe(
                    Effect.flatMap((state) =>
                      check(
                        !state.carriedItem &&
                          state.slots.some((slot) => slot.slot === 0 && slot.itemId === item),
                        "The crafting grid never produced its first result",
                      ),
                    ),
                  ),
                "5 seconds",
                "10 millis",
              );
              const smelt = yield* bot.tasks.smelt({ itemIds: ["minecraft:iron_ore"] }, 1, {
                station: FURNACE,
                fuel: { itemIds: ["minecraft:coal"] },
                ...URGENT,
              });
              yield* Effect.addFinalizer(() => smelt.cancel("test cleanup").pipe(Effect.ignore));
              yield* eventually(
                craft
                  .refresh()
                  .pipe(
                    Effect.flatMap((task) =>
                      check(
                        task.status === BotTaskStatus.SUSPENDED,
                        "Smelting did not suspend crafting",
                      ),
                    ),
                  ),
                "3 seconds",
                "10 millis",
              );
              yield* smelt.result({ call: { timeoutMs: 20_000 } });
              // Interrupt a picked-up output too: reopening a table has already
              // returned it to the inventory and must not repeat that operation.
              yield* eventually(
                bot
                  .inventoryState()
                  .pipe(
                    Effect.flatMap((state) =>
                      check(
                        state.carriedItem?.itemId === item,
                        "Crafting did not pick up its output",
                      ),
                    ),
                  ),
                "10 seconds",
                "10 millis",
              );
              const diversion = yield* bot.tasks.goTo(
                goals.block({ x: 4052, y: 64, z: 8 }),
                URGENT,
              );
              yield* Effect.addFinalizer(() =>
                diversion.cancel("test cleanup").pipe(Effect.ignore),
              );
              yield* eventually(
                craft
                  .refresh()
                  .pipe(
                    Effect.flatMap((task) =>
                      check(
                        task.status === BotTaskStatus.SUSPENDED,
                        "Navigation did not suspend crafting",
                      ),
                    ),
                  ),
                "3 seconds",
                "10 millis",
              );
              yield* diversion.result({ call: { timeoutMs: 15_000 } });
              const result = yield* craft.result({ call: { timeoutMs: 15_000 } });
              yield* check(result.crafted === 4, "Resumed crafting changed its operation count");
              for (const [id, count] of [
                [item, table ? 4 : 16],
                ["minecraft:iron_ingot", 1],
              ] as const) {
                yield* eventually(
                  rcon(`clear ${BOT_NAME} ${id} 0`).pipe(
                    Effect.flatMap((text) =>
                      check(
                        text.includes(`Found ${count} `),
                        "Vanilla output disagrees with resumed tasks",
                      ),
                    ),
                  ),
                  "3 seconds",
                );
              }
              yield* check(
                (yield* bot.inventory.count(
                  table ? "minecraft:oak_planks" : "minecraft:oak_log",
                )) === 0n,
                "Resumed crafting left an operation's ingredients unused",
              );
              const state = yield* bot.inventoryState();
              yield* check(
                !state.carriedItem && state.layout?.containerType === "inventory",
                "Completed crafting left a cursor item or an open table",
              );
            }),
          );
        }
      }),
    ),
} satisfies E2ETest;
