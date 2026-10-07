package com.example.datban;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Destructive fixtures are restricted to a disposable localhost database. */
@EnabledIfEnvironmentVariable(named="MYSQL_TEST_URL",matches="jdbc:mysql://(localhost|127.0.0.1):13308/golden_leaf_week3_test(\\?.*)?")
@SpringBootTest(properties={"spring.datasource.url=${MYSQL_TEST_URL}","spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=root","spring.datasource.password=${MYSQL_TEST_PASSWORD}",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect","app.security.require-auth=true",
        "app.payment.bank-name=Test bank","app.payment.account-number=123456789","app.payment.account-name=TEST RESTAURANT"})
@ActiveProfiles("test")
@Import(OperationsTests.Fakes.class)
class MySqlOperationsTests extends OperationsTests {}
