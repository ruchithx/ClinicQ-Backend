package com.dispensary.dms.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    long count();

    @Query("select u from UserEntity u")
    Page<UserEntity> findAllUsers(Pageable pageable);

    @Query("""
            select distinct u from UserEntity u join u.roles r
            where u.enabled = true and r in :roles
            order by u.fullName
            """)
    List<UserEntity> findEnabledByRoleIn(@Param("roles") List<Role> roles);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u join u.roles r where r = com.dispensary.dms.user.Role.ADMIN "
            + "and u.enabled = true order by u.id")
    List<UserEntity> findEnabledAdminsForUpdate();
}
