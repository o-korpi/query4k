import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.query4k.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue


interface TypeTest {
    val tableName: String

    @BeforeEach
    fun beforeEach()

    @AfterEach
    fun afterEach() { q4k.execute("DROP TABLE $tableName") }

    fun insertRows(rowsCount: Int)
}

class TestList : TypeTest {
    override val tableName = "list_table"

    @Serializable
    data class ListTable(
        val id: Long,
        val test: List<Int>
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test INTEGER ARRAY NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { i ->
            q4k.execute(
                "INSERT INTO $tableName (test) VALUES :list",
                mapOf("list" to listOf(1, 2, 3, 4, i))
            )
        }
    }

    @Test
    fun `test unsafe list insert`() {
        val result = q4k.execute(
            "INSERT INTO $tableName (test) VALUES ({1, 2});"
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `test safe insert`() {
        val result = q4k.execute(
            "INSERT INTO $tableName (test) VALUES :list",
            mapOf("list" to listOf(1, 2, 3))
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `query should pass`() {
        insertRows(10)
        val result = q4k.query<ListTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 10
    }
}

class TestBigDecimal : TypeTest {
    override val tableName: String = "test_table"

    @Serializable
    data class BigDecimalTable(
        val id: Long,
        @Serializable(with = BigDecimalSerializer::class)
        val test: BigDecimal
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test NUMBER NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { i ->
            q4k.execute("INSERT INTO $tableName (test) VALUES :test", mapOf("test" to ((i * 10) / 2).toBigDecimal()))
        }
    }

    @Test
    fun `unsafe insert should work`() {
        val result = q4k.execute(
            "INSERT INTO $tableName (test) VALUES (500.123);"
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `normal insert should work`() {
        val result = q4k.execute(
            "INSERT INTO $tableName (test) VALUES (:value)",
            mapOf("value" to BigDecimal.valueOf(123.5))
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `query should work and be able to map to BigDecimal`() {
        insertRows(5)
        val result = q4k.query<BigDecimalTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 5
    }

}

class TestInt : TypeTest {

    override val tableName: String = "test_table"

    @Serializable
    data class IntTable(
        val id: Long,
        val test: Int
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test INTEGER NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { i ->
            q4k.execute("INSERT INTO $tableName (test) VALUES :test", mapOf("test" to i))
        }
    }

    @Test
    fun `inserts should work`() {
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES :test",
            mapOf("test" to 10)
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `queries should work`() {
        insertRows(3)
        val result = q4k.query<IntTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 3
    }
}


class TestDouble : TypeTest {

    override val tableName: String = "test_table"

    @Serializable
    data class DoubleTable(
        val id: Long,
        val test: Double
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test FLOAT NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { i ->
            q4k.execute("INSERT INTO $tableName (test) VALUES :test", mapOf("test" to i * 2.5))
        }
    }

    @Test
    fun `inserts should work`() {
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES :test",
            mapOf("test" to 10.125)
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `queries should work`() {
        insertRows(3)
        val result = q4k.query<DoubleTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 3
    }
}
