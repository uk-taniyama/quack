package com.koushikdutta.quack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuackCoercionsTest {
    public static void assertEqualsJSON(String expected, Object actual) {
        try {
            assertTrue(actual instanceof String);
            JSONAssert.assertEquals(expected, actual.toString(), true);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testJavaScriptToJava() {
        try (QuackContext quackContext = QuackContext.create()) {
            QuackCoercions.putToMap(quackContext);
            QuackCoercions.putToList(quackContext);

            Map<?, ?> obj = quackContext.evaluate("({\"str\":\"S\",\"array\":[10,\"X\"],\"list\":[10,\"X\"],\"map\":{\"b\":\"B\"}})", Map.class);
            assertEquals(4, obj.size());

            Object jsArray = obj.get("array");
            assertEquals(JavaScriptObject.class, jsArray.getClass());
            List<?> array = (List<?>) quackContext.coerceJavaScriptToJava(List.class, jsArray);
            assertEquals(2, array.size());
            assertEquals(10, array.get(0));
            assertEquals("X", array.get(1));

            Object jsList = obj.get("list");
            assertEquals(JavaScriptObject.class, jsList.getClass());
            List<?> list = (List<?>) quackContext.coerceJavaScriptToJava(List.class, jsList);
            assertEquals(2, list.size());
            assertEquals(10, list.get(0));
            assertEquals("X", list.get(1));

            Object jsMap = obj.get("map");
            assertEquals(JavaScriptObject.class, jsMap.getClass());
            Map<?, ?> map = (Map<?, ?>) quackContext.coerceJavaScriptToJava(Map.class, jsMap);
            assertEquals(1, map.size());
            assertEquals("B", map.get("b"));

            String str = (String) obj.get("str");
            assertEquals(str, "S");
        }
    }

    @Test
    public void testJavaScriptFromJava() {
        try (QuackContext quackContext = QuackContext.create()) {
            QuackCoercions.putFromMap(quackContext);
            QuackCoercions.putFromList(quackContext);
            QuackCoercions.putFromArray(quackContext);

            JavaScriptObject fn = quackContext.evaluateForJavaScriptObject("(function(x){ return x===undefined ? 'undefined' : JSON.stringify(x)})");

            Map<String, Object> map = new HashMap<>();
            map.put("b", "B");
            List<Object> list = new ArrayList<>();
            list.add(10);
            list.add("X");
            Map<String, Object> arg = new HashMap<>();
            arg.put("array", list.toArray());
            arg.put("list", list);
            arg.put("map", map);
            arg.put("str", "S");

            assertEqualsJSON("{\"str\":\"S\",\"array\":[10,\"X\"],\"list\":[10,\"X\"],\"map\":{\"b\":\"B\"}}", fn.call(arg));
        }
    }

    @Test
    public void testPutToDate() {
        try (QuackContext quackContext = QuackContext.create()) {
            QuackCoercions.putToDate(quackContext);

            Date date1 = quackContext.evaluate("1651985264193", Date.class);
            assertEquals(1651985264193L, date1.getTime());
            // Note 'Sun May 08 2022 13:47:44 GMT+0900'
            Date date2 = quackContext.evaluate("new Date(1651985264193)", Date.class);
            assertEquals(1651985264000L, date2.getTime());
            // Note '2022-05-08T04:47:44.193Z'
            Date date3 = quackContext.evaluate("new Date(1651985264193).toISOString()", Date.class);
            assertEquals(1651985264193L, date3.getTime());
        }
    }

    @Test
    public void testPutFromMap() {
        try (QuackContext quackContext = QuackContext.create()) {
            JavaScriptObject fn = quackContext.evaluateForJavaScriptObject("(function(map){ return JSON.stringify(map)})");

            Map<String, String> map = new HashMap<>();
            map.put("A", "a");
            map.put("B", "b");

            Object str1 = fn.call(map);
            assertNull(str1);

            QuackCoercions.putFromMap(quackContext);

            assertEqualsJSON("{\"A\":\"a\",\"B\":\"b\"}", fn.call(map));
        }
    }

    @Test
    public void testPutToMap() {
        try (QuackContext quackContext = QuackContext.create()) {
            try {
                Map<?, ?> map1 = quackContext.evaluate("({A:'a',B:'b'})", Map.class);
                System.out.println(map1.size());
                fail();
            } catch (QuackException e) {
                e.printStackTrace();
                assertEquals("TypeError: not a function", e.getMessage());
            }

            QuackCoercions.putToMap(quackContext);

            Map<?, ?> map = quackContext.evaluate("({A:'a',B:'b'})", Map.class);

            assertEquals(2, map.size());
            assertEquals("a", map.get("A"));
            assertEquals("b", map.get("B"));
        }
    }

    String STR(String... lines) {
        return String.join("\r\n", lines);
    }

    interface AsyncMethods0 {
        QuackFuture method();

        QuackFuture<?> methodUnknown();

        QuackFuture<String> methodString();

        QuackFuture<List> methodList();

        QuackFuture<List<String>> methodListString();

        QuackFuture<List<?>> methodListUnknown();
    }

    @Test
    public void testQuackFuture0() throws InterruptedException {
        try (QuackContext quackContext = QuackContext.create()) {
            QuackCoercions.putToList(quackContext);

            String script = STR(
                    "({",
                    "method: () => null,",
                    "methodUnknown: () => null,",
                    "methodString: () => 'string',",
                    "methodList: () => ['string'],",
                    "methodListString: () => ['string'],",
                    "methodListUnknown: () => ['string'],",
                    "})");
            AsyncMethods0 asyncMethods = quackContext.evaluate(script, AsyncMethods0.class);

            Object result = asyncMethods.method().get();
            assertNull(result);
            Object resultUnknown = asyncMethods.methodUnknown().get();
            assertNull(resultUnknown);
            String resultString = asyncMethods.methodString().get();
            assertEquals("string", resultString);
            List resultList = asyncMethods.methodList().get();
            assertEquals(1, resultList.size());
            assertEquals("string", resultList.get(0));
            List resultListString = asyncMethods.methodListString().get();
            assertEquals(1, resultListString.size());
            assertEquals("string", resultListString.get(0));
            List resultListUnknown = asyncMethods.methodListUnknown().get();
            assertEquals(1, resultListUnknown.size());
            assertEquals("string", resultListUnknown.get(0));
        }
    }

    class JavaObj {
        public void method(int params) {
        }
    }

    @Test
    public void testQuackFutureError() throws InterruptedException {
        try (QuackContext quackContext = QuackContext.create()) {
            QuackCoercions.putToList(quackContext);

            quackContext.getGlobalObject().set("javaObj", new JavaObj());
            String script = STR(
                    "({",
                    "method: async () => {",
                    "  javaObj.method('abc')",
                    "},",
                    "})");
            AsyncMethods0 asyncMethods = quackContext.evaluate(script, AsyncMethods0.class);
            try {
                asyncMethods.method().get();
                fail();
            } catch (NumberFormatException e) {
            }
        }
    }

    interface AsyncMethods {
        QuackFuture<Map<String, String>> method1(Map<String, String> val);

        QuackFuture<Map<String, String>> method2(Map<String, String> val);

        QuackFuture<?> error();
    }

    @Test
    public void testQuackFuture() throws InterruptedException {
        try (QuackContext quackContext = QuackContext.create()) {
            QuackCoercions.putToMap(quackContext);
            QuackCoercions.putFromMap(quackContext);

            String script = STR(
                    "({",
                    // Promise<Map> -> QuackFuture<Map>
                    "method1: async (v) => v,",
                    // Map -> QuackFuture<Map>
                    "method2: (v) => v,",
                    //
                    "error: () => {throw new Error('error')},",
                    "})");
            AsyncMethods asyncMethods = quackContext.evaluate(script, AsyncMethods.class);

            Map<String, String> val = new HashMap<>();
            val.put("A", "a");

            Map<String, String> val1 = asyncMethods.method1(val).get();
            assertEquals(val, val1);

            Map<String, String> val2 = asyncMethods.method2(val).get();
            assertEquals(val, val2);

            try {
                asyncMethods.error();
                fail();
            } catch (QuackException e) {
                assertEquals("Error: error", e.getMessage());
            }
        }
    }

    @Test
    public void testAsync() throws InterruptedException {
        QuackContext quackContext = QuackContext.create();

        String script = STR(
                "(async (success) => {",
                "if(success) return true",
                "throw new Error('error')",
                "})"
        );

        JavaScriptObject fn = quackContext.evaluateForJavaScriptObject(script);
        QuackAsyncCaller<Boolean> asyncFn = QuackAsyncCaller.create(Boolean.class, fn);
        try {
            boolean data = asyncFn.call(true).get();
            assertTrue(data);
        } catch (QuackException e) {
            e.printStackTrace();
            fail();
        }
        try {
            asyncFn.call(false).get();
            fail();
        } catch (QuackException e) {
            assertEquals("Error: error", e.getMessage());
        }
    }
}