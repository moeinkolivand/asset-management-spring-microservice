package com.tutorial.sharedmodule.infra.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "outbox_events",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_aggregate_id",
          columnNames = {"aggregate_id"})
    })
public class OutBox {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column private String aggregateType;
  @Column private String aggregateId;

  @Column private String eventType;
  @Column private String topic;

  @Column(insertable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column private boolean published = false;

  @Column(insertable = false, updatable = true)
  private Instant publishedAt;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "payload", nullable = false, columnDefinition = "BYTEA")
  private byte[] payload;

  public OutBox() {}

  public OutBox(
      String aggregateType,
      String aggregateId,
      String eventType,
      String topic,
      byte[] payload,
      Instant createdAt,
      boolean published,
      Instant publishedAt) {
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.topic = topic;
    this.createdAt = createdAt;
    this.published = published;
    this.publishedAt = publishedAt;
    this.payload = payload;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(String aggregateId) {
    this.aggregateId = aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
  }
}
