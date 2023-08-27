package com.example.account_project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration //bean 타입 등록
@EnableJpaAuditing // 자동 저장 Auditing

public class JpaAuditingConfiguration {
}
