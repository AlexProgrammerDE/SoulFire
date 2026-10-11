import { create, toBinary } from "@bufbuild/protobuf";
import { AnySchema } from "@bufbuild/protobuf/wkt";
import {
  BotTaskProgressSchema,
  CollectBlocksTaskProgressDetail_Phase,
  CollectBlocksTaskProgressDetailSchema,
} from "@soulfiremc/sdk/generated/soulfire/task_pb";
import { describe, expect, it } from "vitest";

import { decodeSmokeTaskProgress } from "../smoke/debug-task-progress.js";

describe("smoke task progress diagnostics", () => {
  it("decodes typed collection candidates and failed approaches", () => {
    const detail = create(CollectBlocksTaskProgressDetailSchema, {
      phase: CollectBlocksTaskProgressDetail_Phase.FOLLOWING_ROUTE,
      playerPosition: {
        x: -131,
        y: 64,
        z: 45,
        dimension: "minecraft:overworld",
      },
      activeTargets: [
        {
          x: -131,
          y: 72,
          z: 45,
          dimension: "minecraft:overworld",
        },
      ],
      failedApproaches: [
        {
          target: {
            x: -131,
            y: 72,
            z: 45,
            dimension: "minecraft:overworld",
          },
          playerPositions: [
            {
              x: -131,
              y: 64,
              z: 45,
              dimension: "minecraft:overworld",
            },
          ],
        },
      ],
      pathCurrentMovement: 3,
      pathTotalMovements: 12,
    });
    const progress = create(BotTaskProgressSchema, {
      message: "Following collection route",
      detail: create(AnySchema, {
        typeUrl: "type.googleapis.com/soulfire.v1.CollectBlocksTaskProgressDetail",
        value: toBinary(CollectBlocksTaskProgressDetailSchema, detail),
      }),
    });

    expect(decodeSmokeTaskProgress(progress)).toMatchObject({
      detailType: "soulfire.v1.CollectBlocksTaskProgressDetail",
      detail: {
        phase: CollectBlocksTaskProgressDetail_Phase.FOLLOWING_ROUTE,
        playerPosition: { x: -131, y: 64, z: 45 },
        activeTargets: [{ x: -131, y: 72, z: 45 }],
        failedApproaches: [
          {
            target: { x: -131, y: 72, z: 45 },
            playerPositions: [{ x: -131, y: 64, z: 45 }],
          },
        ],
        pathCurrentMovement: 3,
        pathTotalMovements: 12,
      },
    });
  });
});
