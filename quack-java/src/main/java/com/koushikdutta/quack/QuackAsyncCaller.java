package com.koushikdutta.quack;

public class QuackAsyncCaller<T> {
    private final Class<T> clazz;
    private final JavaScriptObject jso;

    private QuackAsyncCaller(Class<T> clazz, JavaScriptObject jso)
    {
        this.clazz = clazz;
        this.jso = jso;
    }

    public static <T> QuackAsyncCaller<T> create(Class<T> clazz, JavaScriptObject jso) {
        return new QuackAsyncCaller<>(clazz, jso);
    }

    private QuackFuture<T> toFuture(Object o) {
        return ((QuackFuture<T>) QuackFuture.create(clazz, jso.quackContext, o));
    }

    public QuackFuture<T> call(Object... args) {
        return toFuture(jso.call(args));
    }

    public QuackFuture<T> callProperty(Object property, Object... args) {
        return toFuture(jso.callProperty(property, args));
    }

    public QuackFuture<T> callMethod(Object thiz, Object... args) {
        return toFuture(jso.callMethod(thiz, args));
    }
}
