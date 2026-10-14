package com.dispensary.dms.user;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.user.dto.StaffMember;
import com.dispensary.dms.user.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

/** Maps {@link UserEntity} to its response DTOs. */
@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                new LinkedHashSet<>(user.getRoles()),
                user.isEnabled(),
                user.isMustChangePassword(),
                user.getProfessionalRegistrationNumber(),
                user.getLastLoginAt(),
                user.getLockedUntil(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getVersion());
    }

    public StaffMember toStaffMember(UserEntity user) {
        return new StaffMember(user.getId(), user.getFullName(), new LinkedHashSet<>(user.getRoles()));
    }

    public StaffRef toStaffRef(UserEntity user) {
        return new StaffRef(user.getId(), user.getFullName());
    }
}
