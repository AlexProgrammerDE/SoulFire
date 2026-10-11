import { fromBinary } from "@bufbuild/protobuf";
import {
  CollectBlocksTaskProgressDetailSchema,
  type BotTaskProgress,
} from "@soulfiremc/sdk/generated/soulfire/task_pb";

const collectBlocksProgressType = "soulfire.v1.CollectBlocksTaskProgressDetail";

export function decodeSmokeTaskProgress(progress: BotTaskProgress | undefined): unknown {
  if (progress?.detail === undefined) {
    return progress;
  }
  const { detail, ...summary } = progress;
  const typeName = canonicalTypeName(detail.typeUrl);
  if (typeName !== collectBlocksProgressType) {
    return progress;
  }
  return {
    ...summary,
    detailType: typeName,
    detail: fromBinary(CollectBlocksTaskProgressDetailSchema, detail.value),
  };
}

function canonicalTypeName(typeUrl: string): string {
  const separator = typeUrl.lastIndexOf("/");
  return separator === -1 ? typeUrl : typeUrl.slice(separator + 1);
}
