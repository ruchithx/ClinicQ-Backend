package com.dispensary.dms.medicine;

import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.medicine.dto.CreateMedicineRequest;
import com.dispensary.dms.medicine.dto.MedicineResponse;
import com.dispensary.dms.medicine.dto.UpdateMedicineRequest;
import com.dispensary.dms.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/medicines", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<PageResponse<MedicineResponse>> listMedicines(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(medicineService.list(search, active, page, size));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicineResponse> createMedicine(@Valid @RequestBody CreateMedicineRequest request,
                                                           CurrentUser principal) {
        MedicineResponse created = medicineService.create(request, principal);
        return ResponseEntity.created(URI.create("/api/v1/medicines/" + created.id()))
                .cacheControl(CacheControl.noStore()).body(created);
    }

    @GetMapping("/{medicineId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicineResponse> getMedicine(@PathVariable UUID medicineId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(medicineService.get(medicineId));
    }

    @PutMapping(path = "/{medicineId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicineResponse> updateMedicine(@PathVariable UUID medicineId,
                                                           @Valid @RequestBody UpdateMedicineRequest request,
                                                           CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(medicineService.update(medicineId, request, principal));
    }
}
