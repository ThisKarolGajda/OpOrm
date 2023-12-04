# OpORM (Object-Relational Mapping for Java)

OpORM is a lightweight Object-Relational Mapping library for Java, designed to simplify database interactions and provide a high-level abstraction for managing relational databases.

## Features

### Base ORM (me.opkarol.oporm.ORM)

- **CRUD Operations**: Perform Create, Read, Update, and Delete operations on database entities.
- **Dynamic Table Creation**: Automatically generate database tables based on Java entity classes.
- **Transaction Management**: Support for transactional operations, including commit and rollback.
- **Asynchronous Operations**: Execute database operations asynchronously using CompletableFuture.
- **Connection Pooling**: Utilize connection pooling for efficient database connections.

### AsyncORM (me.opkarol.oporm.AsyncORM)

- **Async Execution**: Asynchronous versions of CRUD and other operations for improved concurrency.
- **CompletableFuture Integration**: Leverage Java's CompletableFuture for handling asynchronous tasks.

### DatabaseEntity (me.opkarol.oporm.DatabaseEntity)

- **Marker Interface**: DatabaseEntity serves as a marker interface for objects that can be persisted in the database.

## Getting Started

1. **Include Dependency**: Add the OPORM library to your project.
   ```xml
   <!-- Maven -->
   <dependency>
      <groupId>com.github.ThisKarolGajda</groupId>
      <artifactId>OpOrm</artifactId>
      <version>-SNAPSHOT</version>
   </dependency>

   <repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
   
    ```

2. **Create ORM Instance**: Initialize the ORM with your database connection details.
    ```java
    ORM orm = new ORM("jdbc:mysql://localhost:3306/your_database", "your_user", "your_password");
    ```

3. **Define Entity Classes**: Create Java classes representing entities you want to persist.

4. **Usage Examples**: Use the ORM to perform database operations.

    ```java
    // Example: Creating a table for an entity class
    orm.createTable(YourEntityClass.class);
    
    // Example: Saving an entity
    YourEntityClass entity = new YourEntityClass(/* provide values */);
    orm.save(entity);
    ```
