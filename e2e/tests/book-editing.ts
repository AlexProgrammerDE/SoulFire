import { randomUUID } from "node:crypto";
import { Hand } from "@soulfiremc/sdk/generated/soulfire/bot_live_pb";
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

const PAGE = "Book page before signing";
const SECOND_PAGE = "Second page before signing";
const TITLE = "Book QA";
const text = (value: string) =>
  Array.from(value, (character) => ({ kind: Kind.CHARACTER, code: character.codePointAt(0)! }));
const key = (code: number, modifiers = 0) => [
  { kind: Kind.KEY, code, modifiers, action: 1 },
  { kind: Kind.KEY, code, modifiers, action: 0 },
];

export default {
  name: "book-editing",
  run: ({ bot, client }) =>
    Effect.gen(function* () {
      yield* rcon("forceload add 1344 -16 1376 16");
      yield* rcon("fill 1358 63 -2 1362 63 2 stone");
      yield* rcon("fill 1358 64 -2 1362 67 2 air");
      yield* rcon(`gamemode creative ${BOT_NAME}`);
      yield* placeBot(bot, { x: 1360.5, y: 64, z: 0.5 });
      yield* rcon(`item replace entity ${BOT_NAME} weapon.mainhand with minecraft:writable_book`);
      yield* bot.useItem({ hand: Hand.MAIN });
      yield* Effect.tryPromise({
        try: async () => {
          const abort = new AbortController();
          const sessionId = randomUUID();
          const pov = client.service(PovService);
          let sequence = 0n;
          let phase = 0;
          let signOpened = 0n;
          try {
            for await (const frame of pov.watch(
              {
                instanceId: bot.instanceId,
                botId: bot.id,
                sessionId,
                width: 640,
                height: 480,
                maxFps: 15,
              },
              { signal: AbortSignal.any([abort.signal, AbortSignal.timeout(15_000)]) },
            )) {
              const input = (events: ReturnType<typeof text>, width = 640, height = 480) =>
                pov.input({
                  sessionId,
                  sequence: ++sequence,
                  captured: true,
                  width,
                  height,
                  inputToken: frame.inputToken,
                  events,
                });
              if (phase === 0 && frame.screenOpen) {
                await input([
                  ...text(PAGE),
                  ...key(267),
                  ...text(SECOND_PAGE),
                  ...key(258, 1),
                  ...key(258, 1),
                  ...key(257),
                ]);
                signOpened = frame.sequence;
                phase = 1;
              } else if (phase === 1 && frame.sequence >= signOpened + 2n) {
                await input([...text(TITLE), ...key(268), ...key(262, 1)]);
                await input([], 320, 240);
                phase = 2;
              } else if (phase === 2 && frame.width === 320 && frame.height === 240) {
                await input([...text("X"), ...key(257)], 320, 240);
                phase = 3;
              } else if (phase === 3 && !frame.screenOpen) {
                return;
              }
            }
            throw new Error("The native book signing screen did not close");
          } finally {
            abort.abort();
          }
        },
        catch: (cause) =>
          new CheckFailed({ message: `Native book editing failed: ${String(cause)}` }),
      });
      yield* eventually(
        rcon(`data get entity ${BOT_NAME} SelectedItem`).pipe(
          Effect.flatMap((book) =>
            check(
              book.includes("minecraft:written_book") &&
                book.includes(`X${TITLE.slice(1)}`) &&
                book.includes(PAGE) &&
                book.includes(SECOND_PAGE),
              `POV resize lost the book title, selection, or pages: ${book}`,
            ),
          ),
        ),
        "5 seconds",
      );
    }),
} satisfies E2ETest;
