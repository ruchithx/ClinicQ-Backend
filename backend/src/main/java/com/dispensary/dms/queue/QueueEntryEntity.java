package com.dispensary.dms.queue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** A booking / position in a queue session (table {@code queue_entries}). */
@Entity
@Table(name = "queue_entries")
public class QueueEntryEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "queue_session_id", nullable = false)
    private UUID queueSessionId;

    @Column(name = "queue_number", nullable = false)
    private int queueNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueEntryStatus status = QueueEntryStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private BookingChannel channel;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "notes")
    private String notes;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    @Column(name = "booked_by", nullable = false)
    private UUID bookedBy;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "called_by")
    private UUID calledBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "skipped_at")
    private Instant skippedAt;

    @Column(name = "skipped_by")
    private UUID skippedBy;

    @Column(name = "skip_reason")
    private String skipReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "requeue_count", nullable = false)
    private short requeueCount;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected QueueEntryEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQueueSessionId() {
        return queueSessionId;
    }

    public void setQueueSessionId(UUID queueSessionId) {
        this.queueSessionId = queueSessionId;
    }

    public int getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(int queueNumber) {
        this.queueNumber = queueNumber;
    }

    public QueueEntryStatus getStatus() {
        return status;
    }

    public void setStatus(QueueEntryStatus status) {
        this.status = status;
    }

    public BookingChannel getChannel() {
        return channel;
    }

    public void setChannel(BookingChannel channel) {
        this.channel = channel;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(Instant bookedAt) {
        this.bookedAt = bookedAt;
    }

    public UUID getBookedBy() {
        return bookedBy;
    }

    public void setBookedBy(UUID bookedBy) {
        this.bookedBy = bookedBy;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Instant calledAt) {
        this.calledAt = calledAt;
    }

    public UUID getCalledBy() {
        return calledBy;
    }

    public void setCalledBy(UUID calledBy) {
        this.calledBy = calledBy;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public UUID getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(UUID completedBy) {
        this.completedBy = completedBy;
    }

    public Instant getSkippedAt() {
        return skippedAt;
    }

    public void setSkippedAt(Instant skippedAt) {
        this.skippedAt = skippedAt;
    }

    public UUID getSkippedBy() {
        return skippedBy;
    }

    public void setSkippedBy(UUID skippedBy) {
        this.skippedBy = skippedBy;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(UUID cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public short getRequeueCount() {
        return requeueCount;
    }

    public void setRequeueCount(short requeueCount) {
        this.requeueCount = requeueCount;
    }

    public Instant getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(Instant anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
