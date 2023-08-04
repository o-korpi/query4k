import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.query4k.Query4k

val dataSource = HikariDataSource(
    HikariConfig().apply {
        jdbcUrl = "jdbc:h2:~/test"
        username = "sa"
        password = ""
        driverClassName = "org.h2.Driver"
    }
)

val q4k = Query4k.create(dataSource)