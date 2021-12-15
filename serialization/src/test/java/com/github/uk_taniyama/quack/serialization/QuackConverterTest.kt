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
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date

@ExperimentalSerializationApi
class QuackConverterTest {
    private val ctx = QuackContext.create()

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        ctx.close()
    }

    @Serializable
    data class Simple(val name: String = "", val value: String = "")

    @Test
    fun simpleObject() {
        val any = ctx.evaluate(
            """
            ({name:"XXX", value:"YYY"})
        """
        )
        val jsObject = any as JavaScriptObject
        val data = jsObject.convert<Simple>()
        assertEquals(Simple("XXX", "YYY"), data)
    }

    @Test
    fun simpleObjectEmpty() {
        val any = ctx.evaluate(
            """
            ({})
        """
        )
        val jsObject = any as JavaScriptObject
        val data = jsObject.convert<Simple>()
        assertEquals(Simple(), data)
    }

    @Test
    fun simpleObjectEmptyName() {
        val any = ctx.evaluate(
            """
            ({value:"YYY"})
        """
        )
        val jsObject = any as JavaScriptObject
        val data = jsObject.convert<Simple>()
        assertEquals(Simple("", "YYY"), data)
    }

    @Test
    fun simpleObjectEmptyValue() {
        val any = ctx.evaluate(
            """
            ({name:"XXX"})
        """
        )
        val jsObject = any as JavaScriptObject
        val data = jsObject.convert<Simple>()
        assertEquals(Simple("XXX"), data)
    }

    @Serializable
    data class Wrapped(val name: String, val simple: Simple)

    @Test
    fun testWrapped() {
        val any = ctx.evaluate(
            """
            ({name:"XXX",simple:{name:"YYY"}})
        """
        )
        val jsObject = any as JavaScriptObject
        val data = jsObject.convert<Wrapped>()
        assertEquals(Wrapped("XXX", Simple("YYY")), data)
    }

    @Serializable
    data class HasArray(val name: String, val simples: Array<Simple>)

    @Test
    fun testArray() {
        val any = ctx.evaluate(
            """
            ({name:"XXX",simples:[{name:"YYY"},{name:"YYY"}]})
        """
        )
        val jsObject = any as JavaScriptObject
        val data = jsObject.convert<HasArray>()
        assertEquals("XXX", data.name)
        assertEquals(2, data.simples.size)
        assertEquals(Simple("YYY"), data.simples[0])
        assertEquals(Simple("YYY"), data.simples[1])
    }

    @Serializable
    data class HasList(val name: String, val simples: List<Simple> = emptyList())

    @Test
    fun testList0() {
        val js = """
            ([{name:"YYY"},{name:"YYY"}])
        """
        val jsObject = ctx.evaluate(js) as JavaScriptObject
        val data0 = Json.decodeFromStream<List<Simple>>(jsObject.stringify().byteInputStream())
        val data = jsObject.convert<List<Simple>>()
        assertEquals(data0, data)
        assertEquals(2, data.size)
        assertEquals(Simple("YYY"), data[0])
        assertEquals(Simple("YYY"), data[1])
    }

    @Test
    fun testList() {
        val js = """
            ({name:"XXX",simples:[{name:"YYY"},{name:"YYY"}]})
        """
        val jsObject = ctx.evaluate(js) as JavaScriptObject
        val data0 = Json.decodeFromStream<HasList>(jsObject.stringify().byteInputStream())
        val data = jsObject.convert<HasList>()
        assertEquals(data0, data)
        assertEquals("XXX", data.name)
        assertEquals(2, data.simples.size)
        assertEquals(Simple("YYY"), data.simples[0])
        assertEquals(Simple("YYY"), data.simples[1])
    }

    @Test
    fun testListEmpty() {
        val js = """
            ({name:"XXX"})
        """
        val jsObject = ctx.evaluate(js) as JavaScriptObject
        val data0 = Json.decodeFromStream<HasList>(jsObject.stringify().byteInputStream())
        val data = jsObject.convert<HasList>()
        assertEquals(data0, data)
        assertEquals("XXX", data.name)
        assertEquals(0, data.simples.size)
    }

    @Serializable
    data class Maps(val name: String, val simples: Map<String, Simple>)

    @Test
    fun testMaps() {
        val any = ctx.evaluate(
            """
            ({name:"XXX",simples:{"key":{name:"YYY"}}})
        """
        )
        val jsObject = any as JavaScriptObject
        val data = jsObject.convert<Maps>()
        assertEquals("XXX", data.name)
        assertEquals(Simple("YYY"), data.simples["key"])
    }

    @Serializable
    class HasContextual(
        @Contextual
        val date: Date
    )

    private val format = Json { serializersModule = DefaultSerializersModule }

    @Test
    fun testContextual() {
        val data =
            HasContextual(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        val json = format.encodeToString(data)

        val jsObject = ctx.evaluateForJavaScriptObject("($json)")
        val javaObject = jsObject.convert<HasContextual>()

        assertEquals(data.date, javaObject.date)
        assertEquals(json, jsObject.stringify())
    }

    @Test
    fun testContextualEmpty() {
        try {
            val data =
                HasContextual(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
            val json = format.encodeToString(data)
            val jsObject = ctx.evaluateForJavaScriptObject("($json)")
            jsObject.convert<HasContextual>(EmptySerializersModule)
            fail()
        } catch (e: SerializationException) {
            println(e.message)
            assertEquals(
                """
                Serializer for class 'Date' is not found.
                Mark the class as @Serializable or provide the serializer explicitly.
                """.trimIndent(),
                e.message
            )
        }
    }

    @Serializable
    abstract class Project {
        abstract val name: String
    }

    @Serializable
    data class UnknownProject(override val name: String, val type: String) : Project()

    @Serializable
    @SerialName("BasicProject")
    data class BasicProject(override val name: String) : Project()

    @Serializable
    @SerialName("OwnedProject")
    data class OwnedProject(override val name: String, val owner: String) : Project()

    @Test
    fun testPolyOpen() {
        val polyModule = SerializersModule {
            polymorphic(Project::class) {
                subclass(OwnedProject::class)
                default { UnknownProject.serializer() }
            }
        }

        val polyJson = Json { serializersModule = polyModule }

        var json = """
            [
            {"type":"unknown","name":"example"},
            {"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}
            ]
            """
        val projects = polyJson.decodeFromString<List<Project>>(json)

        val jsObject = ctx.evaluateForJavaScriptObject("($json)")
        val javaObject = jsObject.convert<List<Project>>(polyModule)
        var unknown = javaObject[0] as UnknownProject
        assertEquals("example", unknown.name)
        var owned = javaObject[1] as OwnedProject
        assertEquals("kotlinx.serialization", owned.name)
        assertEquals("kotlin", owned.owner)
    }

    @Test
    fun testPolyRegistered() {
        val polyModule = SerializersModule {
            polymorphic(Project::class) {
                subclass(OwnedProject::class)
                subclass(BasicProject::class)
            }
        }

        val polyJson = Json { serializersModule = polyModule }

        var json = """
            [
            {"type":"BasicProject","name":"example"},
            {"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}
            ]
            """
        val projects = polyJson.decodeFromString<List<Project>>(json)

        val jsObject = ctx.evaluateForJavaScriptObject("($json)")
        val javaObject = jsObject.convert<List<Project>>(polyModule)
        var unknown = javaObject[0] as BasicProject
        assertEquals("example", unknown.name)
        var owned = javaObject[1] as OwnedProject
        assertEquals("kotlinx.serialization", owned.name)
        assertEquals("kotlin", owned.owner)
    }
}
