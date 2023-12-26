/**
 * The "me.opkarol.oporm" package contains classes for Object-Relational Mapping (ORM) in Java applications.
 * This ORM provides a high-level abstraction for interacting with relational databases.
 *
 * <p>The main class, {@link me.opkarol.oporm.OpOrm}, offers features such as CRUD operations, dynamic table creation,
 * transaction management, asynchronous operations, and more. The ORM aims to simplify database interactions and
 * promote efficient and maintainable code.
 *
 * <p>The package also includes an asynchronous version of the ORM, {@link me.opkarol.oporm.AsyncOpOrm}, which extends
 * the base ORM to support asynchronous execution of database operations using {@link java.util.concurrent.CompletableFuture}.
 *
 * <p>Annotations, such as {@link me.opkarol.oporm.Id}, are used for customization, and reflection is employed for
 * mapping Java objects to database records. The code is organized for modularity and extendability.
 *
 * <p>The base class for entities to be managed by the ORM is {@link me.opkarol.oporm.DatabaseEntity}. This class
 * serves as a marker interface to identify objects that can be persisted in the database.
 *
 * @since 1.0
 * @version 1.0
 */
package me.opkarol.oporm;
