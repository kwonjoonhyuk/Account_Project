package com.example.account_project.domain;

import com.example.account_project.Exception.AccountException;
import com.example.account_project.type.AccountStatus;
import com.example.account_project.type.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity //설정 파일이다 라는 뜻
@EntityListeners(AuditingEntityListener.class) //@createDate 와 @LastModifiedDate 를 사용 하기 위해 씀
public class Account {
    // 하나의 테이블을 생성한것

    @Id
    @GeneratedValue
    Long id;

    @ManyToOne  //한명의 사람이 여러 계좌를 가질 수 있기 때문에 ManyToOne 이다.
    private AccountUser accountUser;
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unregisteredAt;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void useBalance(Long amount) {
        if (amount > balance) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
        balance -= amount;
    }


    public void CancelBalance(Long amount) {
        if (amount < 0) {
            throw new AccountException(ErrorCode.INVALID_REQUEST);
        }
        balance += amount;
    }
}
