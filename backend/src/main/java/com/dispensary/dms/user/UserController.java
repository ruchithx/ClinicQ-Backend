package com.dispensary.dms.user;

import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.security.CurrentUser;
import com.dispensary.dms.user.dto.CreateUserRequest;
import com.dispensary.dms.user.dto.ResetPasswordRequest;
import com.dispensary.dms.user.dto.UpdateUserRequest;
import com.dispensary.dms.user.dto.UserResponse;
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
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(userService.list(page, size));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request,
                                                   CurrentUser principal) {
        UserResponse created = userService.create(request, principal);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id()))
                .cacheControl(CacheControl.noStore()).body(created);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(userService.get(userId));
    }

    @PutMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId,
                                                   @Valid @RequestBody UpdateUserRequest request,
                                                   CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(userService.update(userId, request, principal));
    }

    @PostMapping(path = "/{userId}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> resetUserPassword(@PathVariable UUID userId,
                                                  @Valid @RequestBody ResetPasswordRequest request,
                                                  CurrentUser principal) {
        userService.resetPassword(userId, request, principal);
        return ResponseEntity.noContent().build();
    }
}
