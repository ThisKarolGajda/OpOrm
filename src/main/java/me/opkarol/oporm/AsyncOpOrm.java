package me.opkarol.oporm;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncOpOrm extends OpOrm {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public AsyncOpOrm(String url, String user, String password) {
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

    @Override
    public <T extends DatabaseEntity> List<T> findAll(@NotNull Class<T> entityClass) {
        return CompletableFuture.supplyAsync(() -> super.findAll(entityClass), executorService).join();
    }
}
