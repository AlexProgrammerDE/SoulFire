package com.soulfiremc.server.grpc;

import java.util.Date;

public class DefaultAuthSystem implements AuthSystem {
  @Override
  public AuthenticatedUser authenticate(String subject, Date issuedAt) {
    return resource -> true;
  }
}
