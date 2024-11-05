package com.soulfiremc.server.util.mcstructs;

import org.cloudburstmc.math.vector.Vector3d;

public abstract class HitResult {
  protected final Vector3d location;

  protected HitResult(Vector3d arg) {
    this.location = arg;
  }

  public abstract HitResult.Type getType();

  public Vector3d getLocation() {
    return this.location;
  }

  public enum Type {
    MISS,
    BLOCK,
    ENTITY;
  }
}

