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
package com.soulfiremc.server.script.nodes.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.soulfiremc.server.script.*;
import com.soulfiremc.server.util.ReactorHttpHelper;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Network node that performs HTTP requests with full control over method, headers, and body.
public final class WebFetchNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("network.web_fetch")
    .displayName("Web Fetch")
    .category(CategoryRegistry.NETWORK)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("url", "URL", PortType.STRING, "The URL to fetch"),
      PortDefinition.inputWithDefault("method", "Method", PortType.STRING, "\"GET\"", "HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)"),
      PortDefinition.inputWithDefault("headers", "Headers", PortType.STRING, "\"{}\"", "JSON object of headers"),
      PortDefinition.inputWithDefault("body", "Body", PortType.STRING, "\"\"", "Request body (for POST/PUT/PATCH)"),
      PortDefinition.inputWithDefault("timeout", "Timeout", PortType.NUMBER, "30000", "Timeout in milliseconds")
    )
    .addOutputs(
      PortDefinition.output("exec_success", "Success", PortType.EXEC, "Executes on successful request"),
      PortDefinition.output("exec_error", "Error", PortType.EXEC, "Executes on failed request"),
      PortDefinition.output("response", "Response", PortType.STRING, "Response body as string"),
      PortDefinition.output("statusCode", "Status Code", PortType.NUMBER, "HTTP status code"),
      PortDefinition.output("responseHeaders", "Response Headers", PortType.STRING, "Response headers as JSON object"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether request succeeded (2xx status)"),
      PortDefinition.output("errorMessage", "Error Message", PortType.STRING, "Error message if failed")
    )
    .description("Performs HTTP requests with full control over method, headers, and body")
    .icon("globe")
    .color("#06B6D4")
    .addKeywords("http", "fetch", "request", "api", "rest", "web", "url", "get", "post")
    .build();

  private static final Gson GSON = new Gson();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var url = getStringInput(inputs, "url", "");
    var method = getStringInput(inputs, "method", "GET").toUpperCase();
    var headersJson = getStringInput(inputs, "headers", "{}");
    var body = getStringInput(inputs, "body", "");
    var timeout = getLongInput(inputs, "timeout", 30000L);

    if (url.isEmpty()) {
      return completed(results(
        "exec_error", true,
        "response", "",
        "statusCode", 0,
        "responseHeaders", "{}",
        "success", false,
        "errorMessage", "URL is required"
      ));
    }

    // Parse headers
    JsonObject headers;
    try {
      headers = JsonParser.parseString(headersJson).getAsJsonObject();
    } catch (Exception _) {
      headers = new JsonObject();
    }

    var hasBody = !body.isEmpty();
    var client = ReactorHttpHelper.createReactorClient(null, hasBody);

    // Add custom headers
    final JsonObject finalHeaders = headers;
    client = client.headers(h -> {
      for (var entry : finalHeaders.entrySet()) {
        h.set(entry.getKey(), entry.getValue().getAsString());
      }
    });

    // Set timeout
    client = client.responseTimeout(Duration.ofMillis(timeout));

    // Create request based on method
    HttpClient.ResponseReceiver<?> request = switch (method) {
      case "POST" -> hasBody
        ? client.post().uri(url).send(Mono.just(Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8))))
        : client.post().uri(url);
      case "PUT" -> hasBody
        ? client.put().uri(url).send(Mono.just(Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8))))
        : client.put().uri(url);
      case "DELETE" -> client.delete().uri(url);
      case "PATCH" -> hasBody
        ? client.patch().uri(url).send(Mono.just(Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8))))
        : client.patch().uri(url);
      case "HEAD" -> client.head().uri(url);
      case "OPTIONS" -> client.options().uri(url);
      default -> client.get().uri(url); // GET
    };

    return request
      .responseSingle((resp, buf) -> {
        var statusCode = resp.status().code();
        var responseHeadersObj = new JsonObject();
        resp.responseHeaders().forEach(entry ->
          responseHeadersObj.addProperty(entry.getKey(), entry.getValue()));
        var isSuccess = statusCode >= 200 && statusCode < 300;

        return buf.asString(StandardCharsets.UTF_8)
          .defaultIfEmpty("")
          .map(responseBody -> results(
            isSuccess ? "exec_success" : "exec_error", true,
            "response", responseBody,
            "statusCode", statusCode,
            "responseHeaders", GSON.toJson(responseHeadersObj),
            "success", isSuccess,
            "errorMessage", ""
          ));
      })
      .onErrorResume(e -> Mono.just(results(
        "exec_error", true,
        "response", "",
        "statusCode", 0,
        "responseHeaders", "{}",
        "success", false,
        "errorMessage", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
      )))
      .toFuture();
  }
}
