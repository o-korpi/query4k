
---

# query4k

Most recent version: 1.0.0  

---

Interact with your database in a safe and efficient manner, without the 
burden of ORMs. 

Query4k was developed with the aim of offering a straightforward and 
simple solution for accessing your database. 
It eliminates the need for intricate setups or cumbersome ORMs, 
ensuring a user-friendly experience without the burden of handling 
low-level intricacies.


Adding query4k to your project is as simple as decorating your data classes 
with `@Serializable`! 

Tested with H2 and PostgreSQL, should work with any SQL database.

---
## Getting started

More details at https://github.com/o-korpi/query4k/wiki/Documentation. 

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

If you want to make use of UUIDs you also need the following dependency:
`implementation("app.softwork:kotlinx-uuid-core:0.0.21")`

For now, UUIDs cannot be inserted in an injection-safe manner, and direct insertions
together with `.toSQLParseable()` are necessary.

Query4k is not yet on the maven repository. To use you must
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

