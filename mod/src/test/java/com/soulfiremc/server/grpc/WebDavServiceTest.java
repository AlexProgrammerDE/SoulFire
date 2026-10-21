/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.grpc;

import com.linecorp.armeria.server.Server;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.UserRole;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.RPCConstants;
import org.apache.catalina.LifecycleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class WebDavServiceTest {
  @Test
  void concurrentFirstRequestsWorkAndStoppingReleasesTomcat(@TempDir Path storage) throws Exception {
    var soulFireServer = mock(SoulFireServer.class);
    var auth = mock(AuthSystem.class);
    var user = mock(SoulFireUser.class);
    when(soulFireServer.getObjectStoragePath()).thenReturn(storage);
    when(soulFireServer.authSystem()).thenReturn(auth);
    when(auth.authenticateByHeader("Bearer storage-test", RPCConstants.WEBDAV_AUDIENCE)).thenReturn(Optional.of(user));
    when(user.getRole()).thenReturn(UserRole.ADMIN);
    var webdav = RPCServer.newWebDAVService(soulFireServer);
    var server = Server.builder()
      .http(new InetSocketAddress("127.0.0.1", 0))
      .serviceUnder("/webdav/", new RewriteBlocker(webdav))
      .build();
    try (var client = HttpClient.newHttpClient()) {
      server.start().join();
      var baseUri = URI.create("http://127.0.0.1:" + server.activeLocalPort() + "/webdav/");
      var requests = IntStream.range(0, 8)
        .mapToObj(index -> client.sendAsync(HttpRequest.newBuilder(baseUri.resolve("collection-" + index))
          .timeout(Duration.ofSeconds(10))
          .header("Authorization", "Bearer storage-test")
          .method("MKCOL", HttpRequest.BodyPublishers.noBody())
          .build(), HttpResponse.BodyHandlers.ofString()))
        .toList();
      for (var request : requests) {
        assertEquals(201, request.join().statusCode());
      }
      var connector = webdav.connector();
      assertEquals(LifecycleState.STARTED, connector.getState());
      assertEquals(0, connector.getLocalPort(), "WebDAV must not open another HTTP listener");
      server.stop().join();
      assertEquals(LifecycleState.DESTROYED, connector.getState());
    } finally {
      server.stop().join();
    }
  }
}
