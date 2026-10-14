package com.dispensary.dms.user;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.auth.PasswordPolicy;
import com.dispensary.dms.auth.RefreshTokenService;
import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.security.CurrentUser;
import com.dispensary.dms.user.dto.CreateUserRequest;
import com.dispensary.dms.user.dto.ResetPasswordRequest;
import com.dispensary.dms.user.dto.StaffList;
import com.dispensary.dms.user.dto.UpdateUserRequest;
import com.dispensary.dms.user.dto.UserResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Staff account administration (spec section 10, FR-007/008/009). */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final BusinessClock clock;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
                       RefreshTokenService refreshTokenService, AuditService auditService,
                       BusinessClock clock) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(int page, int size) {
        var result = userRepository.findAllUsers(
                PageRequest.of(page, size, Sort.by("username")));
        return PageResponse.of(result, userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return userMapper.toResponse(require(userId));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, CurrentUser actor) {
        String username = request.username().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new com.dispensary.dms.common.error.ConflictException(
                    ErrorCode.DUPLICATE_USERNAME, "Username already exists");
        }
        passwordPolicy.validate(request.temporaryPassword(), username, null);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setFullName(request.fullName());
        user.setProfessionalRegistrationNumber(request.professionalRegistrationNumber());
        user.setRoles(new LinkedHashSet<>(request.roles()));
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setEnabled(true);
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(clock.now());
        user.setCreatedAt(clock.now());
        user.setUpdatedAt(clock.now());
        user.setCreatedBy(actor.id());
        userRepository.saveAndFlush(user);

        auditService.event(AuditAction.USER_CREATED, EntityType.USER)
                .actor(actor).entityId(user.getId())
                .detail("username", username)
                .detail("roles", request.roles().stream().map(Enum::name).toList())
                .save();
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request, CurrentUser actor) {
        UserEntity user = require(userId);
        if (user.getVersion() != request.version()) {
            throw new ApiException(ErrorCode.CONCURRENT_MODIFICATION,
                    "The user was changed by someone else; reload and retry");
        }

        boolean removingOwnAdmin = user.getId().equals(actor.id())
                && user.getRoles().contains(Role.ADMIN) && !request.roles().contains(Role.ADMIN);
        boolean disablingSelf = user.getId().equals(actor.id()) && !request.enabled();
        if (removingOwnAdmin || disablingSelf) {
            throw new BusinessRuleException(ErrorCode.SELF_MODIFICATION_NOT_ALLOWED,
                    "You cannot disable your own account or remove your own ADMIN role");
        }

        boolean losesAdmin = user.getRoles().contains(Role.ADMIN)
                && (!request.roles().contains(Role.ADMIN) || !request.enabled());
        if (losesAdmin && wouldLeaveNoAdmin(user.getId())) {
            throw new BusinessRuleException(ErrorCode.LAST_ADMIN_REQUIRED,
                    "At least one enabled administrator is required");
        }

        List<String> changed = new ArrayList<>();
        if (!user.getFullName().equals(request.fullName())) {
            changed.add("fullName");
        }
        if (!user.getRoles().equals(new LinkedHashSet<>(request.roles()))) {
            changed.add("roles");
        }
        if (user.isEnabled() != request.enabled()) {
            changed.add("enabled");
        }
        if (!java.util.Objects.equals(user.getProfessionalRegistrationNumber(),
                request.professionalRegistrationNumber())) {
            changed.add("professionalRegistrationNumber");
        }

        boolean wasEnabled = user.isEnabled();
        user.setFullName(request.fullName());
        user.setRoles(new LinkedHashSet<>(request.roles()));
        user.setEnabled(request.enabled());
        user.setProfessionalRegistrationNumber(request.professionalRegistrationNumber());
        user.setUpdatedAt(clock.now());
        userRepository.save(user);

        if (wasEnabled && !request.enabled()) {
            refreshTokenService.revokeAllForUser(user.getId(), "USER_DISABLED");
        }

        auditService.event(AuditAction.USER_UPDATED, EntityType.USER)
                .actor(actor).entityId(user.getId())
                .detail("changedFields", changed)
                .detail("roles", request.roles().stream().map(Enum::name).toList())
                .detail("enabled", request.enabled())
                .save();
        return userMapper.toResponse(user);
    }

    @Transactional
    public void resetPassword(UUID userId, ResetPasswordRequest request, CurrentUser actor) {
        UserEntity user = require(userId);
        passwordPolicy.validate(request.temporaryPassword(), user.getUsername(), null);
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(clock.now());
        user.setUpdatedAt(clock.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId(), "PASSWORD_RESET");
        auditService.event(AuditAction.USER_PASSWORD_RESET, EntityType.USER)
                .actor(actor).entityId(user.getId()).save();
    }

    @Transactional(readOnly = true)
    public StaffList listStaff() {
        return new StaffList(userRepository.findEnabledByRoleIn(List.of(Role.values()))
                .stream().map(userMapper::toStaffMember).toList());
    }

    private boolean wouldLeaveNoAdmin(UUID changingUserId) {
        List<UserEntity> admins = userRepository.findEnabledAdminsForUpdate();
        return admins.stream().allMatch(a -> a.getId().equals(changingUserId));
    }

    private UserEntity require(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
