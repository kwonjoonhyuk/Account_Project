package com.example.account_project.service;

import com.example.account_project.Exception.AccountException;
import com.example.account_project.domain.Account;
import com.example.account_project.domain.AccountUser;
import com.example.account_project.dto.AccountDto;
import com.example.account_project.repository.AccountRepository;
import com.example.account_project.repository.AccountUserRepository;
import com.example.account_project.type.AccountStatus;
import com.example.account_project.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.DomainEvents;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;
    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성 성공")
    void createAccountService() {

        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user)); // 계좌 찾기

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012").build()));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000014").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 100L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());

    }

    @Test
    @DisplayName("계좌 생성 성공 - 계좌가 존재하지 않을시")
    void createFirstAccount() {
        //given
        AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty()); // 아무계좌도 없는 상황일떄

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 해당 유저 없음")
    void createAccountFailed_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 유저의 계좌 수 10개 초과")
    void createAccountFailed_maxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder().id(15L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));
        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void deleteAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000002").build()));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000002", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());

    }

    @Test
    @DisplayName("계좌 해지 실패 - 유저를 찾을수 없음")
    void deleteAccountFailed_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 해당 계좌가 없음")
    void deleteAccountFailed_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 계좌 소유주가 다름")
    void deleteAccountFailed_userUnMatch() {
        //given
        AccountUser messi = AccountUser.builder().id(1L).name("messi").build();
        AccountUser ronaldo = AccountUser.builder().id(2L).name("ronaldo").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(messi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(ronaldo)
                        .balance(0L)
                        .accountNumber("1234567890").build()));
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 잔액이 남아있음")
    void deleteAccountFailed_balanceNotEmpty() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(100L)
                        .accountNumber("1234567890").build()));
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 이미 해지된 계좌")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountNumber("1234567890")
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED).build()));
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 찾기 성공 - 유저 아이디로 찾기")
    void successGetAccountByUserId() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(user)
                        .accountNumber("1234567890")
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .accountNumber("1237894560")
                        .balance(2000L)
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .accountNumber("4567891230")
                        .balance(3000L)
                        .build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountDto> accountDtoList = accountService.getAccountByUserId(1L);

        //then
        assertEquals(3, accountDtoList.size());
        assertEquals("1234567890", accountDtoList.get(0).getAccountNumber());
        assertEquals(1000, accountDtoList.get(0).getBalance());
        assertEquals("1237894560", accountDtoList.get(1).getAccountNumber());
        assertEquals(2000, accountDtoList.get(1).getBalance());
        assertEquals("4567891230", accountDtoList.get(2).getAccountNumber());
        assertEquals(3000, accountDtoList.get(2).getBalance());
    }

    @Test
    @DisplayName("계좌 찾기 실패 - 해당 유저 없음")
    void failToGetAccounts() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.getAccountByUserId(1L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

}