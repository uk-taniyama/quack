package com.koushikdutta.quack;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QuackFuture<T> {
    private final QuackContext quackContext;
    private final Class<T> clazz;
    private final Semaphore semaphore = new Semaphore(0);
    private Object data = null;
    private Exception error = null;

    private QuackFuture(Class<T> clazz, QuackContext quackContext) {
        this.clazz = clazz;
        this.quackContext = quackContext;
    }


    public static <T> QuackFuture<T> create(Class<T> clazz, QuackContext quackContext, Object ret) {
        QuackFuture<T> future = new QuackFuture<>(clazz, quackContext);
        if (!(ret instanceof JavaScriptObject)) {
            future.setData(ret);
            return future;
        }

        try {
            // Maybe promise.
            QuackPromise promise = ((JavaScriptObject) ret).proxyInterface(QuackPromise.class);
            promise.then(future::setData, future::setError);
            return future;
        } catch (QuackException e) {
            // NOTE 'TypeError: not a function'
            future.setData(ret);
            return future;
        }
    }

    private void setData(Object data) {
        this.data = data;
        semaphore.release();
    }

    private void setError(Object error) {
        if (error instanceof JavaScriptObject) {
            try {
                // rethrow
                quackContext.evaluateForJavaScriptObject("(function(e){throw e})").call(error);
            } catch(Exception e) {
                this.error = e;
            }
        } else if(error instanceof Exception) {
            this.error = (Exception) error;
        }
        semaphore.release();
    }

    public void await() throws InterruptedException {
        semaphore.acquire();
    }

    public boolean tryAwait() {
        return semaphore.tryAcquire();
    }

    public boolean tryAwait(long timeout, TimeUnit unit) throws InterruptedException {
        return semaphore.tryAcquire(timeout, unit);
    }

    @SuppressWarnings("unchecked")
    <U> U getRaw(Class<U> clazz) throws QuackException {
        if (error != null) {
            if (error instanceof QuackException) {
                throw (QuackException) error;
            }
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new RuntimeException(error);
        }
        return (U) quackContext.coerceJavaScriptToJava(clazz, data);
    }

    public <U> U get(Class<U> clazz) throws QuackException, InterruptedException {
        await();
        return getRaw(clazz);
    }

    public T get() throws QuackException, InterruptedException {
        return get(clazz);
    }

    public void join() throws QuackException, InterruptedException {
        get();
    }

    public <U> U tryGet(Class<U> clazz) throws QuackException, TimeoutException {
        if (!semaphore.tryAcquire()) {
            throw new TimeoutException();
        }
        return getRaw(clazz);
    }

    public T tryGet() throws QuackException, TimeoutException {
        return getRaw(clazz);
    }

    public <U> U tryGet(Class<U> clazz, long timeout, TimeUnit unit) throws QuackException, InterruptedException, TimeoutException {
        if (!tryAwait(timeout, unit)) {
            throw new TimeoutException();
        }
        return getRaw(clazz);
    }

    public T tryGet(long timeout, TimeUnit unit) throws QuackException, InterruptedException, TimeoutException {
        if (!tryAwait(timeout, unit)) {
            throw new TimeoutException();
        }
        return getRaw(clazz);
    }
}
