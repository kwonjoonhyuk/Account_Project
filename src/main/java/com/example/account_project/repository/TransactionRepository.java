package com.example.account_project.repository;

import com.example.account_project.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {

    Optional<Transaction> findByTransactionId(String transactionId);
}
