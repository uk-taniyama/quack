package com.koushikdutta.quack;

public interface QuackPromise {
    QuackPromise then(QuackPromiseReceiver onFulfilled, QuackPromiseReceiver onRejected);
    QuackPromise then(QuackPromiseReceiver onFulfilled);
    @QuackMethodName(name = "catch")
    QuackPromise caught(QuackPromiseReceiver onRejected);
}
