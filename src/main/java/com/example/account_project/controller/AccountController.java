package com.example.account_project.controller;

import com.example.account_project.domain.Account;
import com.example.account_project.dto.AccountDto;
import com.example.account_project.dto.AccountInfo;
import com.example.account_project.dto.CreateAccount;
import com.example.account_project.dto.DeleteAccount;
import com.example.account_project.service.AccountService;
import com.example.account_project.service.RedisTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final RedisTestService redisTestService;

    @PostMapping("/account")
    public CreateAccount.Response creatAccount(@RequestBody @Valid CreateAccount.Request request) {
        return CreateAccount.Response.from(accountService.createAccount(
                request.getUserId(),
                request.getInitialBalance())
        );
    }

    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount(@RequestBody @Valid DeleteAccount.Request request) {
        return DeleteAccount.Response.from(
                accountService.deleteAccount(request.getUserId(), request.getAccountNumber())
        );
    }

    @GetMapping("/account")
    public List<AccountInfo> getAccountByUserId(@RequestParam("user_id") Long userId) {
        return accountService.getAccountByUserId(userId).
                stream().map(accountDto -> AccountInfo.builder()
                        .accountNumber(accountDto.getAccountNumber())
                        .balance(accountDto.getBalance()).build()).collect(Collectors.toList());
    }

    @GetMapping("/get-lock")
    public String getLock() {
        return redisTestService.getLock();
    }


    @GetMapping("/account/{id}")
    public Account getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }
}
