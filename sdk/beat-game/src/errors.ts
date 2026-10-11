import { Data } from "effect";

import type { BeatGamePhase } from "./model.js";

interface BeatGameDriverErrorFields {
  readonly operation: string;
  readonly code?: string;
  readonly retryable: boolean;
  readonly message: string;
  readonly cause?: unknown;
}

const DriverErrorBase = Data.TaggedError("BeatGameDriverError")<BeatGameDriverErrorFields>;

export class BeatGameDriverError extends DriverErrorBase {}

interface BeatGameErrorFields {
  readonly runId: string;
  readonly instanceId: string;
  readonly botId: string;
  readonly phase: BeatGamePhase;
  readonly action?: string;
  readonly retryable: boolean;
  readonly message: string;
  readonly cause?: unknown;
}

const ProtocolErrorBase = Data.TaggedError("BeatGameProtocolError")<BeatGameErrorFields>;

export class BeatGameProtocolError extends ProtocolErrorBase {}

const ObservationErrorBase = Data.TaggedError("BeatGameObservationError")<BeatGameErrorFields>;

export class BeatGameObservationError extends ObservationErrorBase {}

const ActionErrorBase = Data.TaggedError("BeatGameActionError")<BeatGameErrorFields>;

export class BeatGameActionError extends ActionErrorBase {}

const PathfindingErrorBase = Data.TaggedError("BeatGamePathfindingError")<BeatGameErrorFields>;

export class BeatGamePathfindingError extends PathfindingErrorBase {}

const RequirementErrorBase = Data.TaggedError("BeatGameRequirementError")<
  BeatGameErrorFields & {
    readonly requirement: string;
  }
>;

export class BeatGameRequirementError extends RequirementErrorBase {}

const CheckpointErrorBase = Data.TaggedError("BeatGameCheckpointError")<
  BeatGameErrorFields & {
    readonly expectedRevision?: number;
    readonly actualRevision?: number;
  }
>;

export class BeatGameCheckpointError extends CheckpointErrorBase {}

const CoordinationErrorBase = Data.TaggedError("BeatGameCoordinationError")<BeatGameErrorFields>;

export class BeatGameCoordinationError extends CoordinationErrorBase {}

const CancelledBase = Data.TaggedError("BeatGameCancelled")<
  BeatGameErrorFields & {
    readonly reason: string;
  }
>;

export class BeatGameCancelled extends CancelledBase {}

export type BeatGameError =
  | BeatGameProtocolError
  | BeatGameObservationError
  | BeatGameActionError
  | BeatGamePathfindingError
  | BeatGameRequirementError
  | BeatGameCheckpointError
  | BeatGameCoordinationError
  | BeatGameCancelled;
