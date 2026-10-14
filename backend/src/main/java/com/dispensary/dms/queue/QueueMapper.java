package com.dispensary.dms.queue;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.queue.dto.QueueCounts;
import com.dispensary.dms.queue.dto.QueueEntryRef;
import com.dispensary.dms.queue.dto.QueueEntryResponse;
import com.dispensary.dms.queue.dto.QueueSessionResponse;
import com.dispensary.dms.queue.dto.ServingEntry;
import com.dispensary.dms.user.UserEntity;
import com.dispensary.dms.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Builds queue DTOs, resolving staff references and aggregate counts (spec section 10.7). */
@Component
public class QueueMapper {

    private final QueueEntryRepository entryRepository;
    private final UserRepository userRepository;

    public QueueMapper(QueueEntryRepository entryRepository, UserRepository userRepository) {
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    public QueueSessionResponse toSessionResponse(QueueSessionEntity session) {
        Map<QueueEntryStatus, Integer> counts = new EnumMap<>(QueueEntryStatus.class);
        for (QueueEntryStatus status : QueueEntryStatus.values()) {
            counts.put(status, 0);
        }
        entryRepository.countByStatus(session.getId())
                .forEach(sc -> counts.put(sc.getStatus(), (int) sc.getCount()));

        List<QueueEntryEntity> serving = entryRepository.findServing(session.getId());
        Integer nextWaiting = entryRepository
                .findWaitingNumbers(session.getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);

        Set<UUID> userIds = serving.stream().map(QueueEntryEntity::getCalledBy)
                .filter(java.util.Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        if (session.getAssignedDoctorId() != null) {
            userIds.add(session.getAssignedDoctorId());
        }
        Map<UUID, UserEntity> users = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<ServingEntry> servingEntries = serving.stream()
                .map(e -> new ServingEntry(e.getId(), e.getQueueNumber(), e.getPatientName(),
                        e.getCalledAt(), staffRef(users.get(e.getCalledBy()))))
                .toList();

        return new QueueSessionResponse(
                session.getId(), session.getSessionDate(), session.getName(), session.getStatus(),
                staffRef(users.get(session.getAssignedDoctorId())),
                session.getLastIssuedNumber(), session.getLastCalledNumber(), nextWaiting,
                new QueueCounts(counts.get(QueueEntryStatus.WAITING), counts.get(QueueEntryStatus.SERVING),
                        counts.get(QueueEntryStatus.COMPLETED), counts.get(QueueEntryStatus.SKIPPED),
                        counts.get(QueueEntryStatus.CANCELLED)),
                servingEntries, session.getOpenedAt(), session.getClosedAt(),
                session.getCreatedAt(), session.getUpdatedAt(), session.getVersion());
    }

    public QueueEntryResponse toEntryResponse(QueueEntryEntity entry) {
        return toEntryResponses(List.of(entry)).get(0);
    }

    public List<QueueEntryResponse> toEntryResponses(List<QueueEntryEntity> entries) {
        Set<UUID> userIds = new HashSet<>();
        for (QueueEntryEntity e : entries) {
            userIds.add(e.getBookedBy());
            if (e.getCalledBy() != null) {
                userIds.add(e.getCalledBy());
            }
        }
        Map<UUID, UserEntity> users = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return entries.stream().map(e -> new QueueEntryResponse(
                e.getId(), e.getQueueSessionId(), e.getQueueNumber(), e.getStatus(), e.getChannel(),
                e.getPatientName(), e.getContactPhone(), e.getNotes(), e.getBookedAt(),
                staffRef(users.get(e.getBookedBy())), e.getCalledAt(), staffRef(users.get(e.getCalledBy())),
                e.getCompletedAt(), e.getSkippedAt(), e.getSkipReason(), e.getCancelledAt(),
                e.getCancelReason(), e.getRequeueCount(), e.getVersion())).toList();
    }

    public QueueEntryRef toRef(QueueEntryEntity entry) {
        return new QueueEntryRef(entry.getId(), entry.getQueueNumber(), entry.getStatus());
    }

    private StaffRef staffRef(UserEntity user) {
        return user == null ? null : new StaffRef(user.getId(), user.getFullName());
    }
}
