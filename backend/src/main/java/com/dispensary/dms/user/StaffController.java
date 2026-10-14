package com.dispensary.dms.user;

import com.dispensary.dms.user.dto.StaffList;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/staff", produces = MediaType.APPLICATION_JSON_VALUE)
public class StaffController {

    private final UserService userService;

    public StaffController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PHARMACIST')")
    public ResponseEntity<StaffList> listStaff() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(userService.listStaff());
    }
}
