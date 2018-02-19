package com.symphony.client.presence;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class Presence {

  private final Long userId;
  private final String status;
  private final Long timestamp;

  public Presence(JsonNode node) {
    userId = node.get("userId").asLong();
    status = node.get("category").asText();
    timestamp = node.get("timestamp").asLong();
  }
}
