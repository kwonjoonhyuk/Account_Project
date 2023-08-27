package com.example.account_project.dto;

import com.example.account_project.controller.TransactionController;
import com.example.account_project.type.TransactionResultType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.*;

import java.time.LocalDateTime;

public class UseBalance {
    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request{
        @NotNull
        @Min(1)
        private Long userId;

        @NotBlank
        @Size(min = 10,max = 10)
        private String accountNumber;

        @NotNull
        @Min(10)
        @Max(1000_000_000)
        private Long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response{
        private String accountNumber;
        private TransactionResultType transactionResultType;
        private String transactionId;
        private Long amount;
        private LocalDateTime transactedAt;

        public static Response from(TransactionDto transactionDto) {
            return Response.builder()
                    .accountNumber(transactionDto.getAccountNumber())
                    .transactionResultType(transactionDto.getTransactionResultType())
                    .transactionId(transactionDto.getTransactionId())
                    .amount(transactionDto.getAmount())
                    .transactedAt(transactionDto.getTransactedAt())
                    .build();
        }
    }
}
