package com.example.xapp.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 統合テストの基底クラス。
 *
 * <p>コンテナは static フィールドなので JVM 内で 1 つだけ起動し、
 * 全テストクラスで再利用される。テストクラスごとに起動すると
 * 全体の実行時間が現実的でなくなるため。
 *
 * <p>スキーマは Flyway が作る。Hibernate の ddl-auto は validate 固定で、
 * エンティティとマイグレーションのズレはコンテキスト起動時に落ちる。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    static {
        // JUnit の @Container ではなく手動起動にしている。
        // static フィールドを共有して 1 コンテナで通すため。
        POSTGRES.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
