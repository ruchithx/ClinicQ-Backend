package com.dispensary.dms.queue;

import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.queue.dto.PublicQueueSessionList;
import com.dispensary.dms.queue.dto.PublicQueueSessionSummary;
import com.dispensary.dms.queue.dto.PublicQueueStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Public, unauthenticated queue views built from numbers only (spec section 10.7). These methods
 * never load name/phone/notes into any DTO (privacy: BR-Q-10).
 */
@Service
public class PublicQueueService {

    private static final int MAX_WAITING_NUMBERS = 50;

    private final QueueSessionRepository sessionRepository;
    private final QueueEntryRepository entryRepository;
    private final BusinessClock clock;

    public PublicQueueService(QueueSessionRepository sessionRepository,
                              QueueEntryRepository entryRepository, BusinessClock clock) {
        this.sessionRepository = sessionRepository;
        this.entryRepository = entryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PublicQueueSessionList listToday() {
        var today = clock.today();
        List<PublicQueueSessionSummary> items = sessionRepository.findBySessionDateOrderByName(today)
                .stream().map(this::toSummary).toList();
        return new PublicQueueSessionList(today, items);
    }

    @Transactional(readOnly = true)
    public long versionOf(UUID sessionId) {
        QueueSessionEntity session = requireVisible(sessionId);
        return session.getVersion();
    }

    @Transactional(readOnly = true)
    public PublicQueueStatus getStatus(UUID sessionId) {
        QueueSessionEntity session = requireVisible(sessionId);
        int waitingCount = countWaiting(sessionId);
        List<Integer> waitingNumbers = entryRepository.findWaitingNumbers(sessionId,
                PageRequest.of(0, MAX_WAITING_NUMBERS));
        List<Integer> servingNumbers = entryRepository.findServingNumbers(sessionId);
        return new PublicQueueStatus(
                session.getId(), session.getName(), session.getSessionDate(), session.getStatus(),
                session.getLastCalledNumber(), servingNumbers, waitingCount, waitingNumbers,
                session.getLastIssuedNumber(), session.getUpdatedAt());
    }

    private PublicQueueSessionSummary toSummary(QueueSessionEntity session) {
        return new PublicQueueSessionSummary(session.getId(), session.getName(), session.getSessionDate(),
                session.getStatus(), session.getLastCalledNumber(), countWaiting(session.getId()));
    }

    private int countWaiting(UUID sessionId) {
        return entryRepository.countByStatus(sessionId).stream()
                .filter(sc -> sc.getStatus() == QueueEntryStatus.WAITING)
                .mapToInt(sc -> (int) sc.getCount()).findFirst().orElse(0);
    }

    private QueueSessionEntity requireVisible(UUID sessionId) {
        QueueSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_SESSION_NOT_FOUND,
                        "Queue session not found"));
        if (session.getSessionDate().isBefore(clock.today())) {
            throw new NotFoundException(ErrorCode.QUEUE_SESSION_NOT_FOUND, "Queue session not found");
        }
        return session;
    }
}
