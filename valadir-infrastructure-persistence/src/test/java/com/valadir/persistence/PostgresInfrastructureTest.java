package com.valadir.persistence;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresInfrastructureTest extends PostgresTestContainer {

    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {

        connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    }

    @ParameterizedTest(name = "{1} should be created by init.sql")
    @MethodSource("provideSchemaObjectQueries")
    @DisplayName("init.sql should create all schema objects")
    void initSql_allExpectedSchemaObjects_exist(String query, String description) throws Exception {

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            resultSet.next();
            assertThat(resultSet.getBoolean(1)).isTrue();
        }
    }

    private static Stream<Arguments> provideSchemaObjectQueries() {

        return Stream.of(
            Arguments.of("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'accounts')", "Table 'accounts'"),
            Arguments.of("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users')", "Table 'users'"),
            Arguments.of("SELECT EXISTS (SELECT FROM pg_type WHERE typname = 'user_role')", "Enum 'user_role'")
        );
    }
}
