package org.example.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recognitions")
public class Recognition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognition_type_id")
    private RecognitionType recognitionType;

    @Column(name = "award_name")
    private String awardName;

    @Column(name = "level")
    private String level;

    @Column(name = "recipient_id", insertable = false, updatable = false)
    private Long recipientId;

    @Column(name = "sender_id", insertable = false, updatable = false)
    private Long senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Employee recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Employee sender;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "award_points")
    private Integer awardPoints;

    @Column(name = "approval_status")
    private String approvalStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at")
    private Instant createdAt;

    public Recognition() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public RecognitionType getRecognitionType() { return recognitionType; }
    public void setRecognitionType(RecognitionType recognitionType) { this.recognitionType = recognitionType; }

    public String getAwardName() { return awardName; }
    public void setAwardName(String awardName) { this.awardName = awardName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Employee getRecipient() { return recipient; }
    public void setRecipient(Employee recipient) { this.recipient = recipient; }

    public Employee getSender() { return sender; }
    public void setSender(Employee sender) { this.sender = sender; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getAwardPoints() { return awardPoints; }
    public void setAwardPoints(Integer awardPoints) { this.awardPoints = awardPoints; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
