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

import com.koushikdutta.quack.JavaScriptList
import com.koushikdutta.quack.JavaScriptMap
import com.koushikdutta.quack.JavaScriptObject
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
class QuackDecoder(
    private val jsObject: JavaScriptObject,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    open class Converter {
        protected var value: Any? = null
        open fun next(descriptor: SerialDescriptor): Int =
            CompositeDecoder.DECODE_DONE

        fun value(): Any? = value
    }

    private var converter: Converter = Converter()

    private class ObjectConverter(
        private val jsObject: JavaScriptObject
    ) : Converter() {
        private var index = 0

        override fun next(descriptor: SerialDescriptor): Int {
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

    private class MapConverter(
        jsObject: JavaScriptObject
    ) : Converter() {
        private var index = 0
        private val iterator = JavaScriptMap.of(jsObject).entries.iterator()
        private var entry: Map.Entry<String, Any>? = null

        override fun next(descriptor: SerialDescriptor): Int {
            if (entry != null) {
                value = entry!!.value
                entry = null
                return index++
            }
            if (iterator.hasNext()) {
                entry = iterator.next()
                value = entry!!.key
                return index++
            }
            value = null
            return CompositeDecoder.DECODE_DONE
        }
    }

    private class ListConverter(
        jsObject: JavaScriptObject
    ) : Converter() {
        private val iterator = JavaScriptList.of(jsObject).iterator()
        private var index = 0

        override fun next(descriptor: SerialDescriptor): Int {
            if (iterator.hasNext()) {
                value = iterator.next()
                return index++
            }
            value = null
            return CompositeDecoder.DECODE_DONE
        }
    }

    private class OpenConverter(
        private val jsObject: JavaScriptObject
    ) : Converter() {
        private var index = 0

        override fun next(descriptor: SerialDescriptor): Int {
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
        converter.next(descriptor)

    override fun decodeValue(): Any {
        val v = converter.value()
        return v ?: 0
    }

    private fun decodeNumber(): Number {
        val v = converter.value()
        if (v is Number) {
            return v
        }
        return 0
    }

    override fun decodeByte(): Byte = decodeNumber().toByte()
    override fun decodeShort(): Short = decodeNumber().toShort()
    override fun decodeInt(): Int = decodeNumber().toInt()
    override fun decodeLong(): Long = decodeNumber().toLong()
    override fun decodeFloat(): Float = decodeNumber().toFloat()
    override fun decodeDouble(): Double = decodeNumber().toDouble()
    override fun decodeChar(): Char = decodeNumber().toChar()
    override fun decodeString(): String = decodeValue() as String

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        val value = converter.value()
        if (value is JavaScriptObject) {
            return convertFromJavaScript(value, deserializer, serializersModule)
        }
        return super.decodeSerializableValue(deserializer)
    }
}
