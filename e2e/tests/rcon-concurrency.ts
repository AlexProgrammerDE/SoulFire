import { Effect, Result } from "effect";
import { check, type E2ETest, rcon } from "../harness.ts";

export default {
  name: "rcon-concurrency",
  run: () =>
    Effect.gen(function* () {
      const commands = ["advance_time", "advance_weather", "spawn_mobs", "random_tick_speed"].map(
        (rule) => `gamerule ${rule}`,
      );
      const expected = yield* Effect.forEach(commands, (command) => rcon(command));
      const results = yield* Effect.forEach(
        Array.from({ length: 128 }, (_, index) => index % commands.length),
        (index) => rcon(commands[index]!).pipe(Effect.map((output) => output === expected[index])),
        { concurrency: 16 },
      );
      yield* check(
        results.every(Boolean),
        `${results.filter((result) => !result).length} concurrent RCON commands received another command's output`,
      );
      const rejected = yield* Effect.result(rcon("soulfire_e2e_missing_command"));
      yield* check(Result.isFailure(rejected), "An invalid RCON command was accepted");
      yield* check(
        (yield* rcon(commands[0]!)) === expected[0],
        "A failed RCON command blocked the next command",
      );
    }),
} satisfies E2ETest;
