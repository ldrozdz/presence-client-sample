package com.symphony.client.presence;

import com.symphony.client.Api;
import com.symphony.client.Client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.symphonyoss.symphony.jcurl.JCurl;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
public class PresenceFeedClient {

  private static final String[] STATUSES = new String[] {"AVAILABLE", "AWAY", "BUSY", "ON_THE_PHONE", "BE_RIGHT_BACK",
      "IN_A_MEETING", "OUT_OF_OFFICE", "OFF_WORK", "OFFLINE"};
  private static final Random RND = new Random();

  private final String[] users;
  private final Api api;
  private final String feedId;

  public PresenceFeedClient(Client client, String[] users) throws IOException, CertificateParsingException {
    this.users = users;

    client.auth();
    this.api = new Api(client);

    JCurl.Response response = api.createPresenceFeed();
    checkResponse(response);
    this.feedId = response.getTag("id");

    log.info("Feed id: {}", feedId);
  }

  private Presence setPresence(String userId, String status) throws IOException, CertificateParsingException {
    JCurl.Response response = api.setPresence(userId, status);
    checkResponse(response);
    return new Presence(response.getJsonNode());
  }


  private List<Presence> readPresenceFeed() throws IOException, CertificateParsingException {
    JCurl.Response response = api.readPresenceFeed(feedId);
    checkResponse(response);
    List<Presence> presences = new ArrayList<>();
    for (JsonNode node : response.getJsonNode()) {
      presences.add(new Presence(node));
    }
    return presences;
  }

  private void checkResponse(JCurl.Response response) throws InternalError {
    int status = response.getResponseCode();
    if (status != 200) {
      log.error(response.getOutput());
      throw new InternalError("Response returned code " + status);
    }
  }

  private static String getRandomElement(String[] array) {
    int idx = RND.nextInt(array.length);
    return array[idx];
  }

  private void warmup(int iterations) throws IOException, CertificateParsingException {
    log.info("===== Warmup =====");

    for (int i = 0; i < iterations; i++) {
      setPresence(getRandomElement(users), getRandomElement(STATUSES));
    }

    readPresenceFeed();
  }

  private void test(int iterations, int maxSetPresenceEvents)
      throws IOException, CertificateParsingException, InterruptedException {
    log.info("===== Test =====");

    for (int i = 0; i < iterations; i++) {
      log.info("--- Iteration {} ---", i + 1);
      Map<Long, Presence> testData = new HashMap<>();

      for (int j = 0; j < RND.nextInt(maxSetPresenceEvents); j++) {
        String user = getRandomElement(users);
        String status = getRandomElement(STATUSES);

        Presence p = setPresence(user, status);
        log.info("[{}] SET user {} to {}", p.getTimestamp(), p.getUserId(), p.getStatus());
        testData.put(p.getUserId(), p);
      }

      Thread.sleep(3000);

      for (Presence p : readPresenceFeed()) {
        log.info("GET feed {}: User {} was {} at {}", feedId, p.getUserId(), p.getStatus(), p.getTimestamp());
        Presence savedPresence = testData.get(p.getUserId());
        if (!p.equals(savedPresence)) {
          log.warn("Presence mismatch! Wanted {}, got {}", savedPresence, p);
        }
      }
    }
  }

  public static void main(String[] args) throws IOException, CertificateParsingException, InterruptedException {

    String podUrl = "https://nexus1-2.symphony.com";
    String agentUrl = "https://nexus1-2.symphony.com";
    String sessionAuthUrl = "https://sym-nexus1-dev-chat-glb-3-ause1-all.symphony.com:8444";
    String keyAuthUrl = "https://sym-nexus1-dev-chat-glb-3-ause1-all.symphony.com:8444";
    String certFile = "/home/lukasz/Projects/atlas/agent/bot.user1.p12";
    String certPassword = "changeit";

    String[] users = new String[] {"9414568312885", "9414568312912", "9414568314259", "9414568314266", "9414568314273",
        "9414568312920", "9414568312907", "9414568312926", "9414568314256"};

    Client apiClient = new Client(podUrl, agentUrl, sessionAuthUrl, keyAuthUrl, certFile, certPassword);
    PresenceFeedClient presenceClient = new PresenceFeedClient(apiClient, users);

    int warmupIterations = 100;
    int testIterations = 10;
    int maxSetPresenceEvents = 20;

    // ***** Main logic *****
    presenceClient.warmup(warmupIterations);
    presenceClient.test(testIterations, maxSetPresenceEvents);
  }

}
