package com.soulfiremc.server.grpc;

import io.grpc.Context;

public class ServerRPCConstants {
  public static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");
  public static final Context.Key<AuthenticatedUser> USER_CONTEXT_KEY = Context.key("user");

  private ServerRPCConstants() {}
}
