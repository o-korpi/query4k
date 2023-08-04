import arrow.core.Either
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.query4k.Query4k
import io.query4k.QueryOnlyError
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


@Serializable
data class TestTable(
    val id: Long,
    val test: String
)

class TestQuery4k {
    private val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:~/test"
            username = "sa"
            password = ""
            driverClassName = "org.h2.Driver"
        }
    )

    private val q4k = Query4k.create(dataSource)

    private fun createTable() = q4k.execute(
            """
            CREATE TABLE test_table (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test VARCHAR NOT NULL
            );
            """.trimIndent()
        )

    private fun insertRows(rowCount: Int) {
        (1..rowCount).forEach {
            q4k.execute("INSERT INTO test_table (test) VALUES ('test$it');")
        }
    }

    private fun dropTable() {
        try {
            q4k.execute("DROP TABLE test_table;")
        } catch (_: Exception) {}
    }

    @BeforeEach
    fun beforeEach() {
        createTable()
    }

    @AfterEach
    fun afterEach() {
        dropTable()
    }

    @Test
    fun `execute should be able to create tables`() {
        dropTable()
        val result = q4k.execute(
            """
            CREATE TABLE test_table (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test VARCHAR NOT NULL
            );
            """.trimIndent()
        )
        result.shouldBeRight()
    }

    @Test
    fun `execute insert should give amount of changed rows`() {
        val result = q4k.execute("INSERT INTO test_table (test) VALUES :test", mapOf("test" to "Hello world!"))
        result.shouldBeRight() shouldBe 1
    }

    @Test
    fun `execute insert should not work for unknown tables`() {
        shouldThrowAny {
            q4k.execute("INSERT INTO unknown_table (test) VALUES ('test')")
        }
    }

    @Test
    fun `executeAndReturnAutoGeneratedKeys insert should give auto-generated keys for single row`() {
        val result = q4k.executeAndReturnAutoGeneratedKeys("INSERT INTO test_table (test) VALUES :test", mapOf("test" to "Hello world!"))
        val keys = result.shouldBeRight()
        keys[0] shouldHaveKey "id"
        keys[0]["id"] shouldBe 1L
    }

    @Test
    fun `executeAndReturnAutoGeneratedKeys insert should give auto-generated keys for multiple rows`() {
        val result = q4k.executeAndReturnAutoGeneratedKeys("INSERT INTO test_table (test) VALUES :test", mapOf("test" to "Hello world!"))
        val keys = result.shouldBeRight()
        keys[0] shouldHaveKey "id"
        keys[0]["id"] shouldBe 1L
    }

    @Test
    fun `executeGetKey should give key for single insert`() {
        val result = q4k.executeGetKey<Long>("INSERT INTO test_table (test) VALUES :test", "id", mapOf("test" to "Hello world!"))
        result.shouldBeRight() shouldBe 1L
    }

    @Test
    fun `query results should be empty if nothing exists`() {
        val result = q4k.query<TestTable>("SELECT * FROM test_table")
        result
            .shouldBeRight()
            .shouldBeEmpty()
    }

    @Test
    fun `query results should contain one element if only one exists`() {
        insertRows(10)
        val result = q4k.query<TestTable>("SELECT * FROM test_table WHERE id=:id", mapOf("id" to 3L))
        result
            .shouldBeRight()
            .shouldHaveSize(1)
    }

    @Test
    fun `query should pass for a 'standard' query`() {
        insertRows(100)
        val result = q4k.query<TestTable>("SELECT * FROM test_table")
        result
            .shouldBeRight()
            .shouldHaveSize(100)
    }

    @Test
    fun `queryFirst should only get the first result from multiple matching rows`() {
        insertRows(25)
        val result = q4k.queryFirst<TestTable>("SELECT * FROM test_table WHERE id>= :id", mapOf("id" to 15L))
        result
            .shouldBeRight()
            .shouldNotBeNull()
            .id shouldBe 15L
    }

    @Test
    fun `queryFirst should be null if no results are found`() {
        val result = q4k.queryFirst<TestTable>("SELECT * FROM test_table")
        result
            .shouldBeRight()
            .shouldBeNull()
    }

    @Test
    fun `queryOnly should succeed if only one result exists`() {
        insertRows(1)
        val result: Either<QueryOnlyError, TestTable> = q4k.queryOnly<TestTable>("SELECT * FROM test_table")
        val value = result.shouldBeRight()
        value.id shouldBe 1L
    }

    @Test
    fun `queryOnly should succeed when one row is taken`() {
        insertRows(3)
        val result: Either<QueryOnlyError, TestTable> = q4k.queryOnly<TestTable>(
            "SELECT * FROM test_table WHERE id=:id",
            mapOf("id" to 2L)
        )
        val value = result.shouldBeRight()
        value.id shouldBe 2L
    }

    @Test
    fun `queryOnly should fail on multiple results`() {
        insertRows(2)
        val result = q4k.queryOnly<TestTable>("SELECT * FROM test_table")
        result.shouldBeLeft()
    }

    @Test
    fun `queryOnly should fail on no results`() {
        val result = q4k.queryOnly<TestTable>("SELECT * FROM test_table")
        result.shouldBeLeft()
    }
}
