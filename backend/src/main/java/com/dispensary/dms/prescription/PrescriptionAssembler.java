package com.dispensary.dms.prescription;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.consultation.ConsultationEntity;
import com.dispensary.dms.consultation.ConsultationRepository;
import com.dispensary.dms.patient.PatientEntity;
import com.dispensary.dms.patient.PatientMapper;
import com.dispensary.dms.patient.PatientRepository;
import com.dispensary.dms.patient.dto.PatientRef;
import com.dispensary.dms.pharmacy.DispensingEntity;
import com.dispensary.dms.pharmacy.DispensingItemEntity;
import com.dispensary.dms.pharmacy.DispensingItemRepository;
import com.dispensary.dms.pharmacy.DispensingRepository;
import com.dispensary.dms.pharmacy.dto.Dispensing;
import com.dispensary.dms.pharmacy.dto.DispensingItem;
import com.dispensary.dms.prescription.dto.PrescriptionItem;
import com.dispensary.dms.prescription.dto.PrescriptionPatient;
import com.dispensary.dms.prescription.dto.PrescriptionResponse;
import com.dispensary.dms.prescription.dto.PrescriptionSummary;
import com.dispensary.dms.user.UserEntity;
import com.dispensary.dms.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds prescription DTOs for a list of prescriptions using batch loaders (spec section 6.4),
 * so the number of SQL statements is bounded regardless of page size.
 */
@Component
public class PrescriptionAssembler {

    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DispensingRepository dispensingRepository;
    private final DispensingItemRepository dispensingItemRepository;
    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    public PrescriptionAssembler(PrescriptionItemRepository itemRepository,
                                 PrescriptionRepository prescriptionRepository,
                                 DispensingRepository dispensingRepository,
                                 DispensingItemRepository dispensingItemRepository,
                                 ConsultationRepository consultationRepository,
                                 PatientRepository patientRepository,
                                 UserRepository userRepository,
                                 PatientMapper patientMapper) {
        this.itemRepository = itemRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.dispensingRepository = dispensingRepository;
        this.dispensingItemRepository = dispensingItemRepository;
        this.consultationRepository = consultationRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientMapper = patientMapper;
    }

    public PrescriptionResponse toResponse(PrescriptionEntity prescription) {
        return toResponses(List.of(prescription)).get(0);
    }

    public List<PrescriptionResponse> toResponses(List<PrescriptionEntity> prescriptions) {
        if (prescriptions.isEmpty()) {
            return List.of();
        }
        List<UUID> prescriptionIds = prescriptions.stream().map(PrescriptionEntity::getId).toList();
        Set<UUID> consultationIds = prescriptions.stream()
                .map(PrescriptionEntity::getConsultationId).collect(Collectors.toSet());
        Set<UUID> patientIds = prescriptions.stream()
                .map(PrescriptionEntity::getPatientId).collect(Collectors.toSet());

        Map<UUID, ConsultationEntity> consultations = consultationRepository.findAllById(consultationIds)
                .stream().collect(Collectors.toMap(ConsultationEntity::getId, Function.identity()));
        Map<UUID, PatientEntity> patients = patientRepository.findAllById(patientIds)
                .stream().collect(Collectors.toMap(PatientEntity::getId, Function.identity()));

        Map<UUID, List<PrescriptionItemEntity>> itemsByPrescription =
                itemRepository.findByPrescriptionIds(prescriptionIds).stream()
                        .collect(Collectors.groupingBy(PrescriptionItemEntity::getPrescriptionId));

        Map<UUID, DispensingEntity> dispensingsByPrescription =
                dispensingRepository.findByPrescriptionIds(prescriptionIds).stream()
                        .collect(Collectors.toMap(DispensingEntity::getPrescriptionId, Function.identity()));
        List<UUID> dispensingIds = dispensingsByPrescription.values().stream()
                .map(DispensingEntity::getId).toList();
        Map<UUID, List<DispensingItemEntity>> dispensingItems = dispensingIds.isEmpty() ? Map.of()
                : dispensingItemRepository.findByDispensingIds(dispensingIds).stream()
                .collect(Collectors.groupingBy(DispensingItemEntity::getDispensingId));

        Map<UUID, UUID> supersededBy = new java.util.HashMap<>();
        for (Object[] pair : prescriptionRepository.findSupersededByPairs(prescriptionIds)) {
            supersededBy.put((UUID) pair[0], (UUID) pair[1]);
        }

        // Collect all staff user ids referenced.
        Set<UUID> userIds = new HashSet<>();
        for (PrescriptionEntity p : prescriptions) {
            ConsultationEntity c = consultations.get(p.getConsultationId());
            if (c != null) {
                userIds.add(c.getDoctorId());
            }
            if (p.getStartedBy() != null) {
                userIds.add(p.getStartedBy());
            }
            if (p.getCancelledBy() != null) {
                userIds.add(p.getCancelledBy());
            }
            DispensingEntity d = dispensingsByPrescription.get(p.getId());
            if (d != null) {
                userIds.add(d.getPharmacistId());
            }
        }
        Map<UUID, UserEntity> users = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<PrescriptionResponse> result = new ArrayList<>(prescriptions.size());
        for (PrescriptionEntity p : prescriptions) {
            ConsultationEntity c = consultations.get(p.getConsultationId());
            PatientEntity patient = patients.get(p.getPatientId());
            DispensingEntity dispensing = dispensingsByPrescription.get(p.getId());
            result.add(new PrescriptionResponse(
                    p.getId(),
                    formatNumber(p.getPrescriptionNumber()),
                    p.getConsultationId(),
                    c == null ? null : c.getConsultationAt(),
                    p.getStatus(),
                    p.getNotes(),
                    p.getSubmittedAt(),
                    c == null ? null : staffRef(users.get(c.getDoctorId())),
                    prescriptionPatient(patient),
                    itemsByPrescription.getOrDefault(p.getId(), List.of()).stream()
                            .map(this::toItem).toList(),
                    p.getStartedAt(),
                    staffRef(users.get(p.getStartedBy())),
                    p.getCancelledAt(),
                    staffRef(users.get(p.getCancelledBy())),
                    p.getCancelReason(),
                    p.getSupersedesPrescriptionId(),
                    supersededBy.get(p.getId()),
                    dispensing == null ? null : toDispensing(dispensing,
                            dispensingItems.getOrDefault(dispensing.getId(), List.of()),
                            users.get(dispensing.getPharmacistId())),
                    p.getVersion()));
        }
        return result;
    }

