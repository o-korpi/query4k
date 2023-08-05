import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.query4k.serializers.BigDecimalSerializer
import io.query4k.serializers.toSQLParseable
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.uuid.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal


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

class TestUUID : TypeTest {

    override val tableName: String = "test_table"

    @Serializable
    data class UUIDTable(
        val id: Long,
        val test: UUID
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test UUID NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { _ ->
            q4k.execute("INSERT INTO $tableName (test) VALUES ('${UUID()}')")
        }
    }

    @Test
    fun `unsafe insert should work`() {
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES ('${UUID()}')")
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `inserts should work`() {
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES (:test)",
            mapOf("test" to UUID().toSQLParseable())
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `queries should work`() {
        insertRows(3)
        val result = q4k.query<UUIDTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 3
    }
}


class TestTime : TypeTest {

    override val tableName: String = "test_table"

    @Serializable
    data class TimeTable(
        val id: Long,
        val test: LocalTime
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test TIME NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { i ->
            q4k.execute("INSERT INTO $tableName (test) VALUES ('${Clock.System.now().toLocalDateTime(TimeZone.UTC).time}')")
        }
    }

    @Test
    fun `unsafe insert should work`() {
        val time: LocalTime = Clock.System.now().toLocalDateTime(TimeZone.UTC).time
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES ('$time')")
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `inserts should work`() {
        val time: LocalTime = Clock.System.now().toLocalDateTime(TimeZone.UTC).time
        println(time.toString())
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES :test",
            mapOf("test" to time.toSQLParseable())
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `queries should work`() {
        insertRows(3)
        val result = q4k.query<TimeTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 3
    }
}


class TestDate : TypeTest {

    override val tableName: String = "test_table"

    @Serializable
    data class DateTable(
        val id: Long,
        val test: LocalDate
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test DATE NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { i ->
            q4k.execute("INSERT INTO $tableName (test) VALUES ('${LocalDate(2023, 4, i)}')")
        }
    }

    @Test
    fun `unsafe insert should work`() {
        val date: LocalDate = LocalDate(2023, 8, 5)
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES ('$date')")
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `inserts should work`() {
        val date: LocalDate = LocalDate(2023, 8, 5)
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES :test",
            mapOf("test" to date.toSQLParseable())
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `queries should work`() {
        insertRows(3)
        val result = q4k.query<DateTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 3
    }
}


class TestDateTime : TypeTest {

    override val tableName: String = "test_table"

    @Serializable
    data class TimeTable(
        val id: Long,
        val test: LocalDateTime
    )

    @BeforeEach
    override fun beforeEach() {
        q4k.execute("""
            CREATE TABLE $tableName (
                id BIGSERIAL PRIMARY KEY NOT NULL,
                test TIMESTAMP NOT NULL
            );
        """.trimIndent())
    }

    override fun insertRows(rowsCount: Int) {
        (1..rowsCount).forEach { i ->
            q4k.execute("INSERT INTO $tableName (test) VALUES ('${LocalDateTime(2023, 8, 5, 12, i)}')")
        }
    }

    @Test
    fun `unsafe insert should work`() {
        val dateTime = LocalDateTime(2023, 8, 5, 12, 30)
        println(dateTime.toString())
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES ('$dateTime')")
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `inserts should work`() {
        val result = q4k.execute("INSERT INTO $tableName (test) VALUES :test",
            mapOf("test" to LocalDateTime(2023, 8, 5, 12, 30).toSQLParseable())
        )
        result.shouldBeRight() shouldBeEqual 1
    }

    @Test
    fun `queries should work`() {
        insertRows(3)
        val result = q4k.query<TimeTable>("SELECT * FROM $tableName")
        result.shouldBeRight() shouldHaveSize 3
    }
}

