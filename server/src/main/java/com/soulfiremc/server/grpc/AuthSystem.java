package com.soulfiremc.server.grpc;

import java.util.Date;

public interface AuthSystem {
  /**
   * Authenticates a user by subject and token issued at date.
   * @param subject The subject of the token
   * @param issuedAt The date the token was made, use to check if the token is valid for that user.
   *                 Use issuedAt to check if the token is valid for that user.
   *                 If a user resets their password, the token should be invalidated by raising
   *                 the required issuedAt date to the current date.
   * @return The authenticated user
   */
  AuthenticatedUser authenticate(String subject, Date issuedAt);
}
