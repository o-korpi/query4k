# query4k
*A functional library which provides a simple and streamlined way to interact
with SQL databases.*

Most recent version: 0.0.1

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


---
## Documentation

Complete documentation can be found at [query4k.com](www.query4k.com). 


### Installation
Query4k requires Kotlinx Serialization to run. 

```kotlin
plugins {
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}
```

For now, query4k is not on the maven repository. To use you must
download and compile the project.

### Usage

First create an instance of `Query4k`, using your preferred method. 
Example:
```kotlin
val q4k = Query4k.create("postgresql://postgres:postgres@localhost:5432/postgres")
```

With an instance of `Query4k`, we can now interact with our database:
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
With our model defined, we can use the `mappingQuery` method:
```kotlin
q4k.mappingQuery<User>("SELECT * FROM users")
```
Like the `execute` method, we can also provide injection-safe parameters.


