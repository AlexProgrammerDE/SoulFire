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
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.common;

import com.google.common.collect.Sets;
import com.linecorp.armeria.common.annotation.Nullable;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/// HTTP request method.
public enum HttpMethod {

  // Forked from Netty 4.1.34 at ff7484864b1785103cbc62845ff3a392c93822b7
  // Changes:
  // - Switched to enum for type
  // - Added additional methods from https://www.iana.org/assignments/http-methods/http-methods.xhtml
  // - Added safe methods

  /// Special HTTP method that is not defined in HTTP/1.1.
  ACL(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  BASELINE_CONTROL(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  BIND(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  CHECKIN(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  CHECKOUT(Type.IDEMPOTENT),
  /// The CONNECT method which is used for a proxy that can dynamically switch to being a tunnel or for
  /// <a href="https://datatracker.ietf.org/doc/rfc8441/">bootstrapping WebSockets with HTTP/2</a>.
  /// Note that Armeria handles a `CONNECT` request only for bootstrapping WebSockets.
  CONNECT(Type.NORMAL),
  /// Special HTTP method that is not defined in HTTP/1.1.
  COPY(Type.IDEMPOTENT),
  /// The DELETE method which requests that the origin server delete the resource identified by the
  /// Request-URI.
  DELETE(Type.IDEMPOTENT),
  /// The GET method which means retrieve whatever information (in the form of an entity) is identified
  /// by the Request-URI.  If the Request-URI refers to a data-producing process, it is the
  /// produced data which shall be returned as the entity in the response and not the source text
  /// of the process, unless that text happens to be the output of the process.
  GET(Type.SAFE),
  /// The HEAD method which is identical to GET except that the server MUST NOT return a message-body
  /// in the response.
  HEAD(Type.SAFE),
  /// Special HTTP method that is not defined in HTTP/1.1.
  LABEL(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  LINK(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  LOCK(Type.NORMAL),
  /// Special HTTP method that is not defined in HTTP/1.1.
  MERGE(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  MKACTIVITY(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  MKCALENDAR(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  MKCOL(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  MKREDIRECTREF(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  MKWORKSPACE(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  MOVE(Type.IDEMPOTENT),
  /// The OPTIONS method which represents a request for information about the communication options
  /// available on the request/response chain identified by the Request-URI. This method allows
  /// the client to determine the options and/or requirements associated with a resource, or the
  /// capabilities of a server, without implying a resource action or initiating a resource
  /// retrieval.
  OPTIONS(Type.SAFE),
  /// Special HTTP method that is not defined in HTTP/1.1.
  ORDERPATCH(Type.IDEMPOTENT),
  /// The PATCH method which requests that a set of changes described in the
  /// request entity be applied to the resource identified by the Request-URI.
  PATCH(Type.NORMAL),
  /// The POST method which is used to request that the origin server accept the entity enclosed in the
  /// request as a new subordinate of the resource identified by the Request-URI in the
  /// Request-Line.
  POST(Type.NORMAL),
  /// Special HTTP method that is not defined in HTTP/1.1.
  PRI(Type.SAFE),
  /// Special HTTP method that is not defined in HTTP/1.1.
  PROPFIND(Type.SAFE),
  /// Special HTTP method that is not defined in HTTP/1.1.
  PROPPATCH(Type.IDEMPOTENT),
  /// The PUT method which requests that the enclosed entity be stored under the supplied Request-URI.
  PUT(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  REBIND(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  REPORT(Type.SAFE),
  /// Special HTTP method that is not defined in HTTP/1.1.
  SEARCH(Type.SAFE),
  /// The TRACE method which is used to invoke a remote, application-layer loop-back of the request
  /// message.
  TRACE(Type.SAFE),
  /// Special HTTP method that is not defined in HTTP/1.1.
  UNBIND(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  UNCHECKOUT(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  UNLINK(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  UNLOCK(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  UPDATE(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  UPDATEREDIRECTREF(Type.IDEMPOTENT),
  /// Special HTTP method that is not defined in HTTP/1.1.
  VERSION_CONTROL(Type.IDEMPOTENT),
  /// A special constant returned by [#method()] to signify that a request has a method
  /// not defined in this enum.
  UNKNOWN(Type.UNKNOWN);

  private static final Set<HttpMethod> knownMethods; // ImmutableEnumSet
  private static final Set<HttpMethod> safeMethods;
  private static final Set<HttpMethod> idempotentMethods;

  static {
    final Set<HttpMethod> allMethods = EnumSet.allOf(HttpMethod.class);
    allMethods.remove(UNKNOWN);
    knownMethods = Sets.immutableEnumSet(allMethods);
    final Set<HttpMethod> safeMethodsBuilder = EnumSet.noneOf(HttpMethod.class);
    for (HttpMethod method : knownMethods) {
      if (method.type == Type.SAFE) {
        safeMethodsBuilder.add(method);
      }
    }
    safeMethods = Sets.immutableEnumSet(safeMethodsBuilder);
    final Set<HttpMethod> idempotentMethodsBuilder = EnumSet.noneOf(HttpMethod.class);
    for (HttpMethod method : knownMethods) {
      if (method.type == Type.SAFE || method.type == Type.IDEMPOTENT) {
        idempotentMethodsBuilder.add(method);
      }
    }
    idempotentMethods = Sets.immutableEnumSet(idempotentMethodsBuilder);
  }

  private final Type type;

  HttpMethod(Type type) {
    this.type = type;
  }

  /// Returns whether the specified [String] is one of the supported method names.
  ///
  /// @return `true` if supported. `false` otherwise.
  public static boolean isSupported(String value) {
    requireNonNull(value, "value");
    return switch (value) {
      case "ACL", "BASELINE-CONTROL", "BIND", "CHECKIN", "CHECKOUT", "CONNECT", "COPY", "DELETE", "GET", "HEAD", "LABEL", "LINK", "LOCK", "MERGE", "MKACTIVITY", "MKCALENDAR", "MKCOL", "MKREDIRECTREF", "MKWORKSPACE", "MOVE", "OPTIONS", "ORDERPATCH", "PATCH", "POST", "PRI", "PROPFIND", "PROPPATCH", "PUT", "REBIND", "REPORT", "SEARCH", "TRACE", "UNBIND", "UNCHECKOUT", "UNLINK", "UNLOCK", "UPDATE", "UPDATEREDIRECTREF", "VERSION-CONTROL" -> true;
      default -> false;
    };

  }

  /// Returns the <a href="https://developer.mozilla.org/en-US/docs/Glossary/Idempotent">idempotent</a>
  /// HTTP methods.
  public static Set<HttpMethod> idempotentMethods() {
    return idempotentMethods;
  }

  /// Returns the <a href="https://developer.mozilla.org/en-US/docs/Glossary/Safe">safe</a>
  /// HTTP methods.
  public static Set<HttpMethod> safeMethods() {
    return safeMethods;
  }

  /// Returns all [HttpMethod]s except [#UNKNOWN].
  public static Set<HttpMethod> knownMethods() {
    return knownMethods;
  }

  /// Parses the specified [String] into an [HttpMethod]. This method will return the same
  /// [HttpMethod] instance for equal values of `method`. Note that this method will not
  /// treat `"UNKNOWN"` as a valid value and thus will return `null` when `"UNKNOWN"`
  /// is given.
  ///
  /// @return `null` if there is no such [HttpMethod] available
  @Nullable
  public static HttpMethod tryParse(@Nullable String method) {
    if (method == null) {
      return null;
    }

    return switch (method) {
      case "ACL" -> ACL;
      case "BASELINE-CONTROL" -> BASELINE_CONTROL;
      case "BIND" -> BIND;
      case "CHECKIN" -> CHECKIN;
      case "CHECKOUT" -> CHECKOUT;
      case "CONNECT" -> CONNECT;
      case "COPY" -> COPY;
      case "DELETE" -> DELETE;
      case "GET" -> GET;
      case "HEAD" -> HEAD;
      case "LABEL" -> LABEL;
      case "LINK" -> LINK;
      case "LOCK" -> LOCK;
      case "MERGE" -> MERGE;
      case "MKACTIVITY" -> MKACTIVITY;
      case "MKCALENDAR" -> MKCALENDAR;
      case "MKCOL" -> MKCOL;
      case "MKREDIRECTREF" -> MKREDIRECTREF;
      case "MKWORKSPACE" -> MKWORKSPACE;
      case "MOVE" -> MOVE;
      case "OPTIONS" -> OPTIONS;
      case "ORDERPATCH" -> ORDERPATCH;
      case "PATCH" -> PATCH;
      case "POST" -> POST;
      case "PRI" -> PRI;
      case "PROPFIND" -> PROPFIND;
      case "PROPPATCH" -> PROPPATCH;
      case "PUT" -> PUT;
      case "REBIND" -> REBIND;
      case "REPORT" -> REPORT;
      case "SEARCH" -> SEARCH;
      case "TRACE" -> TRACE;
      case "UNBIND" -> UNBIND;
      case "UNCHECKOUT" -> UNCHECKOUT;
      case "UNLINK" -> UNLINK;
      case "UNLOCK" -> UNLOCK;
      case "UPDATE" -> UPDATE;
      case "UPDATEREDIRECTREF" -> UPDATEREDIRECTREF;
      case "VERSION-CONTROL" -> VERSION_CONTROL;
      default -> null;
    };
  }

  public enum Type {
    UNKNOWN,
    NORMAL,
    IDEMPOTENT,
    SAFE
  }
}