    public List<PrescriptionSummary> toSummaries(List<PrescriptionEntity> prescriptions) {
        if (prescriptions.isEmpty()) {
            return List.of();
        }
        List<UUID> prescriptionIds = prescriptions.stream().map(PrescriptionEntity::getId).toList();
        Set<UUID> consultationIds = prescriptions.stream()
                .map(PrescriptionEntity::getConsultationId).collect(Collectors.toSet());
        Set<UUID> patientIds = prescriptions.stream()
                .map(PrescriptionEntity::getPatientId).collect(Collectors.toSet());

        Map<UUID, ConsultationEntity> consultations = consultationRepository.findAllById(consultationIds)
                .stream().collect(Collectors.toMap(ConsultationEntity::getId, Function.identity()));
        Map<UUID, PatientEntity> patients = patientRepository.findAllById(patientIds)
                .stream().collect(Collectors.toMap(PatientEntity::getId, Function.identity()));
        Map<UUID, Long> itemCounts = itemRepository.findByPrescriptionIds(prescriptionIds).stream()
                .collect(Collectors.groupingBy(PrescriptionItemEntity::getPrescriptionId, Collectors.counting()));
        Map<UUID, DispensingEntity> dispensings = dispensingRepository.findByPrescriptionIds(prescriptionIds)
                .stream().collect(Collectors.toMap(DispensingEntity::getPrescriptionId, Function.identity()));

        Set<UUID> userIds = new HashSet<>();
        for (PrescriptionEntity p : prescriptions) {
            ConsultationEntity c = consultations.get(p.getConsultationId());
            if (c != null) {
                userIds.add(c.getDoctorId());
            }
            if (p.getStartedBy() != null) {
                userIds.add(p.getStartedBy());
            }
        }
        Map<UUID, UserEntity> users = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<PrescriptionSummary> result = new ArrayList<>(prescriptions.size());
        for (PrescriptionEntity p : prescriptions) {
            ConsultationEntity c = consultations.get(p.getConsultationId());
            PatientEntity patient = patients.get(p.getPatientId());
            DispensingEntity dispensing = dispensings.get(p.getId());
            result.add(new PrescriptionSummary(
                    p.getId(),
                    formatNumber(p.getPrescriptionNumber()),
                    p.getConsultationId(),
                    p.getStatus(),
                    patient == null ? null : new PatientRef(patient.getId(), patient.getPatientNumber(),
                            patient.getFullName()),
                    c == null ? null : staffRef(users.get(c.getDoctorId())),
                    p.getSubmittedAt(),
                    itemCounts.getOrDefault(p.getId(), 0L).intValue(),
                    staffRef(users.get(p.getStartedBy())),
                    dispensing == null ? null : dispensing.getOutcome(),
                    dispensing == null ? null : dispensing.getDispensedAt()));
        }
        return result;
    }

    public static String formatNumber(long number) {
        return String.format("RX-%06d", number);
    }

    private PrescriptionItem toItem(PrescriptionItemEntity i) {
        return new PrescriptionItem(i.getId(), i.getLineNumber(), i.getMedicineId(), i.getMedicineName(),
                i.getStrength(), i.getDosageForm(), i.getDose(), i.getFrequency(),
                i.getDurationDays() == null ? null : (int) (short) i.getDurationDays(),
                i.getQuantity(), i.getQuantityUnit(), i.getInstructions());
    }

    private Dispensing toDispensing(DispensingEntity d, List<DispensingItemEntity> items, UserEntity pharmacist) {
        return new Dispensing(d.getId(), staffRef(pharmacist), d.getDispensedAt(), d.getOutcome(),
                d.getNotes(), items.stream().map(this::toDispensingItem).toList());
    }

    private DispensingItem toDispensingItem(DispensingItemEntity i) {
        return new DispensingItem(i.getPrescriptionItemId(), i.getQuantityDispensed(), i.getStatus(),
                i.getNote());
    }

    private PrescriptionPatient prescriptionPatient(PatientEntity p) {
        if (p == null) {
            return null;
        }
        return new PrescriptionPatient(p.getId(), p.getPatientNumber(), p.getFullName(), p.getSex(),
                patientMapper.ageYears(p), p.isDateOfBirthEstimated(), p.getKnownAllergies());
    }

    private StaffRef staffRef(UserEntity user) {
        return user == null ? null : new StaffRef(user.getId(), user.getFullName());
    }
}
