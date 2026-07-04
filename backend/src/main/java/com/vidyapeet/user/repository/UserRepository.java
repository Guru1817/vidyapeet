package com.vidyapeet.user.repository;

import com.vidyapeet.common.Role;
import com.vidyapeet.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Tenant-scoped login lookup (INSTITUTE_ADMIN / STUDENT). */
    Optional<User> findByInstituteIdAndEmail(Long instituteId, String email);

    /** Platform-owner login lookup (SUPER_ADMIN has no institute). */
    Optional<User> findByEmailAndInstituteIdIsNull(String email);

    boolean existsByInstituteIdAndEmail(Long instituteId, String email);

    boolean existsByEmailAndInstituteIdIsNull(String email);

    List<User> findByInstituteIdAndRole(Long instituteId, Role role);

    long countByInstituteIdAndRole(Long instituteId, Role role);

    void deleteByInstituteId(Long instituteId);
}
