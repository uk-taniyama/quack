package com.koushikdutta.quack;

import junit.framework.TestCase;

import org.junit.Test;

public class QuackExceptionTest extends TestCase {
    @Test
    public void testSyntaxError() {
        // com.koushikdutta.quack.QuackException: SyntaxError: expecting ')'
        //     at JavaScript.*(?:1)
        try (QuackContext quackContext = QuackContext.create()) {
            try {
                quackContext.evaluate("^SyntaxError");
                fail();
            } catch (QuackException e) {
                assertEquals("SyntaxError: unexpected token in expression: '^'", e.getMessage());
                StackTraceElement stockTrace = e.getStackTrace()[0];
                assertEquals("JavaScript", stockTrace.getClassName());
                assertEquals("*", stockTrace.getMethodName());
                assertEquals(1, stockTrace.getLineNumber());
                assertEquals("?", stockTrace.getFileName());
            }
        }
    }

    @Test
    public void testReferenceError() {
        // ReferenceError: 'a' is not defined
        //    at <eval> (?:1)
        try (QuackContext quackContext = QuackContext.create()) {
            try {
                quackContext.evaluate("a.foo()");
                fail();
            } catch (QuackException e) {
                assertEquals("ReferenceError: 'a' is not defined", e.getMessage());
                StackTraceElement stockTrace = e.getStackTrace()[0];
                assertEquals("<javascript>", stockTrace.getClassName());
                assertEquals("<eval>", stockTrace.getMethodName());
                assertEquals(1, stockTrace.getLineNumber());
                assertEquals("?", stockTrace.getFileName());
            }
        }
    }

    @Test
    public void testError() {
        // Error: message
        //    at <eval> (?)
        try (QuackContext quackContext = QuackContext.create()) {
            try {
                quackContext.evaluate("throw new Error('message')");
                fail();
            } catch (QuackException e) {
                assertEquals("Error: message", e.getMessage());
                StackTraceElement stockTrace = e.getStackTrace()[0];
                assertEquals("<javascript>", stockTrace.getClassName());
                assertEquals("<eval>", stockTrace.getMethodName());
                assertEquals(0, stockTrace.getLineNumber());
                assertEquals("?", stockTrace.getFileName());
            }
        }
    }


    @Test
    public void testMessage() {
        // throw String......
        try (QuackContext quackContext = QuackContext.create()) {
            try {
                quackContext.evaluate("throw 'message'");
                fail();
            } catch (QuackException e) {
                assertEquals("message", e.getMessage());
                StackTraceElement stockTrace = e.getStackTrace()[0];
                System.out.println(stockTrace.toString());
                assertEquals("com.koushikdutta.quack.QuackContext", stockTrace.getClassName());
                assertEquals("evaluate", stockTrace.getMethodName());
                assertEquals(-2, stockTrace.getLineNumber());   // (Native Method)
                assertEquals("QuackContext.java", stockTrace.getFileName());
            }
        }
    }
}