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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer
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

internal class JavaScriptObjectEntriesIterator(jsObject: JavaScriptObject) : Iterator<Any?> {
    private val entries: JavaScriptObject
    private val length: Int
    private var index1 = 0
    private var index2 = 0

    init {
        val fn = jsObject.quackContext.evaluateForJavaScriptObject("Object.entries")
        entries = fn.call(jsObject) as JavaScriptObject
        val length = entries["length"]
        if (length is Number) {
            this.length = length as Int
        } else {
            this.length = 0
        }
    }

    override fun hasNext(): Boolean {
        return index1 < length
    }

    override fun next(): Any? {
        val item = get()
        if (index2 == 0) { // key
            index2 = 1
        } else {
            index1 += 1
            index2 = 0
        }
        return item
    }

    private fun get(): Any? {
        val entry = entries[index1]
        return if (entry !is JavaScriptObject) {
            null
        } else entry[index2]
    }

    val index: Int
        get() = 2 * index1 + index2
}

@ExperimentalSerializationApi
class QuackConverter(
    private val jsObject: JavaScriptObject,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    open class Converter {
        protected var value: Any? = null
        open fun nextIndex(descriptor: SerialDescriptor): Int =
            CompositeDecoder.DECODE_DONE

        fun value(): Any? = value
    }

    var converter: Converter = Converter()

    internal class ObjectConverter(
        private val jsObject: JavaScriptObject
    ) : Converter() {
        private var index = 0

        override fun nextIndex(descriptor: SerialDescriptor): Int {
            while (index < descriptor.elementsCount) {
                val name = descriptor.getElementName(index)
                val curIndex = index++
                value = jsObject[name]
                if (value != null) {
                    return curIndex
                }
            }
            value = null
            return CompositeDecoder.DECODE_DONE
        }
    }

    internal class OpenConverter(
        private val jsObject: JavaScriptObject
    ) : Converter() {
        private var index = 0

        override fun nextIndex(descriptor: SerialDescriptor): Int {
            if (index == 0) {
                val name = descriptor.getElementName(index)
                val curIndex = index++
                value = jsObject[name]
                if (value != null) {
                    return curIndex
                }
                return CompositeDecoder.UNKNOWN_NAME
            }
            if (index == 1) {
                val curIndex = index++
                value = jsObject
                return curIndex
            }
            return CompositeDecoder.DECODE_DONE
        }
    }

    class MapConverter(
        jsObject: JavaScriptObject
    ) : Converter() {
        private val entries = JavaScriptObjectEntriesIterator(jsObject)

        override fun nextIndex(descriptor: SerialDescriptor): Int {
            if (entries.hasNext()) {
                val index = entries.index
                value = entries.next()
                return index
            }
            value = null
            return CompositeDecoder.DECODE_DONE
        }
    }

    class ListConverter(
        private val jsObject: JavaScriptObject,
    ) : Converter() {
        private val count = (jsObject["length"] as Number).toInt()
        private var index = 0

        override fun nextIndex(descriptor: SerialDescriptor): Int {
            if (index < count) {
                value = jsObject[index]
                return index++
            }
            value = null
            return CompositeDecoder.DECODE_DONE
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        converter = when (descriptor.kind) {
            StructureKind.MAP -> MapConverter(jsObject)
            StructureKind.LIST -> ListConverter(jsObject)
            PolymorphicKind.OPEN -> OpenConverter(jsObject)
            else -> ObjectConverter(jsObject)
        }
        return this
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        converter.nextIndex(descriptor)

    override fun decodeValue(): Any = converter.value() ?: 0

    override fun decodeByte(): Byte = decodeInt().toByte()
    override fun decodeShort(): Short = decodeInt().toShort()
    override fun decodeInt(): Int = decodeDouble().toInt()
    override fun decodeLong(): Long = decodeDouble().toLong()
    override fun decodeFloat(): Float = decodeDouble().toFloat()
    override fun decodeDouble(): Double = decodeValue() as Double
    override fun decodeChar(): Char = decodeInt().toChar()
    override fun decodeString(): String = decodeValue() as String

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        val value = converter.value()
        if (value is JavaScriptObject) {
            return convertJavaScriptObject(value, deserializer, serializersModule)
        }
        return super.decodeSerializableValue(deserializer)
    }
}

@ExperimentalSerializationApi
fun <T> convertJavaScriptObject(
    jsObject: JavaScriptObject,
    deserializer: DeserializationStrategy<T>,
    serializersModule: SerializersModule
): T {
    val converter = QuackConverter(jsObject, serializersModule)
    return converter.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> JavaScriptObject.convert(serializersModule: SerializersModule = DefaultSerializersModule): T =
    convertJavaScriptObject(this, serializersModule.serializer(), serializersModule)
