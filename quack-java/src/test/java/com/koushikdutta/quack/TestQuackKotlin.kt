package com.koushikdutta.quack

import org.junit.Assert
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class QuackKotlinTests {
    internal interface ArrayTypeInterface {
        fun foo(): Int
    }

    internal interface ArrayInterface {
        val numbers: Array<ArrayTypeInterface>
    }

    @Test
    fun testArray() {
        val quack = QuackContext.create()
        val iface = quack.evaluate("(function() { return [() => 2, () => 3, () => 4, () => 5] })", ArrayInterface::class.java)
        var total = 0
        for (i in iface.numbers) {
            total += i.foo()
        }
        Assert.assertEquals(total.toLong(), 14)
        quack.close()
    }

    @Test
    fun testPromise() {
        val quack = QuackContext.create();

        val script = "new Promise((resolve, reject) => { resolve('hello'); });"
        val promise = quack.evaluate(script, QuackPromise::class.java)
//        val promise = jo.proxyInterface(QuackPromise::class.java)

        var ret = "world"
        val suspendFun = suspend {
            ret = promise.await() as String
        }

        suspendFun.startCoroutine(Continuation(EmptyCoroutineContext) {
        })

        assert(ret == "hello")
    }

    @Test
    fun testJavaScriptToJava() {
        QuackContext.create().use { quackContext ->
            quackContext.putToMap().putToList()

            val obj = quackContext.evaluate(
                "({\"str\":\"S\",\"array\":[10,\"X\"],\"list\":[10,\"X\"],\"map\":{\"b\":\"B\"}})",
                Map::class.java
            )
            Assert.assertEquals(4, obj.size.toLong())
            val jsArray = obj["array"]
            Assert.assertEquals(JavaScriptObject::class.java, jsArray!!.javaClass)
            val array = quackContext.coerceJavaScriptToJava(
                List::class.java,
                jsArray
            ) as List<*>
            Assert.assertEquals(2, array.size.toLong())
            Assert.assertEquals(10, array[0])
            Assert.assertEquals("X", array[1])
            val jsList = obj["list"]
            Assert.assertEquals(JavaScriptObject::class.java, jsList!!.javaClass)
            val list = quackContext.coerceJavaScriptToJava(
                MutableList::class.java,
                jsList
            ) as List<*>
            Assert.assertEquals(2, list.size.toLong())
            Assert.assertEquals(10, list[0])
            Assert.assertEquals("X", list[1])
            val jsMap = obj["map"]
            Assert.assertEquals(JavaScriptObject::class.java, jsMap!!.javaClass)
            val map = quackContext.coerceJavaScriptToJava(
                MutableMap::class.java,
                jsMap
            ) as Map<*, *>
            Assert.assertEquals(1, map.size.toLong())
            Assert.assertEquals("B", map["b"])
            val str = obj["str"] as String?
            Assert.assertEquals(str, "S")
        }
    }

    @Test
    fun testJavaScriptFromJava() {
        QuackContext.create().use { quackContext ->
            quackContext.putFromMap().putFromList().putFromArray()

            val fn =
                quackContext.evaluateForJavaScriptObject("(function(x){ return x===undefined ? 'undefined' : JSON.stringify(x)})")
            val map: MutableMap<String, Any> =
                HashMap()
            map["b"] = "B"
            val list: MutableList<Any> = ArrayList()
            list.add(10)
            list.add("X")
            val arg: MutableMap<String, Any> =
                HashMap()
            arg["array"] = list.toTypedArray()
            arg["list"] = list
            arg["map"] = map
            arg["str"] = "S"
            Assert.assertEquals(
                "{\"str\":\"S\",\"array\":[10,\"X\"],\"list\":[10,\"X\"],\"map\":{\"b\":\"B\"}}",
                fn.call(arg)
            )
        }
    }

}