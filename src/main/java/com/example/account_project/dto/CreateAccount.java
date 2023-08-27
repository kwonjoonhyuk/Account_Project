package com.example.account_project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class CreateAccount {
    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request{
        @NotNull
        @Min(1)
        private Long userId;

        @NotNull
        @Min(0)
        private Long initialBalance;
    }
    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response{
        private Long userId;
        private String accountNumber;
        private LocalDateTime registeredAt;

        public static Response from(AccountDto accountDto){
            return Response.builder()
                    .userId(accountDto.getUserId())
                    .accountNumber(accountDto.getAccountNumber())
                    .registeredAt(accountDto.getRegisteredAt())
                    .build();
        }
    }
}
