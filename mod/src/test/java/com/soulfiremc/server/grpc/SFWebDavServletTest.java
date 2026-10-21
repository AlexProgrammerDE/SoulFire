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

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.UserRole;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.RPCConstants;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class SFWebDavServletTest {
  private static final String LOCK_BODY = """
    <D:lockinfo xmlns:D="DAV:">
      <D:lockscope><D:exclusive/></D:lockscope>
      <D:locktype><D:write/></D:locktype>
      <D:owner>storage-test</D:owner>
    </D:lockinfo>
    """;
  private Tomcat tomcat;
  private SFWebDavServlet servlet;
  private HttpClient client;
  private URI baseUri;

  @BeforeEach
  void setUp(@TempDir Path directory) throws Exception {
    var server = mock(SoulFireServer.class);
    var auth = mock(AuthSystem.class);
    var user = mock(SoulFireUser.class);
    when(server.authSystem()).thenReturn(auth);
    when(auth.authenticateByHeader("Bearer storage-test", RPCConstants.WEBDAV_AUDIENCE)).thenReturn(Optional.of(user));
    when(user.getRole()).thenReturn(UserRole.ADMIN);
    var storage = Files.createDirectory(directory.resolve("storage"));
    Files.createDirectory(storage.resolve("collection"));
    Files.createDirectory(storage.resolve("sibling"));
    servlet = new SFWebDavServlet(server);
    tomcat = new Tomcat();
    tomcat.setBaseDir(directory.resolve("tomcat").toString());
    tomcat.setPort(0);
    tomcat.getConnector().setProperty("address", "127.0.0.1");
    var context = tomcat.addContext("", storage.toString());
    var registration = Tomcat.addServlet(context, "webdav", servlet);
    registration.addInitParameter("readonly", "false");
    registration.addInitParameter("listings", "true");
    context.addServletMappingDecoded("/webdav/*", "webdav");
    tomcat.start();
    baseUri = URI.create("http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/webdav");
    client = HttpClient.newHttpClient();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    if (tomcat != null) {
      tomcat.stop();
      tomcat.destroy();
    }
  }

  @Test
  void newLocksReturnTheirTokenInTheResponseHeader() throws Exception {
    for (var path : List.of("/collection", "/resource.txt")) {
      var response = lock(path, "Second-60");
      assertEquals(200, response.statusCode());
      var token = response.headers().firstValue("Lock-Token").orElseThrow();
      assertEquals(204, send(path, "UNLOCK", "", "Lock-Token", token).statusCode());
    }
  }

  @Test
  void unlockingACollectionAllowsNewWritesAndLocks() throws Exception {
    var lock = lock("/collection", "Second-60");
    assertEquals(200, lock.statusCode());
    assertEquals(423, put("/collection/blocked.txt").statusCode());
    var document = DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder()
      .parse(new InputSource(new StringReader(lock.body())));
    var token = "<" + document.getElementsByTagNameNS("DAV:", "locktoken").item(0).getTextContent().trim() + ">";
    assertEquals(204, send("/collection", "UNLOCK", "", "Lock-Token", token).statusCode());
    assertEquals(201, put("/collection/allowed.txt").statusCode());
    assertEquals(200, lock("/collection", "Second-60").statusCode());
  }

  @Test
  void expiredCollectionLocksDoNotBreakUnrelatedWritesOrRelocking() throws Exception {
    assertEquals(200, lock("/collection", "Second-1").statusCode());
    Thread.sleep(Duration.ofMillis(1100));
    assertEquals(201, put("/sibling/allowed.txt").statusCode());
    assertEquals(200, lock("/collection", "Second-60").statusCode());
  }

  @Test
  void periodicCleanupRemovesExpiredCollectionLocks() throws Exception {
    assertEquals(200, lock("/collection", "Second-1").statusCode());
    Thread.sleep(Duration.ofMillis(1100));
    assertDoesNotThrow(servlet::periodicEvent);
    assertEquals(201, put("/collection/allowed.txt").statusCode());
    assertEquals(200, lock("/collection", "Second-60").statusCode());
  }

  private HttpResponse<String> lock(String path, String timeout) throws Exception {
    return send(path, "LOCK", LOCK_BODY, "Depth", "infinity", "Timeout", timeout, "Content-Type", "application/xml");
  }

  private HttpResponse<String> put(String path) throws Exception {
    return send(path, "PUT", "storage-test");
  }

  private HttpResponse<String> send(String path, String method, String body, String... headers) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(baseUri + path))
      .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer storage-test")
      .method(method, HttpRequest.BodyPublishers.ofString(body));
    if (headers.length > 0) {
      request.headers(headers);
    }
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
