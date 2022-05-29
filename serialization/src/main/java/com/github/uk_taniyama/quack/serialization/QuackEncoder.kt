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
import com.koushikdutta.quack.QuackContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
class QuackEncoder(
    private val quackContext: QuackContext,
    override val serializersModule: SerializersModule
) : AbstractEncoder() {
    open class Converter(encoder: QuackEncoder) {
        private var value: Any? = null
        var parent: Converter? = null

        open fun setIndex(descriptor: SerialDescriptor, index: Int) {}
        open fun setValue(value: Any?) {
            this.value = value
        }

        open fun getValue(): Any? {
            return this.value
        }
    }

    private var converter: Converter = Converter(this)

    fun getValue(): Any? {
        return converter.getValue()
    }

    private fun createObject(): JavaScriptObject {
        return quackContext.evaluateForJavaScriptObject("({})")
    }

    private fun createArray(): JavaScriptObject {
        return quackContext.evaluateForJavaScriptObject("([])")
    }

    private class ObjectConverter(encoder: QuackEncoder) : Converter(encoder) {
        private val jso: JavaScriptObject = encoder.createObject()
        private var key: String? = null

        override fun setIndex(descriptor: SerialDescriptor, index: Int) {
            key = descriptor.getElementName(index)
        }

        override fun setValue(value: Any?) {
            if (key != null) {
                jso.set(key, value)
            }
        }

        override fun getValue(): Any {
            return jso
        }
    }

    private class ListConverter(encoder: QuackEncoder) : Converter(encoder) {
        private val jso: JavaScriptObject = encoder.createArray()
        private var index = 0

        override fun setIndex(descriptor: SerialDescriptor, index: Int) {
            this.index = index
        }

        override fun setValue(value: Any?) {
            jso.set(index, value)
        }

        override fun getValue(): Any {
            return jso
        }
    }

    private class OpenConverter(encoder: QuackEncoder) : Converter(encoder) {
        private var index: Int = -1
        private var typeName: String = ""
        private var typeValue: Any? = null

        override fun setIndex(descriptor: SerialDescriptor, index: Int) {
            this.index = index
            if (index == 0) {
                typeName = descriptor.getElementName(index)
            }
        }

        override fun setValue(value: Any?) {
            if (index == 0) {
                typeValue = value
                return
            }
            if (index == 1) {
                if (value is JavaScriptObject) {
                    value.set(typeName, typeValue)
                    super.setValue(value)
                    return
                }
            }
        }
    }

    private class MapConverter(encoder: QuackEncoder) : Converter(encoder) {
        private val jso: JavaScriptObject = encoder.createObject()
        private var index = 0
        private var key: String? = null

        override fun setIndex(descriptor: SerialDescriptor, index: Int) {
            this.index = index
        }

        override fun setValue(value: Any?) {
            when (index % 2) {
                0 -> key = value?.toString() ?: "null"
                1 -> jso.set(key, value)
            }
        }

        override fun getValue(): Any {
            return jso
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val child = when (descriptor.kind) {
            StructureKind.MAP -> MapConverter(this)
            StructureKind.LIST -> ListConverter(this)
            PolymorphicKind.OPEN -> OpenConverter(this)
            else -> ObjectConverter(this)
        }
        child.parent = converter
        converter = child
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val child = converter
        converter = child.parent ?: return
        converter.setValue(child.getValue())
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        converter.setIndex(descriptor, index)
        return true
    }

    override fun encodeValue(value: Any) {
        converter.setValue(value)
    }
}
