package com.oskarro.oskarito.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, String> {
    void deleteByUserId(Integer userId);
}
