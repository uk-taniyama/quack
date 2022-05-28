package com.koushikdutta.quack

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun QuackPromise.await(): Any {
    return suspendCoroutine<Any> { resume ->
        this.then {
            resume.resume(it)
        }.caught {
            try {
                if (it !is JavaScriptObject)
                    throw QuackException("JavaScript Error type not thrown")
                val jo: JavaScriptObject = it
                jo.quackContext.evaluateForJavaScriptObject("(function(t) { throw t; })").call(it)
            } catch (e: Throwable) {
                resume.resumeWithException(e)
            }
        }
    }
}

fun QuackContext.putFromObject(coercion: QuackCoercion<Any, Any>): QuackContext {
    QuackCoercions.putFromObject(this, coercion)
    return this
}

fun QuackContext.putToObject(coercion: QuackCoercion<Any, Any>): QuackContext {
    QuackCoercions.putToObject(this, coercion)
    return this
}

fun QuackContext.putToDate(): QuackContext {
    QuackCoercions.putToDate(this)
    return this
}

fun QuackContext.putFromList(): QuackContext {
    QuackCoercions.putFromList(this)
    return this
}

fun QuackContext.putFromArray(): QuackContext {
    QuackCoercions.putFromArray(this)
    return this
}

fun QuackContext.putToList(): QuackContext {
    QuackCoercions.putToList(this)
    return this
}

fun QuackContext.putFromMap(): QuackContext {
    QuackCoercions.putFromMap(this)
    return this
}

fun QuackContext.putToMap(): QuackContext {
    QuackCoercions.putToMap(this)
    return this
}

fun QuackContext.putUtilCoercion(): QuackContext {
    this.putFromMap().putToMap()
    this.putFromArray()
    this.putFromList().putToList()
    return this
}
