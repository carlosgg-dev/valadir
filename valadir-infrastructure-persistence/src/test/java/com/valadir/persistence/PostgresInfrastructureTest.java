package com.valadir.persistence;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresInfrastructureTest {

    private static Connection connection;

    @Container
    @SuppressWarnings("resource")
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

    @ParameterizedTest(name = "{1} should be created by init.sql")
    @MethodSource("schemaObjectQueries")
    @DisplayName("init.sql should create all schema objects")
    void initSql_allExpectedSchemaObjects_exist(String query, String description) throws Exception {

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        resultSet.next();

        assertThat(resultSet.getBoolean(1)).isTrue();
    }

    static Stream<Arguments> schemaObjectQueries() {

        return Stream.of(
            Arguments.of("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'accounts')", "Table 'accounts'"),
            Arguments.of("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users')", "Table 'users'"),
            Arguments.of("SELECT EXISTS (SELECT FROM pg_type WHERE typname = 'user_role')", "Enum 'user_role'")
        );
    }
}
