package com.dispensary.dms.queue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A numbered queue session (table {@code queue_sessions}). */
@Entity
@Table(name = "queue_sessions")
public class QueueSessionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "assigned_doctor_id")
    private UUID assignedDoctorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueSessionStatus status = QueueSessionStatus.SCHEDULED;

    @Column(name = "last_issued_number", nullable = false)
    private int lastIssuedNumber;

    @Column(name = "last_called_number")
    private Integer lastCalledNumber;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "opened_by")
    private UUID openedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected QueueSessionEntity() {
    }

    /** Bump updatedAt so the optimistic @Version and public ETag advance on every mutation. */
    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getAssignedDoctorId() {
        return assignedDoctorId;
    }

    public void setAssignedDoctorId(UUID assignedDoctorId) {
        this.assignedDoctorId = assignedDoctorId;
    }

    public QueueSessionStatus getStatus() {
        return status;
    }

    public void setStatus(QueueSessionStatus status) {
        this.status = status;
    }

    public int getLastIssuedNumber() {
        return lastIssuedNumber;
    }

    public void setLastIssuedNumber(int lastIssuedNumber) {
        this.lastIssuedNumber = lastIssuedNumber;
    }

    public Integer getLastCalledNumber() {
        return lastCalledNumber;
    }

    public void setLastCalledNumber(Integer lastCalledNumber) {
        this.lastCalledNumber = lastCalledNumber;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public UUID getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(UUID openedBy) {
        this.openedBy = openedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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
