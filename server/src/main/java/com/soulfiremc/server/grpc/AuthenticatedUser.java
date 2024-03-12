package com.soulfiremc.server.grpc;

public interface AuthenticatedUser {
  boolean canAccess(Resource resource);

  default void canAccessOrThrow(Resource resource) {
    if (!canAccess(resource)) {
      throw new IllegalStateException("User does not have access to resource: " + resource);
    }
  }
}
