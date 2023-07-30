import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.query4k.Query4k
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue


class TestQuery4k {
    private val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/db"
            username = "postgres"
            password = "postgres"
            driverClassName = "org.postgresql.Driver"
        }
    )
    private val q4k = Query4k.create(dataSource)

    @Serializable
    data class TestTable(
        val id: Long,
        val test: String
    )

    private fun populate() {
        q4k.execute(
            """
            CREATE TABLE test_table (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test VARCHAR NOT NULL
            );
            """.trimIndent()
        )

        (1..10).forEach {
            q4k.execute("INSERT INTO test_table (test) VALUES ('test$it');") // NOT INJECTION SAFE
        }
    }

    @BeforeTest
    fun before() {
        try {
            q4k.execute("DROP TABLE test_table;")
        } catch (_: Exception) {}
    }

    @AfterEach
    fun afterEach() {
        try {
            q4k.execute("DROP TABLE test_table;")
        } catch (_: Exception) {}
    }

    @Test
    fun testExecute() {
        assertTrue {
            q4k.execute(
                """
                CREATE TABLE test_table (
                    id BIGSERIAL PRIMARY KEY NOT NULL,
                    test VARCHAR NOT NULL
                );
                """.trimIndent()
            ).isRight()
        }

        assertTrue {
            q4k.execute(
                """
                INSERT INTO test_table (test) VALUES (:test);
                """.trimIndent(),
                mapOf("test" to "test")
            ).isRight()
        }

        assertEquals(
            1,
            q4k.execute(
                """
                INSERT INTO test_table (test) VALUES (:test);
                """.trimIndent(),
                mapOf("test" to "test")
            ).getOrNull(),
        )

        assertEquals(
            3L,
            q4k.executeAndReturnAutoGeneratedKeys(
                """
                INSERT INTO test_table (test) VALUES (:test);
                """.trimIndent(),
                mapOf("test" to "test")
            ).getOrNull()
                ?.firstOrNull()
                ?.get("id")
        )

        assertEquals(
            1,
            q4k.execute(
                """
                UPDATE test_table
                SET test=:test
                WHERE id=:id
                """.trimIndent(),
                mapOf("id" to 1L, "test" to "test")
            ).getOrNull()
        )

        assertThrows<UnableToExecuteStatementException> {
            runBlocking {
                q4k.execute(
                    """
                INSERT INTO unknown_table (test) VALUES ('test');
                """.trimIndent()
                )
            }

        }
    }

    @Test
    fun testQuery() {
        populate()
        assertTrue {
            q4k.query<TestTable>("SELECT * FROM test_table").isRight()
        }
        assertEquals(10, q4k.query<TestTable>("SELECT * FROM test_table").getOrNull()?.size)

        assertTrue {
            q4k.query<TestTable>(
                "SELECT * FROM test_table WHERE id=:id",
                mapOf("id" to 1L)
            ).isRight()
        }

        assertEquals(
            "test1",
            q4k.query<TestTable>(
                "SELECT * FROM test_table WHERE id=:id",
                mapOf("id" to 1L)
            ).getOrNull()?.firstOrNull()?.test
        )
    }

    @Test
    fun testQueryFirst() {
        populate()
        assertTrue {
            q4k.queryFirst<TestTable>("SELECT * FROM test_table").isRight()
        }

        assertTrue {
            q4k.queryFirst<TestTable>(
                "SELECT * FROM test_table WHERE id=:id;",
                mapOf("id" to 1L)
            ).isRight()
        }

        assertEquals(
            "test1",
            q4k.queryFirst<TestTable>(
                "SELECT * FROM test_table;"
            ).getOrNull()?.test
        )

        assertEquals(
            "test1",
            q4k.queryFirst<TestTable>(
                "SELECT * FROM test_table WHERE id=:id",
                mapOf("id" to 1L)
            ).getOrNull()?.test
        )
    }

    @Test
    fun testQueryOnly() {
        populate()
        assertTrue {
            q4k.queryOnly<TestTable>("SELECT * FROM test_table").isLeft()
        }

        assertTrue {
            q4k.queryOnly<TestTable>(
                "SELECT * FROM test_table WHERE id=:id;",
                mapOf("id" to 1L)
            ).isRight()
        }

        assertNotEquals(
            "test1",
            q4k.queryOnly<TestTable>(
                "SELECT * FROM test_table;"
            ).getOrNull()?.test
        )

        assertEquals(
            "test1",
            q4k.queryOnly<TestTable>(
                "SELECT * FROM test_table WHERE id=:id",
                mapOf("id" to 1L)
            ).getOrNull()?.test
        )
    }


}