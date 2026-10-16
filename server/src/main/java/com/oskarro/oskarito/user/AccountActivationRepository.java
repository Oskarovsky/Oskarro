package com.oskarro.oskarito.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountActivationRepository extends JpaRepository<AccountActivation, String> {
    void deleteByUserId(Integer userId);
}
