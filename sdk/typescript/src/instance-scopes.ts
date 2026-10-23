import { Deferred, Effect } from "effect";

interface InstanceState {
  references: number;
  deletions: number;
  deleted: boolean;
  changed: Deferred.Deferred<void>;
}

export interface InstanceScope {
  readonly deleted: boolean;
  readonly awaitPendingDeletion: Effect.Effect<void>;
  release(): void;
}

// Share confirmed deletions across handles without retaining inactive instances.
export class InstanceScopes {
  readonly #states = new Map<string, InstanceState>();

  #state(id: string): InstanceState {
    let state = this.#states.get(id);
    if (!state) {
      state = { references: 0, deletions: 0, deleted: false, changed: Deferred.makeUnsafe() };
      this.#states.set(id, state);
    }
    return state;
  }

  #prune(id: string, state: InstanceState) {
    if (state.references === 0 && state.deletions === 0 && this.#states.get(id) === state)
      this.#states.delete(id);
  }

  acquire(id: string): InstanceScope {
    const state = this.#state(id);
    state.references++;
    let active = true;
    return {
      get deleted() {
        return state.deleted;
      },
      awaitPendingDeletion: Effect.gen(function* () {
        while (!state.deleted && state.deletions > 0) yield* Deferred.await(state.changed);
      }),
      release: () => {
        if (!active) return;
        active = false;
        state.references--;
        this.#prune(id, state);
      },
    };
  }

  beginDeletion(id: string): (deleted: boolean) => void {
    const state = this.#state(id);
    state.deletions++;
    let pending = true;
    return (deleted) => {
      if (!pending) return;
      pending = false;
      state.deleted ||= deleted;
      state.deletions--;
      const changed = state.changed;
      state.changed = Deferred.makeUnsafe();
      Deferred.doneUnsafe(changed, Effect.void);
      this.#prune(id, state);
    };
  }
}
