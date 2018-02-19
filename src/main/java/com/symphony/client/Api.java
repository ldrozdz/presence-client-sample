package com.symphony.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.symphony.jcurl.JCurl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.CertificateParsingException;

@Slf4j
public class Api {
  private static final Logger LOG = LoggerFactory.getLogger(Api.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String SESSIONINFO = "/pod/v2/sessioninfo";
  private static final String USER_CREATE = "/pod/v1/admin/user/create";
  private static final String SESSIONAUTH = "/sessionauth/v1/authenticate";
  private static final String KEYAUTH = "/keyauth/v1/authenticate";
  private static final String USERS_PRESENCE = "/pod/v2/users/presence";
  private static final String USER_UID_PRESENCE = "/pod/v3/user/%s/presence";
  private static final String SESSION_TOKEN = "sessionToken";
  private static final String USER_PRESENCE = "/pod/v2/user/presence";
  private static final String V3_USER_PRESENCE = "/pod/v3/user/presence";
  private static final String PRESENCE_FEED_CREATE = "/pod/v1/presence/feed/create";
  private static final String PRESENCE_FEED_READ = "%s/pod/v1/presence/feed/%s/read";
  private static final String USER_INFO = "/pod/v2/user";
  
  private Client client;
  
  public Api(Client client) {
   this.client = client;
  }

  public JCurl.Response sessionAuth() throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .keystore(client.getCert())
        .storepass(client.getPassword())
        .storetype("pkcs12")
        .extract("skey", "token")
        .url(client.getSessionAuthUrl() + SESSIONAUTH)
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

  public JCurl.Response keyAuth() throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .keystore(client.getCert())
        .storepass(client.getPassword())
        .storetype("pkcs12")
        .extract("kmtoken", "token")
        .url(client.getKeyAuthUrl() + KEYAUTH)
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

  public JCurl.Response sessionInfo() throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.GET)
        .header(SESSION_TOKEN, client.getSessionToken())
        .extract("uid", "id")
        .url(client.getPodUrl() + SESSIONINFO)
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

  public JCurl.Response createUser(String firstName, String lastName, String userName, String displayName, String email,
      boolean isPerson) throws IOException, CertificateParsingException {
    ObjectNode data = new ObjectNode(JsonNodeFactory.instance);

    data.with("userAttributes")
        .put("emailAddress", email)
        .put("displayName", displayName)
        .put("userName", userName)
        .put("accountType", (isPerson) ? "NORMAL" : "SYSTEM");

    if (isPerson) {
      data.with("userAttributes")
          .put("firstName", firstName)
          .put("lastName", lastName);

      data.with("password")
          .put("hSalt", "IbMZa1uczMadRerbbBdZDA==")
          .put("hPassword", "4Gpflt9U+mDQjhdPCQ9RRFEw45UCc0xDoPAX3ZZwycg=")
          .put("khSalt", "ht5RSh7Hb5/Xp2z1bfagcA==")
          .put("khPassword", "rvki/h72kG2plS5kl1OtoGjXVF97cccSLIjX4eP/VdE=");
    }

    if (!isPerson) {
      data.withArray("roles").add("INDIVIDUAL");
    }

    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.GET)
        .header(SESSION_TOKEN, client.getSessionToken())
        .data(MAPPER.writeValueAsString(data))
        .extract("uid", "userSystemInfo.id")
        .url(client.getPodUrl() + USER_CREATE)
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    return jcurl.processResponse(connection);
  }

  public JCurl.Response getUserInfoById(String userId, Boolean local) throws IOException, CertificateParsingException {
    JCurl.Builder builder = JCurl.builder()
        .method(JCurl.HttpMethod.GET)
        .header(SESSION_TOKEN, client.getSessionToken())
        .query("uid", userId)
        .url(client.getPodUrl() + String.format(USER_INFO, userId));

    if (local != null) {
      builder.query("local", String.valueOf(local));
    }

    JCurl jcurl = builder.build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    return jcurl.processResponse(connection);
  }

  public JCurl.Response getAllPresence(long lastUserId, int limit) throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.GET)
        .header(SESSION_TOKEN, client.getSessionToken())
        .extract("category")
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect(client.getPodUrl() + USERS_PRESENCE
        + "?lastUserId=" + lastUserId + "&limit=" + limit);
    JCurl.Response response = jcurl.processResponse(connection);
    return response;
  }


  public JCurl.Response getPresence(String userId) throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.GET)
        .header(SESSION_TOKEN, client.getSessionToken())
        .extract("status", "category")
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect(client.getPodUrl() + String.format(USER_UID_PRESENCE, userId));
    JCurl.Response response = jcurl.processResponse(connection);
    return response;
  }

  public JCurl.Response setOwnPresence(String status) throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .header(SESSION_TOKEN, client.getSessionToken())
        .data("{\"category\": \"" + status + "\"}")
        .extract("status", "category")
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect(client.getPodUrl() + USER_PRESENCE);
    JCurl.Response response = jcurl.processResponse(connection);

    LOG.debug(jcurl.toString());

    return response;
  }

  public JCurl.Response setPresence(String userId, String status) throws IOException, CertificateParsingException {
    ObjectNode data = new ObjectNode(JsonNodeFactory.instance);
    data.put("userId", userId);
    data.put("category", status);

    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .header(SESSION_TOKEN, client.getSessionToken())
        .data(MAPPER.writeValueAsString(data))
        .extract("status", "category")
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect(client.getPodUrl() + V3_USER_PRESENCE);
    JCurl.Response response = jcurl.processResponse(connection);
    return response;
  }

  public JCurl.Response createPresenceFeed() throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .header(SESSION_TOKEN, client.getSessionToken())
        .header("keyManagerToken", client.getKeyManagerToken())
        .extract("id", "id")
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect(client.getPodUrl() + PRESENCE_FEED_CREATE);
    JCurl.Response response = jcurl.processResponse(connection);
    return response;
  }

  public JCurl.Response readPresenceFeed(String feedId) throws IOException, CertificateParsingException {
    JCurl jcurl = JCurl.builder()
        .method(JCurl.HttpMethod.GET)
        .header(SESSION_TOKEN, client.getSessionToken())
        .header("keyManagerToken", client.getKeyManagerToken())
        .build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect(String.format(PRESENCE_FEED_READ, client.getPodUrl(), feedId));
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

}
