package com.symphony.client;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.symphony.jcurl.JCurl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Paths;
import java.security.cert.CertificateParsingException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class Client {
  private static final Logger LOG = LoggerFactory.getLogger(Client.class);
  private static final String SESSION_TOKEN = "sessionToken";
  private static final String KEY_MANAGER_TOKEN = "keyManagerToken";

  private final String password;
  private final String cert;
  private final String podUrl;
  private final String agentUrl;
  private final String sessionAuthUrl;
  private final String keyAuthUrl;
  private String sessionToken;
  private String keyManagerToken;
  private String appToken;
  private String userId;
  private Api api;

  public Client(String podUrl, String agentUrl, String sessionAuthUrl, String keyAuthUrl, String cert, String password) {
    this.podUrl = podUrl;
    this.agentUrl = agentUrl;
    this.sessionAuthUrl = sessionAuthUrl;
    this.keyAuthUrl = keyAuthUrl;
    this.cert = cert;
    this.password = password;
    this.api = new Api(this);
  }

  public static Client fromConfig(String configPath, String certName, String certPassword) throws IOException {
    Properties config = new Properties();
    try (InputStream is = Client.class.getResourceAsStream(configPath)) {
      config.load(is);
    }

    String cert = Paths.get(config.getProperty("CERT_PATH"), certName).toString();
    String podUrl = config.getProperty("POD_URL");
    String agentUrl = config.getProperty("AGENT_URL");
    String sessionAuthUrl = config.getProperty("SESSION_AUTH_URL");
    String keyAuthUrl = config.getProperty("KEY_AUTH_URL");

    return new Client(podUrl, agentUrl, sessionAuthUrl, keyAuthUrl, cert, certPassword);
  }

  public void auth() throws IOException, CertificateParsingException {
    this.sessionToken = api.sessionAuth().getTag("skey");
    this.keyManagerToken = api.keyAuth().getTag("kmtoken");
    this.userId = api.sessionInfo().getTag("uid");
  }

  public JCurl.Response doGet(String url, Map<String, String> query) throws IOException, CertificateParsingException {
    JCurl.Builder builder = JCurl.builder()
        .method(JCurl.HttpMethod.GET)
        .cookie("skey", sessionToken)
        .cookie("kmtoken", keyManagerToken)
        .header(SESSION_TOKEN, sessionToken)
        .header(KEY_MANAGER_TOKEN, keyManagerToken)
        .header("x-symphony-csrf-token", UUID.randomUUID().toString())
        .url(url);

    if (query != null) {
      for (Map.Entry<String, String> entry : query.entrySet()) {
        builder.query(entry.getKey(), entry.getValue());
      }
    }

    JCurl jcurl = builder.build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

  public JCurl.Response doPost(String url, Map<String, String> params) throws IOException, CertificateParsingException {
    JCurl.Builder builder = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .cookie("skey", sessionToken)
        .cookie("kmtoken", keyManagerToken)
        .header(SESSION_TOKEN, sessionToken)
        .header(KEY_MANAGER_TOKEN, keyManagerToken)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("x-symphony-csrf-token", UUID.randomUUID().toString())
        .expect(400)
        .url(url);

    if (params != null) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        builder.form(entry.getKey(), entry.getValue());
      }
    }

    JCurl jcurl = builder.build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

  public JCurl.Response doPost(String url, String data) throws IOException, CertificateParsingException {
    JCurl.Builder builder = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .cookie("skey", sessionToken)
        .cookie("kmtoken", keyManagerToken)
        .header(SESSION_TOKEN, sessionToken)
        .header(KEY_MANAGER_TOKEN, keyManagerToken)
        .header("Content-Type", "application/json")
        .header("x-symphony-csrf-token", UUID.randomUUID().toString())
        .expect(400)
        .url(url);

    if (data != null) {
      builder.data(data);
    }

    JCurl jcurl = builder.build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

  public JCurl.Builder buildPost(String url) {
    JCurl.Builder builder = JCurl.builder()
        .method(JCurl.HttpMethod.POST)
        .cookie("skey", sessionToken)
        .cookie("kmtoken", keyManagerToken)
        .header(SESSION_TOKEN, sessionToken)
        .header(KEY_MANAGER_TOKEN, keyManagerToken)
        .header("x-symphony-csrf-token", UUID.randomUUID().toString())
        .url(url);

    return builder;
  }

  public JCurl.Response process(JCurl.Builder builder) throws IOException, CertificateParsingException {
    JCurl jcurl = builder.build();

    LOG.debug(jcurl.toString());

    HttpURLConnection connection = jcurl.connect();
    JCurl.Response response = jcurl.processResponse(connection);

    return response;
  }

  public String getSessionToken() {
    return sessionToken;
  }

  public String getKeyManagerToken() {
    return keyManagerToken;
  }

  public String getUserId() {
    return userId;
  }

  public String getAppToken() {
    return appToken;
  }

  public String getPassword() {
    return password;
  }

  public String getCert() {
    return cert;
  }

  public String getPodUrl() {
    return podUrl;
  }

  public String getAgentUrl() {
    return agentUrl;
  }

  public String getSessionAuthUrl() {
    return sessionAuthUrl;
  }

  public String getKeyAuthUrl() {
    return keyAuthUrl;
  }

  public Api getApi() {
    return api;
  }

  public void setAuth(String skey, String kmToken) throws IOException, CertificateParsingException {
    this.sessionToken = skey;
    this.keyManagerToken = kmToken;
    this.userId = api.sessionInfo().getTag("uid");
  }

  public void setSessionToken(String sessionToken) {
    this.sessionToken = sessionToken;
  }

  public void setKeyManagerToken(String keyManagerToken) {
    this.keyManagerToken = keyManagerToken;
  }

  public void setAppToken(String appToken) {
    this.appToken = appToken;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
}
