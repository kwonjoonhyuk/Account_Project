package com.example.account_project.repository;

import com.example.account_project.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUserRepository extends JpaRepository<AccountUser ,Long> {
}
