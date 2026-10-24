import { BotTaskStatus } from "@soulfiremc/sdk/generated/soulfire/task_pb";
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

export default {
  name: "crafting-output-storage",
  run: ({ bot }) =>
    Effect.scoped(
      Effect.gen(function* () {
        const wasForced = !(yield* rcon("forceload query 4000 0", { check: false })).includes(
          "not marked",
        );
        yield* rcon("forceload add 4000 0 4015 15");
        yield* Effect.addFinalizer(() =>
          Effect.gen(function* () {
            // Clear the owned cursor before closing a full inventory can drop it.
            yield* rcon(`clear ${BOT_NAME}`, { check: false });
            yield* bot.closeContainer().pipe(Effect.ignore);
            yield* rcon(`gamemode creative ${BOT_NAME}`, { check: false });
            yield* placeBot(bot, { x: 0.5, y: 64, z: 0.5 });
            yield* rcon("fill 4006 63 7 4010 66 9 air", { check: false });
            if (!wasForced) yield* rcon("forceload remove 4000 0 4015 15", { check: false });
          }).pipe(Effect.ignore),
        );
        yield* rcon(`gamemode creative ${BOT_NAME}`);
        yield* rcon("fill 4006 63 7 4010 63 9 stone");
        yield* placeBot(bot, { x: 4008.5, y: 64, z: 8.5 });
        yield* rcon(`gamemode survival ${BOT_NAME}`);
        for (let slot = 0; slot < 36; slot++) {
          const target = slot < 27 ? `inventory.${slot}` : `hotbar.${slot - 27}`;
          yield* rcon(`item replace entity ${BOT_NAME} ${target} with stone 64`);
        }
        yield* rcon(`item replace entity ${BOT_NAME} inventory.1 with oak_log 64`);
        const recipe = yield* eventually(
          bot.recipes.list({ resultItemId: "minecraft:oak_planks" }).pipe(
            Effect.map((response) => response.recipes[0]),
            Effect.filterOrFail(
              (value) => value !== undefined,
              () => new CheckFailed({ message: "The plank recipe did not load" }),
            ),
          ),
          "5 seconds",
        );
        for (const mode of ["split", "offhand", "exhausted"] as const) {
          yield* rcon(
            `item replace entity ${BOT_NAME} inventory.0 with oak_planks ${mode === "split" ? 62 : 63}`,
          );
          yield* rcon(`item replace entity ${BOT_NAME} inventory.1 with oak_log 64`);
          yield* rcon(`item replace entity ${BOT_NAME} inventory.2 with oak_planks 62`);
          yield* rcon(
            `item replace entity ${BOT_NAME} weapon.offhand with ${mode === "offhand" ? "air" : "stone 64"}`,
          );
          yield* eventually(
            bot
              .inventoryState()
              .pipe(
                Effect.flatMap((state) =>
                  check(
                    state.slots.find((slot) => slot.slot === 9)?.count ===
                      (mode === "split" ? 62 : 63) &&
                      state.slots.find((slot) => slot.slot === 10)?.count === 64 &&
                      state.slots.find((slot) => slot.slot === 11)?.count === 62 &&
                      (mode === "offhand"
                        ? !state.slots.some((slot) => slot.slot === 45)
                        : state.slots.find((slot) => slot.slot === 45)?.itemId ===
                          "minecraft:stone"),
                    "The crafting storage fixture did not synchronize",
                  ),
                ),
              ),
            "3 seconds",
          );
          const task = yield* bot.tasks.craft(recipe.recipeId, 1);
          yield* Effect.addFinalizer(() => task.cancel("test cleanup").pipe(Effect.ignore));
          if (mode === "exhausted") {
            const terminal = yield* task.wait({ call: { timeoutMs: 10_000 } });
            yield* check(
              terminal.status === BotTaskStatus.FAILED &&
                terminal.failure?.code === "resource_exhausted",
              "Crafting reported completion without enough storage",
            );
            const state = yield* bot.inventoryState();
            yield* check(
              state.carriedItem?.itemId === "minecraft:oak_planks" && state.carriedItem.count === 1,
              "Crafting did not retain its unstored output on the cursor",
            );
          } else {
            const result = yield* task.result({ call: { timeoutMs: 10_000 } });
            yield* check(
              result.crafted === 1 &&
                result.result?.itemId === "minecraft:oak_planks" &&
                result.result.count === 4,
              "Crafting did not report one complete four-plank output",
            );
            yield* check(
              !(yield* bot.inventoryState()).carriedItem,
              "Completed crafting left output on the cursor",
            );
          }
          // Vanilla counts equipment and the cursor as well as the main inventory.
          // In 26.3, equipment is saved separately from the Inventory NBT list.
          const expected = mode === "split" ? 128 : 129;
          for (const [item, expectedCount] of [
            ["oak_planks", expected],
            ["oak_log", 63],
          ] as const) {
            yield* eventually(
              rcon(`clear ${BOT_NAME} ${item} 0`).pipe(
                Effect.flatMap((count) =>
                  check(
                    count.includes(`Found ${expectedCount} `),
                    `Crafting changed the ${item} count`,
                  ),
                ),
              ),
              "3 seconds",
            );
          }
          if (mode === "offhand") {
            const offhand = yield* rcon(`data get entity ${BOT_NAME} equipment.offhand`);
            yield* check(
              offhand.includes("minecraft:oak_planks") && offhand.includes("count: 1"),
              "The remaining crafted plank did not reach the vanilla offhand",
            );
          }
        }
        // Free one stack, then confirm the retained cursor can still be stored.
        yield* rcon(`item replace entity ${BOT_NAME} inventory.0 with air`);
        yield* eventually(
          bot
            .inventoryState()
            .pipe(
              Effect.flatMap((state) =>
                check(
                  !state.slots.some((slot) => slot.slot === 9),
                  "The recovery slot is occupied",
                ),
              ),
            ),
          "3 seconds",
        );
        yield* bot.clickInventory(9);
        yield* eventually(
          rcon(`clear ${BOT_NAME} oak_planks 0`).pipe(
            Effect.flatMap((count) =>
              check(count.includes("Found 65 "), "The retained crafted plank could not be stored"),
            ),
          ),
          "3 seconds",
        );
        yield* check(!(yield* bot.inventoryState()).carriedItem, "Recovery left a carried item");
      }),
    ),
} satisfies E2ETest;
