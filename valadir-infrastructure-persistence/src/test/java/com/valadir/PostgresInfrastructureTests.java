package com.valadir;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresInfrastructureTests {

    private static Connection connection;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("valadir_db")
        .withUsername("valadir_user")
        .withPassword("valadir_pass")
        .withCopyFileToContainer(
            MountableFile.forHostPath("../docker/postgres/init.sql"),
            "/docker-entrypoint-initdb.d/init.sql"
        );

    @BeforeAll
    static void setup() throws Exception {

        connection = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword());
    }

    @Test
    void should_create_accounts_table() throws Exception {

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'accounts')");
        resultSet.next();

        assertThat(resultSet.getBoolean(1))
            .as("Table 'accounts' should have been created by init.sql")
            .isTrue();
    }

    @Test
    void should_create_users_table() throws Exception {

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'accounts')");
        resultSet.next();

        assertThat(resultSet.getBoolean(1))
            .as("Table 'users' should have been created by init.sql")
            .isTrue();
    }

    @Test
    void should_create_user_role_enum() throws Exception {

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT EXISTS (SELECT FROM pg_type WHERE typname = 'user_role')");
        resultSet.next();

        assertThat(resultSet.getBoolean(1))
            .as("Enum 'user_role' should have been created by init.sql")
            .isTrue();
    }
}
