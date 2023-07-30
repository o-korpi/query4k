
---

# query4k

Most recent version: 0.3.0

---

Query4k was developed with the aim of offering a straightforward and 
simple solution for accessing your database. 
It eliminates the need for intricate setups or cumbersome ORMs, 
ensuring a user-friendly experience without the burden of handling 
low-level intricacies.

To establish a robust API, the power of the Arrow library is leveraged. This
library uses
JDBI as the underlying query execution library and harnesses the capabilities
of Kotlinx Serialization to seamlessly convert query results to model objects. 

Adding query4k to your project is as simple as decorating your data classes 
with `@Serializable`! Query4k provides serializers for most JDBC types.

Despite most functions being wrapped with typed errors, crashing errors may
appear. These are left unhandled by the wrappers, as they are caused by
compilation-time issues such as missing serialization for a data class
or invalid SQL.

Tested with PostgreSQL, but should work with any SQL database.

---
## Documentation


### Installation
Query4k requires Kotlinx Serialization to run. You will also need the Arrow core
library to handle results from function calls.

```kotlin
plugins {
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("io.arrow-kt:arrow-core:1.2.0")
}
```

For now, query4k is not on the maven repository. To use you must
download and compile the project, or install the jar provided in releases.

### Usage

First create an instance of `Query4k`, using your preferred method.
```kotlin
val q4k = Query4k.create("postgresql://postgres:postgres@localhost:5432/postgres")
```

With an instance of `Query4k`, we can now interact with our database.
```kotlin
q4k.execute(
    "INSERT INTO users (email) VALUES (:email)", 
    mapOf("email" to "example@email.com")
)
```

Notice how SQL injection is dealt with.

To query our database, we first need to create our model. Models in query4k are
just simple serializable dataclasses.
```kotlin
@Serializable
data class User(
    val id: Long,
    val email: String
)
```
With our model defined, we can use the `query` method:
```kotlin
q4k.query<User>("SELECT * FROM users")
```
Like the `execute` method, we can also provide injection-safe parameters here.

To use transactions, we make use of the self-closing `transaction` block.
```kotlin
q4k.transaction {
    execute("INSERT INTO users (email) VALUES (:email)", mapOf("email" to "example"))
    println(query<User>("SELECT * FROM users"))
}
```

For  now the `transaction` block returns `Unit`. 

Not all types are serializable. If you need to use UUID or timestamps, you need 
custom serializers.
Additionally, arrays are not yet supported.