import type { Effect } from "effect";

import type { BeatGameDriver } from "./driver.js";
import type {
  BeatGameCheckpoint,
  BeatGameEyeSample,
  BeatGameItemRequirement,
  BeatGameObservation,
  BeatGameStrategy,
} from "./model.js";

export interface BeatGamePolicyContext {
  readonly driver: BeatGameDriver;
  readonly checkpoint: BeatGameCheckpoint;
  readonly observation: BeatGameObservation;
  readonly strategy: BeatGameStrategy;
}

export interface BeatGameRequirementPolicyContext extends BeatGamePolicyContext {
  readonly requirement: BeatGameItemRequirement;
}

type PolicyEffect<A> = Effect.Effect<A, unknown>;

/**
 * Optional replacements for game-specific policy.
 *
 * Hooks run inside the normal action timeout, retry, checkpoint, claim, and
 * control lifecycle. A hook should perform one stable unit of work and return.
 * The planner always reads a fresh observation before advancing a phase.
 */
export interface BeatGameStrategyHooks {
  readonly recoverDeath?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
  readonly eat?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
  readonly retreat?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
  readonly prepareEquipment?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
  readonly satisfyRequirement?: (context: BeatGameRequirementPolicyContext) => PolicyEffect<void>;
  readonly buildAndEnterNether?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
  readonly returnThroughPortal?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
  readonly throwEye?: (context: BeatGamePolicyContext) => PolicyEffect<BeatGameEyeSample>;
  readonly searchStronghold?: (context: BeatGamePolicyContext) => PolicyEffect<boolean>;
  readonly activateEndPortal?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
  readonly fightEnderDragon?: (context: BeatGamePolicyContext) => PolicyEffect<boolean>;
  readonly exitEnd?: (context: BeatGamePolicyContext) => PolicyEffect<void>;
}
