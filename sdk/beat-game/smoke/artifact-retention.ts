import { lstat, mkdir, readdir, rm } from "node:fs/promises";
import path from "node:path";

export interface PruneArtifactRunsOptions {
  readonly currentDirectory: string;
  readonly maximumRuns: number;
  readonly rootDirectory: string;
}

export async function pruneArtifactRuns(
  options: PruneArtifactRunsOptions,
): Promise<readonly string[]> {
  const rootDirectory = path.resolve(options.rootDirectory);
  const currentDirectory = path.resolve(options.currentDirectory);
  if (path.dirname(currentDirectory) !== rootDirectory) {
    throw new Error(
      `Artifact directory ${currentDirectory} must be a direct child of ${rootDirectory}`,
    );
  }
  if (!Number.isSafeInteger(options.maximumRuns) || options.maximumRuns < 1) {
    throw new Error("maximumRuns must be a positive integer");
  }

  await mkdir(rootDirectory, { recursive: true });
  const entries = await readdir(rootDirectory, { withFileTypes: true });
  const candidates = await Promise.all(
    entries.flatMap((entry) => {
      if (!entry.isDirectory()) {
        return [];
      }
      const directory = path.join(rootDirectory, entry.name);
      if (directory === currentDirectory) {
        return [];
      }
      return [
        lstat(directory).then((stats) => ({
          directory,
          modifiedAt: stats.mtimeMs,
        })),
      ];
    }),
  );
  candidates.sort((left, right) => right.modifiedAt - left.modifiedAt);

  const remove = candidates.slice(Math.max(0, options.maximumRuns - 1));
  await Promise.all(remove.map(({ directory }) => rm(directory, { recursive: true, force: true })));
  return remove.map(({ directory }) => directory);
}
