package com.koushikdutta.quack;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class QuackCoercions {
    static class Chain implements QuackCoercion<Object, Object> {
        private QuackCoercion<Object, Object> prev;
        QuackCoercion<Object, Object> curr;

        @Override
        @SuppressWarnings("rawtypes")
        public Object coerce(Class clazz, Object o) {
            Object result = this.prev.coerce(clazz, o);
            if (result != null) {
                return result;
            }
            return this.curr.coerce(clazz, o);
        }

        public static QuackCoercion<Object, Object> chain(QuackCoercion<Object, Object> prev, QuackCoercion<Object, Object> curr) {
            if (prev == null) {
                return curr;
            }
            Chain chain = new Chain();
            chain.curr = curr;
            chain.prev = prev;
            return chain;
        }
    }

    /**
     * put coercion to be chained.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putChain(Map<Class, QuackCoercion> target, QuackCoercion<Object, Object> coercion) {
        target.put(Object.class, Chain.chain(target.get(Object.class), coercion));
    }

    /**
     * put coercion to JavaScriptObject From Java Object
     */
    public static void putFromObject(QuackContext quackContext, QuackCoercion<Object, Object> coercion) {
        putChain(quackContext.JavaToJavascriptCoercions, coercion);
    }

    /**
     * put coercion from JavaScriptObject to Java Object
     */
    public static void putToObject(QuackContext quackContext, QuackCoercion<Object, Object> coercion) {
        putChain(quackContext.JavaScriptToJavaCoercions, coercion);
    }

    private QuackCoercions() {
    }

    private static final Date invalidDate = new Date(-1);

    private static final DateFormat dateISOformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    static {
        dateISOformat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    synchronized static Date parseISODate(String str) throws ParseException {
        return dateISOformat.parse(str);
    }

    /**
     * put coercion from JavaScriptObject to Java-Date
     */
    public static void putToDate(QuackContext quackContext) {
        quackContext.putJavaScriptToJavaCoercion(Date.class, (clazz, o) -> {
            if (o instanceof Number) {
                return new Date(((Number) o).longValue());
            }

            String str = o.toString();
            try {
                return new Date(str);
            } catch (IllegalArgumentException ignore) {
            }
            try {
                // 2022-05-08T04:47:44.193Z
                return parseISODate(str);
            } catch (ParseException ignore) {
            }

            return invalidDate;
        });
    }

    /**
     * put coercion to JavaScriptObject from Java-List
     */
    public static void putFromList(QuackContext quackContext) {
        quackContext.putJavaToJavaScriptCoercion(List.class, (clazz, o) -> {
            JavaScriptObject jso = quackContext.evaluateForJavaScriptObject("([])");
            int index = 0;
            for (Object obj : o) {
                jso.set(index++, quackContext.coerceJavaToJavaScript(obj));
            }
            return jso;
        });
    }

    /**
     * put coercion to JavaScriptObject from Java-Array
     */
    public static void putFromArray(QuackContext quackContext) {
        putFromObject(quackContext, (clazz, o) -> {
            if (!clazz.isArray()) {
                return null;
            }
            JavaScriptObject jso = quackContext.evaluateForJavaScriptObject("([])");
            for (int index = 0; index < Array.getLength(o); index += 1) {
                Object value = Array.get(o, index);
                jso.set(index, quackContext.coerceJavaToJavaScript(value));
            }
            return jso;
        });
    }

    /**
     * put coercion from JavaScriptObject to Java-List
     */
    public static void putToList(QuackContext quackContext) {
        quackContext.putJavaScriptToJavaCoercion(List.class, (clazz, o) -> {
            if (o instanceof JavaScriptObject) {
                return JavaScriptList.of((JavaScriptObject) o);
            }
            return null;
        });
    }

    /**
     * put coercion to JavaScriptObject from Java-Map
     */
    public static void putFromMap(QuackContext quackContext) {
        quackContext.putJavaToJavaScriptCoercion(Map.class, (clazz, o) -> {
            JavaScriptObject jso = quackContext.evaluateForJavaScriptObject("({})");
            for (Object obj : o.entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
                jso.set(entry.getKey().toString(), quackContext.coerceJavaToJavaScript(entry.getValue()));
            }
            return jso;
        });
    }

    /**
     * put coercion from JavaScriptObject to Java-Map
     */
    public static void putToMap(QuackContext quackContext) {
        quackContext.putJavaScriptToJavaCoercion(Map.class, (clazz, o) -> {
            if (o instanceof JavaScriptObject) {
                return JavaScriptMap.of((JavaScriptObject) o);
            }
            return null;
        });
    }
}
