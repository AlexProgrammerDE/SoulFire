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
package com.soulfiremc.server.script.nodes.flow;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/// Flow control node that limits execution rate using a token bucket algorithm.
public final class RateLimitNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.rate_limit")
    .displayName("Rate Limit")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("key", "Key", PortType.STRING, "\"default\"", "Unique key for this rate limiter"),
      PortDefinition.inputWithDefault("maxTokens", "Max Tokens", PortType.NUMBER, "10", "Maximum tokens in bucket"),
      PortDefinition.inputWithDefault("refillRate", "Refill Rate", PortType.NUMBER, "1", "Tokens added per second"),
      PortDefinition.inputWithDefault("tokensRequired", "Tokens Required", PortType.NUMBER, "1", "Tokens consumed per execution")
    )
    .addOutputs(
      PortDefinition.output("exec_allowed", "Allowed", PortType.EXEC, "Execution path if allowed"),
      PortDefinition.output("exec_denied", "Denied", PortType.EXEC, "Execution path if rate limited"),
      PortDefinition.output("wasAllowed", "Was Allowed", PortType.BOOLEAN, "Whether execution was allowed"),
      PortDefinition.output("tokensRemaining", "Tokens Remaining", PortType.NUMBER, "Tokens left in bucket"),
      PortDefinition.output("retryAfterMs", "Retry After (ms)", PortType.NUMBER, "Milliseconds until tokens available")
    )
    .description("Limits execution rate using a token bucket algorithm")
    .icon("gauge")
    .color("#8B5CF6")
    .addKeywords("rate", "limit", "throttle", "bucket", "spam", "cooldown")
    .build();

  private static final Map<String, TokenBucket> BUCKETS = new ConcurrentHashMap<>();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var key = getStringInput(inputs, "key", "default");
    var maxTokens = getDoubleInput(inputs, "maxTokens", 10.0);
    var refillRate = getDoubleInput(inputs, "refillRate", 1.0);
    var tokensRequired = getDoubleInput(inputs, "tokensRequired", 1.0);

    var bucket = BUCKETS.computeIfAbsent(key, _ -> new TokenBucket(maxTokens, refillRate));

    // Update bucket parameters if changed
    bucket.updateParams(maxTokens, refillRate);

    var result = bucket.tryConsume(tokensRequired);

    return completed(results(
      "wasAllowed", result.allowed,
      "tokensRemaining", result.tokensRemaining,
      "retryAfterMs", result.retryAfterMs
    ));
  }

  private static class TokenBucket {
    private double tokens;
    private long lastRefillTime;
    private double maxTokens;
    private double refillRate;

    TokenBucket(double maxTokens, double refillRate) {
      this.tokens = maxTokens;
      this.maxTokens = maxTokens;
      this.refillRate = refillRate;
      this.lastRefillTime = System.currentTimeMillis();
    }

    synchronized void updateParams(double maxTokens, double refillRate) {
      this.maxTokens = maxTokens;
      this.refillRate = refillRate;
    }

    synchronized ConsumeResult tryConsume(double required) {
      refill();

      if (tokens >= required) {
        tokens -= required;
        return new ConsumeResult(true, tokens, 0);
      }

      // Calculate time until enough tokens are available
      var deficit = required - tokens;
      var msUntilAvailable = refillRate > 0 ? (long) (deficit / refillRate * 1000) : Long.MAX_VALUE;

      return new ConsumeResult(false, tokens, msUntilAvailable);
    }

    private void refill() {
      var now = System.currentTimeMillis();
      var elapsed = (now - lastRefillTime) / 1000.0;
      tokens = Math.min(maxTokens, tokens + elapsed * refillRate);
      lastRefillTime = now;
    }
  }

  private record ConsumeResult(boolean allowed, double tokensRemaining, long retryAfterMs) {}
}
