/*
 * Copyright 2021 UK-taniyama.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.uk_taniyama.quack.serialization

import com.koushikdutta.quack.JavaScriptObject
import com.koushikdutta.quack.QuackCoercions
import com.koushikdutta.quack.QuackContext
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.lang.invoke.MethodHandles
import java.util.*

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

@ExperimentalSerializationApi
val DefaultSerializersModule: SerializersModule = SerializersModule {
    contextual(DateAsLongSerializer)
}

@ExperimentalSerializationApi
fun <T> convertFromJavaScript(
    jsObject: JavaScriptObject,
    deserializer: DeserializationStrategy<T>,
    serializersModule: SerializersModule
): T {
    val decoder = QuackDecoder(jsObject, serializersModule)
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> JavaScriptObject.convert(serializersModule: SerializersModule = DefaultSerializersModule): T =
    convertFromJavaScript(this, serializersModule.serializer(), serializersModule)

@ExperimentalSerializationApi
inline fun <reified T> JavaScriptObject.convert(deserializer: DeserializationStrategy<T>, serializersModule: SerializersModule = DefaultSerializersModule): T =
    convertFromJavaScript(this, deserializer, serializersModule)

@ExperimentalSerializationApi
fun <T> convertToJavaScript(
    quackContext: QuackContext,
    data: T,
    serializer: SerializationStrategy<T>,
    serializersModule: SerializersModule = DefaultSerializersModule
): Any? {
    val encoder = QuackEncoder(quackContext, serializersModule)
    encoder.encodeSerializableValue(serializer, data)
    return encoder.getValue()
}

@ExperimentalSerializationApi
inline fun <reified T> QuackContext.convertFrom(
    data: T,
    serializersModule: SerializersModule = DefaultSerializersModule
): Any? = convertToJavaScript(this, data, serializersModule.serializer(), serializersModule)

internal fun getSerializer(clazz: Class<*>): KSerializer<Any>? {
    if (!clazz.isAnnotationPresent(Serializable::class.java)) {
        return null
    }

    // findStaticGetter cannot be used because type of Companion is unknown
    val lookup: MethodHandles.Lookup = MethodHandles.lookup()
    val companion = lookup.unreflectGetter(clazz.getDeclaredField("Companion")).invoke()
    @Suppress("UNCHECKED_CAST")
    return companion.javaClass.getDeclaredMethod("serializer").invoke(companion) as KSerializer<Any>
}

/**
 * put coercion from JavaScriptObject to Kotlin-Serializable
 */
@ExperimentalSerializationApi
fun QuackContext.putToSerializable(
    serializersModule: SerializersModule = DefaultSerializersModule
): QuackContext {
    QuackCoercions.putToObject(this) { clazz: Class<*>, o: Any? ->
        if (o !is JavaScriptObject) {
            return@putToObject null
        }

        val serializer = getSerializer(clazz) ?: return@putToObject null
        val decoder = QuackDecoder(o, serializersModule)
        decoder.decodeSerializableValue(serializer)
    }
    return this
}

/**
 * put coercion to JavaScriptObject from Kotlin-Serializable
 */
@ExperimentalSerializationApi
fun QuackContext.putFromSerializable(
    serializersModule: SerializersModule = DefaultSerializersModule
): QuackContext {
    QuackCoercions.putFromObject(this) { clazz: Class<*>, o: Any ->
        val serializer = getSerializer(clazz) ?: return@putFromObject null
        val encoder = QuackEncoder(this, serializersModule)
        encoder.encodeSerializableValue(serializer, o)
        encoder.getValue()
    }
    return this
}

@ExperimentalSerializationApi
fun QuackContext.putSerializableCoercion(
    serializersModule: SerializersModule = DefaultSerializersModule
): QuackContext {
    return this.putToSerializable(serializersModule).putFromSerializable(serializersModule)
}
