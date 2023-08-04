import io.query4k.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TestTypes {


    @Serializable
    data class TestList(
        val id: Long,
        val test: List<String>
    )
    private val listTableName = "test_list"
    private val testListTable = """
        CREATE TABLE $listTableName (
            id BIGSERIAL PRIMARY KEY NOT NULL,
            test INTEGER ARRAY NOT NULL
        );
    """.trimIndent()

    @Serializable
    data class TestBigInt(
        val id: Long,
        @Serializable(with = BigDecimalSerializer::class)
        val test: BigDecimal
    )
    private val bigIntTableName = "test_big_int"
    private val testBigIntTable = """
        CREATE TABLE $bigIntTableName (
            id BIGSERIAL PRIMARY KEY NOT NULL,
            test NUMBER NOT NULL
        );
    """.trimIndent()

    @Serializable
    data class TestIntAndDouble(
        val id: Long,
        val test1: Int,
        val test2: Double
    )
    private val intDoubleTableName = "test_int_double"
    private val intDoubleTable = """
        CREATE TABLE $intDoubleTableName (
            id BIGSERIAL PRIMARY KEY NOT NULL,
            test1 INTEGER NOT NULL,
            test2 FLOAT NOT NULL
        );
    """.trimIndent()

    private fun setupTables() {
        q4k.transaction {
            q4k.execute(testListTable)
            q4k.execute(testBigIntTable)
            q4k.execute(intDoubleTable)
        }
    }

    @BeforeTest
    fun before() {
        try {
            q4k.execute("DROP TABLE $listTableName;")
            q4k.execute("DROP TABLE $bigIntTableName;")
            q4k.execute("DROP TABLE $intDoubleTableName;")
            setupTables()
        } catch (_: Exception) {}
    }

    @BeforeEach
    fun beforeEach() {
        setupTables()
    }

    @AfterEach
    fun afterEach() {
        try {
            q4k.execute("DROP TABLE $listTableName;")
            q4k.execute("DROP TABLE $bigIntTableName;")
            q4k.execute("DROP TABLE $intDoubleTableName;")
        } catch (_: Exception) {}
    }

    @Test
    fun testListType() {
        val insertUnsafe = q4k.execute(
            "INSERT INTO $listTableName (test) VALUES ('{ \"hello\", \"world\" }');"
        )
        assertTrue(insertUnsafe.isRight())

        println(q4k.query(q4k.handle(), "SELECT * FROM $listTableName"))
        val query = q4k.queryOnly<TestList>("SELECT * FROM $listTableName")
        assertTrue(query.isRight())

        val insertSafe = q4k.execute(
            "INSERT INTO $listTableName (test) VALUES (:list)",
            mapOf("list" to listOf("hello", "world", "again"))
        )
        assertTrue(insertSafe.isRight())

        assertEquals(
            2,
            q4k.query<TestList>("SELECT * FROM $listTableName").getOrNull()?.size
        )
    }

    @Test
    fun `test int and double`() {
        val insertSafeInt = q4k.execute(
            "INSERT INTO $intDoubleTableName (test1, test2) VALUES (:int, :double)",
            mapOf(
                "int" to 5,
                "double" to 10.5
            )
        )
        assertTrue(insertSafeInt.isRight())

        val query = q4k.queryOnly<TestIntAndDouble>("SELECT * FROM $intDoubleTableName")
        assertTrue(query.isRight())
    }

    @Test
    fun testBigDecimalType() {
        val insertUnsafe = q4k.execute(
            "INSERT INTO $bigIntTableName (test) VALUES (500.123);"
        )
        assertTrue(insertUnsafe.isRight())

        val query = q4k.queryOnly<TestBigInt>("SELECT * FROM $bigIntTableName")
        assertTrue(query.isRight())

        val insertSafe = q4k.execute(
            "INSERT INTO $bigIntTableName (test) VALUES (:value)",
            mapOf("value" to BigDecimal.valueOf(123.5))
        )
        assertTrue(insertSafe.isRight())

        assertEquals(
            2,
            q4k.query<TestBigInt>("SELECT * FROM $bigIntTableName").getOrNull()?.size
        )
    }

    @Test
    fun testUUID() {
        // TODO!
    }

    @Test
    fun testTimestamp() {
        // TODO!
    }

}