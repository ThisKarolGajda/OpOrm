package me.opkarol.oporm;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncOpORM extends OpORM {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public AsyncOpORM(String url, String user, String password) {
        super(url, user, password);
    }

    @Override
    public void save(DatabaseEntity entity) {
        CompletableFuture.runAsync(() -> super.save(entity), executorService);
    }

    @Override
    public void createTable(@NotNull Class<? extends DatabaseEntity> entityClass) {
        CompletableFuture.runAsync(() -> super.createTable(entityClass), executorService);
    }

    @Override
    public <T extends DatabaseEntity> T findById(@NotNull Class<T> entityClass, int id) {
        return CompletableFuture.supplyAsync(() -> super.findById(entityClass, id), executorService).join();
    }

    @Override
    public void deleteById(@NotNull Class<? extends DatabaseEntity> entityClass, int id) {
        CompletableFuture.runAsync(() -> super.deleteById(entityClass, id), executorService);
    }
}
