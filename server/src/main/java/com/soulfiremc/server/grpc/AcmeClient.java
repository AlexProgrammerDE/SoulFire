/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.grpc;

import com.soulfiremc.server.util.SFHelpers;
import lombok.extern.slf4j.Slf4j;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.toolbox.AcmeUtils;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Scanner;

@Slf4j
public class AcmeClient {
  private static final Path TLS_DIR = Path.of("tls");
  public static final Path USER_KEY_FILE = TLS_DIR.resolve("acme-user.key");
  public static final Path DOMAIN_KEY_FILE = TLS_DIR.resolve("acme-domain.key");
  public static final Path DOMAIN_CHAIN_FILE = TLS_DIR.resolve("acme-domain-chain.crt");

  // Maximum time to wait until VALID/INVALID is expected
  private static final Duration TIMEOUT = Duration.ofMinutes(30);

  public void provisionAcmeCertIfNeeded() {
    if (Boolean.getBoolean("sf.grpc.acme.enabled")) {
      System.setProperty("sf.grpc.tls.enabled", "true");
      System.setProperty("sf.grpc.tls.key", DOMAIN_KEY_FILE.toString());
      System.clearProperty("sf.grpc.tls.key.password");
      System.setProperty("sf.grpc.tls.cert", DOMAIN_CHAIN_FILE.toString());

      try {
        fetchCertificate();
      } catch (IOException | InterruptedException | AcmeException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void fetchCertificate() throws IOException, AcmeException, InterruptedException {
    Files.createDirectories(TLS_DIR);

    // Load the user key file. If there is no key file, create a new one.
    var userKeyPair = loadOrCreateUserKeyPair();

    // Create a session.
    var session = new Session(System.getProperty("sf.grpc.acme.server", "acme://letsencrypt.org"));

    // Get the Account.
    // If there is no account yet, create a new one.
    var acct = findOrRegisterAccount(session, userKeyPair);

    // Load or create a key pair for the domains. This should not be the userKeyPair!
    var domainKeyPair = loadOrCreateDomainKeyPair();

    var domain = System.getProperty("sf.grpc.acme.domain");

    // Order the certificate
    var order = acct.newOrder().domain(domain).create();

    // Perform all required authorizations
    for (var auth : order.getAuthorizations()) {
      authorize(auth);
    }

    // Wait for the order to become READY
    order.waitUntilReady(TIMEOUT);

    // Order the certificate
    order.execute(domainKeyPair);

    // Wait for the order to complete
    var status = order.waitForCompletion(TIMEOUT);
    if (status != Status.VALID) {
      log.error("Order has failed, reason: {}", order.getError()
        .map(Problem::toString)
        .orElse("unknown"));
      throw new AcmeException("Order failed... Giving up.");
    }

    // Get the certificate
    var certificate = order.getCertificate();

    log.info("Success! The certificate for domain {} has been generated!", domain);
    log.info("Certificate URL: {}", certificate.getLocation());

    // Write a combined file containing the certificate and chain.
    try (var fw = Files.newBufferedWriter(DOMAIN_CHAIN_FILE)) {
      certificate.writeCertificate(fw);
    }
  }

  private KeyPair loadOrCreateUserKeyPair() throws IOException {
    if (Files.exists(USER_KEY_FILE)) {
      // If there is a key file, read it
      try (var fr = Files.newBufferedReader(USER_KEY_FILE)) {
        return KeyPairUtils.readKeyPair(fr);
      }
    } else {
      // If there is none, create a new key pair and save it
      var userKeyPair = KeyPairUtils.createKeyPair();
      try (var fw = Files.newBufferedWriter(USER_KEY_FILE)) {
        KeyPairUtils.writeKeyPair(userKeyPair, fw);
      }
      return userKeyPair;
    }
  }

  private KeyPair loadOrCreateDomainKeyPair() throws IOException {
    if (Files.exists(DOMAIN_KEY_FILE)) {
      try (var fr = Files.newBufferedReader(DOMAIN_KEY_FILE)) {
        return KeyPairUtils.readKeyPair(fr);
      }
    } else {
      var domainKeyPair = KeyPairUtils.createKeyPair(4096);
      try (var fw = Files.newBufferedWriter(DOMAIN_KEY_FILE)) {
        KeyPairUtils.writeKeyPair(domainKeyPair, fw);
      }
      return domainKeyPair;
    }
  }

  private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException, IOException {
    // Ask the user to accept the TOS, if server provides us with a link.
    var tos = session.getMetadata().getTermsOfService();
    if (tos.isPresent()) {
      var tosUrl = tos.get();
      var tosHash = AcmeUtils.hexEncode(SFHelpers.md5Hash(tosUrl.toString()));
      var tosFile = TLS_DIR.resolve("tos-%s.txt".formatted(tosHash));
      if (!Files.exists(tosFile)) {
        scannerPromptYes("You agree to: %s".formatted(tosUrl));
        Files.writeString(tosFile, tosUrl.toString());
      }
    }


    var accountBuilder = new AccountBuilder()
      .agreeToTermsOfService()
      .useKeyPair(accountKey);

    var accountEmail = System.getProperty("sf.grpc.acme.email");
    if (accountEmail != null) {
      accountBuilder.addEmail(accountEmail);
    }

    var account = accountBuilder.create(session);
    log.info("Logged into account, URL: {}", account.getLocation());

    return account;
  }

  private void authorize(Authorization auth) throws AcmeException, InterruptedException {
    log.info("Authorization for domain {}", auth.getIdentifier().getDomain());

    // The authorization is already valid. No need to process a challenge.
    if (auth.getStatus() == Status.VALID) {
      return;
    }

    // Find the desired challenge and prepare it.
    var challenge = switch (ChallengeType.valueOf(System.getProperty("sf.grpc.acme.challenge-type"))) {
      case HTTP -> httpChallenge(auth);
      case DNS -> dnsChallenge(auth);
    };

    if (challenge == null) {
      throw new AcmeException("No challenge found");
    }

    // If the challenge is already verified, there's no need to execute it again.
    if (challenge.getStatus() == Status.VALID) {
      return;
    }

    // Now trigger the challenge.
    challenge.trigger();

    // Poll for the challenge to complete.
    var status = challenge.waitForCompletion(TIMEOUT);
    if (status != Status.VALID) {
      log.error("Challenge has failed, reason: {}", challenge.getError()
        .map(Problem::toString)
        .orElse("unknown"));
      throw new AcmeException("Challenge failed... Giving up.");
    }

    log.info("Challenge has been completed. Remember to remove the validation resource.");
    scannerPromptYes("""
      Challenge has been completed.
      You can remove the resource again now.""");
  }

  @SuppressWarnings("HttpUrlsUsage")
  public Challenge httpChallenge(Authorization auth) throws AcmeException {
    // Find a single http-01 challenge
    var challenge = auth.findChallenge(Http01Challenge.class)
      .orElseThrow(() -> new AcmeException("Found no %s challenge, don't know what to do...".formatted(Http01Challenge.TYPE)));

    // Output the challenge, wait for acknowledge...
    log.info("Please create a file in your web server's base directory.");
    log.info("It must be reachable at: http://{}/.well-known/acme-challenge/{}", auth.getIdentifier().getDomain(), challenge.getToken());
    log.info("File name: {}", challenge.getToken());
    log.info("Content: {}", challenge.getAuthorization());
    log.info("The file must not contain any leading or trailing whitespaces or line breaks!");
    log.info("If you're ready, dismiss the dialog...");

    var message = """
      Please create a file in your web server's base directory.

      http://%s/.well-known/acme-challenge/%s

      Content:

      %s""".formatted(auth.getIdentifier().getDomain(), challenge.getToken(), challenge.getAuthorization());
    scannerPromptYes(message);

    return challenge;
  }

  public Challenge dnsChallenge(Authorization auth) throws AcmeException {
    // Find a single dns-01 challenge
    var challenge = auth.findChallenge(Dns01Challenge.TYPE)
      .map(Dns01Challenge.class::cast)
      .orElseThrow(() -> new AcmeException("Found no %s challenge, don't know what to do...".formatted(Dns01Challenge.TYPE)));

    // Output the challenge, wait for acknowledge...
    log.info("Please create a TXT record:");
    log.info("{} IN TXT {}", Dns01Challenge.toRRName(auth.getIdentifier()), challenge.getDigest());
    log.info("If you're ready, dismiss the dialog...");

    var message = """
      Please create a TXT record:

      %s IN TXT %s""".formatted(Dns01Challenge.toRRName(auth.getIdentifier()), challenge.getDigest());
    scannerPromptYes(message);

    return challenge;
  }

  private void scannerPromptYes(String message) {
    var scanner = new Scanner(System.in);
    log.info(message);
    log.info("Enter 'yes' to continue...");
    if (!scanner.nextLine().equalsIgnoreCase("yes")) {
      log.error("User did not confirm action, exiting...");
      throw new IllegalStateException("User did not confirm action");
    }
  }

  private enum ChallengeType {HTTP, DNS}
}
