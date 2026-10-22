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

const LECTERN = { x: 2481, y: 64, z: 8 };
const rpc = <A>(call: () => Promise<A>) =>
  Effect.tryPromise({ try: call, catch: (cause) => new CheckFailed({ message: String(cause) }) });

export default {
  name: "lectern-controls",
  run: ({ bot, client }) =>
    Effect.scoped(
      Effect.gen(function* () {
        yield* rcon("forceload add 2480 0 2495 15");
        yield* Effect.addFinalizer(() =>
          Effect.gen(function* () {
            yield* bot.closeContainer().pipe(Effect.ignore);
            yield* rcon(`gamemode creative ${BOT_NAME}`, { check: false });
            yield* rcon("fill 2480 63 7 2483 66 9 air", { check: false });
            yield* rcon("forceload remove 2480 0 2495 15", { check: false });
          }).pipe(Effect.ignore),
        );
        yield* rcon("fill 2480 63 7 2483 63 9 stone");
        yield* rcon("fill 2480 64 7 2483 66 9 air");
        yield* rcon(
          'setblock 2481 64 8 lectern[has_book=true]{Book:{id:"minecraft:written_book",count:1,components:{"minecraft:written_book_content":{title:{raw:"Lectern QA"},author:"QA",pages:[{raw:{text:"First page"}},{raw:{text:"Second page"}}]}}},Page:0}',
        );
        yield* rcon(`gamemode adventure ${BOT_NAME}`);
        yield* rcon(`clear ${BOT_NAME} minecraft:written_book`, { check: false });
        yield* placeBot(bot, { x: 2480.5, y: 64, z: 8.5 }, -90);
        yield* bot.inventory.open(LECTERN);
        yield* eventually(
          bot
            .inventoryState()
            .pipe(
              Effect.flatMap((state) =>
                check(
                  state.layout?.buttons.find((button) => button.buttonId === 3)?.disabled === true,
                  "Adventure mode offered to take a protected lectern book",
                ),
              ),
            ),
          "3 seconds",
        );

        const service = client.service(BotService);
        const click = (buttonId: number) =>
          rpc(() =>
            service.clickContainerButton({ instanceId: bot.instanceId, botId: bot.id, buttonId }),
          );
        yield* check(
          !(yield* click(3)).success,
          "Taking a protected book was reported as accepted",
        );
        yield* check(!(yield* click(1)).success, "Previous page was accepted on the first page");
        yield* check((yield* click(2)).success, "Adventure mode could not read the next page");
        yield* eventually(
          rcon("data get block 2481 64 8 Page").pipe(
            Effect.flatMap((data) => check(data.endsWith(": 1"), "Vanilla did not turn the page")),
          ),
          "3 seconds",
        );
        yield* eventually(
          bot
            .inventoryState()
            .pipe(
              Effect.flatMap((state) =>
                check(
                  state.layout?.currentBookPage === 1,
                  "The client did not receive the new page",
                ),
              ),
            ),
          "3 seconds",
        );
        yield* check(!(yield* click(2)).success, "Next page was accepted on the last page");
        yield* check(
          (yield* rcon("data get block 2481 64 8 Book.id")).includes("minecraft:written_book"),
          "The protected book left the lectern",
        );

        yield* rcon(`gamemode survival ${BOT_NAME}`);
        yield* eventually(
          bot
            .inventoryState()
            .pipe(
              Effect.flatMap((state) =>
                check(
                  state.layout?.buttons.find((button) => button.buttonId === 3)?.disabled === false,
                  "Survival mode could not take the lectern book",
                ),
              ),
            ),
          "3 seconds",
        );
        yield* check((yield* click(3)).success, "Taking an available book was rejected");
        yield* eventually(
          rcon(`clear ${BOT_NAME} minecraft:written_book 0`).pipe(
            Effect.flatMap((data) =>
              check(data.includes("Found 1"), "The lectern book never reached the player"),
            ),
          ),
          "3 seconds",
        );
      }),
    ),
} satisfies E2ETest;
