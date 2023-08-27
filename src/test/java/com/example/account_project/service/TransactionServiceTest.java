package com.example.account_project.service;

import com.example.account_project.Exception.AccountException;
import com.example.account_project.domain.Account;
import com.example.account_project.domain.AccountUser;
import com.example.account_project.domain.Transaction;
import com.example.account_project.dto.TransactionDto;
import com.example.account_project.repository.AccountRepository;
import com.example.account_project.repository.AccountUserRepository;
import com.example.account_project.repository.TransactionRepository;
import com.example.account_project.type.AccountStatus;
import com.example.account_project.type.ErrorCode;
import com.example.account_project.type.TransactionResultType;
import com.example.account_project.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long CANCEL_AMOUNT = 1000L;
    public static final String TRANSACTION_ID = "transactionId";
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //------------실제로 테스트할때 동작해서 잔액 부분 쓰이는곳------------------
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        //-----------------------------------------------------------------------
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        //이 값은 의미가 없음
        TransactionDto transactionDto =
                transactionService.useBalance(1L, "1000000012", 1500L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1500L, captor.getValue().getAmount());
        assertEquals(8500L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 사용자 없음")
    void useBalanceFailed_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 계좌 없음")
    void useBalanceFailed_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 소유주 같지 않음")
    void useBalanceFailed_userUnMatch() {
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
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 이미 해지된 계좌")
    void useBalanceFailed_alreadyUnregistered() {
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
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 사용 금액이 계좌 금액 초과")
    void useBalanceFailed_exceedAmount() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(100L)
                .accountNumber("1000000012")
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, accountException.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("잔액 사용 실패 - Transaction 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //------------실제로 테스트할때 동작해서 잔액 부분 쓰이는곳------------------
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        //-----------------------------------------------------------------------
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        //이 값은 의미가 없음
        transactionService.saveFailedUseTransaction("1000000000", 1500L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1500L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.FAIL, captor.getValue().getTransactionResultType());
    }

    @Test
    @DisplayName("잔액 사용 취소 성공")
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000000")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //------------실제로 테스트할때 동작해서 잔액 부분 쓰이는곳------------------
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.CANCEL)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(10000L)
                        .build());
        //-----------------------------------------------------------------------
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        //이 값은 의미가 없음
        TransactionDto transactionDto =
                transactionService.cancelBalance("transactionId", "1000000000", 1000L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(11000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancelTransactionFailed_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder()
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(9000L)
                        .build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", CANCEL_AMOUNT));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 거래 없음")
    void cancelTransactionFailed_TransactionNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", CANCEL_AMOUNT));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래와 계좌가 매칭 실패")
    void cancelTransactionFailed_TransactionAccountUnMatch() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Account accountNotUse = Account.builder()
                .id(2L)
                .accountUser(user)
                .accountNumber("1234567888")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .transactionId(TRANSACTION_ID)
                        .amount(CANCEL_AMOUNT)
                        .transactedAt(LocalDateTime.now())
                        .balanceSnapshot(9000L)
                        .build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        TRANSACTION_ID, "1234567890", CANCEL_AMOUNT));
        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래금액과 취소금액이 다름")
    void cancelTransactionFailed_CancelMustFully() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .transactionId(TRANSACTION_ID)
                        .amount(CANCEL_AMOUNT + 1000L)
                        .transactedAt(LocalDateTime.now())
                        .balanceSnapshot(9000L)
                        .build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        TRANSACTION_ID, "1234567890", CANCEL_AMOUNT));
        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 취소는 1년까지만 가능")
    void cancelTransactionFailed_() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder().build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .amount(CANCEL_AMOUNT)
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1)).build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        TRANSACTION_ID, "1234567890", CANCEL_AMOUNT));
        //then
        assertEquals(ErrorCode.TO_OLD_ORDER_TO_CANCEL, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래 조회 확인 성공")
    void successQueryTransaction() {
        //given
        AccountUser user = AccountUser.builder().id(1L).name("messi").build();
        Account account = Account.builder().build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .amount(CANCEL_AMOUNT)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now()).build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        //then
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(TransactionResultType.SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("거래 조회 실패 - 원거래 없음")
        void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                ()-> transactionService.queryTransaction("transactionId"));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND,accountException.getErrorCode());
    }
}