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
import com.koushikdutta.quack.QuackFuture
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat

@ExperimentalSerializationApi
class QuackConverterTest {
    @Test
    fun testData() {
        QuackContext.create().use { quackContext ->
            val json = "{\"name\":\"XXX\",\"value\":\"YYY\"}"
            val js = "($json)"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<Data>()
            assertEquals(Data("XXX", "YYY"), data)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testDataEmpty() {
        QuackContext.create().use { quackContext ->
            val json = "{\"name\":\"\",\"value\":\"\"}"
            val js = "({})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<Data>()
            assertEquals(Data(), data)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testDataEmptyName() {
        QuackContext.create().use { quackContext ->
            val json = "{\"name\":\"\",\"value\":\"YYY\"}"
            val js = "({value:'YYY'})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<Data>()
            assertEquals(Data("", "YYY"), data)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testDataEmptyValue() {
        QuackContext.create().use { quackContext ->
            val json = "{\"name\":\"XXX\",\"value\":\"\"}"
            val js = "({name:'XXX'})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<Data>()
            assertEquals(Data("XXX"), data)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testHasData() {
        QuackContext.create().use { quackContext ->
            val json =
                "{\"name\":\"XXX\",\"data\":{\"name\":\"YYY\",\"value\":\"\"},\"extra\":\"ZZZ\"}"
            val js = "({name:'XXX',data:{name:'YYY'},extra:'ZZZ'})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<HasData>()
            assertEquals(HasData("XXX", Data("YYY"), "ZZZ"), data)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testHasArray() {
        QuackContext.create().use { quackContext ->
            val json =
                "{\"name\":\"XXX\",\"array\":[{\"name\":\"YYY\",\"value\":\"\"},{\"name\":\"YYY\",\"value\":\"\"}]}"
            val js = "({name:'XXX',array:[{name:'YYY'},{name:'YYY'}]})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<HasArray>()
            assertEquals("XXX", data.name)
            assertEquals(2, data.array.size)
            assertEquals(Data("YYY"), data.array[0])
            assertEquals(Data("YYY"), data.array[1])

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testList() {
        QuackContext.create().use { quackContext ->
            val json = "[{\"name\":\"YYY\",\"value\":\"\"},{\"name\":\"YYY\",\"value\":\"\"}]"
            val js = "([{name:'YYY'},{name:'YYY'}])"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<List<Data>>()
            assertTrue(data is List)
            assertEquals(2, data.size)
            assertEquals(Data("YYY"), data[0])
            assertEquals(Data("YYY"), data[1])

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testHasList() {
        QuackContext.create().use { quackContext ->
            val json =
                "{\"name\":\"XXX\",\"list\":[{\"name\":\"YYY\",\"value\":\"\"},{\"name\":\"YYY\",\"value\":\"\"}]}"
            val js = "({name:'XXX',list:[{name:'YYY'},{name:'YYY'}]})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<HasList>()
            assertEquals("XXX", data.name)
            assertEquals(2, data.list.size)
            assertEquals(Data("YYY"), data.list[0])
            assertEquals(Data("YYY"), data.list[1])

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testHasListEmpty() {
        QuackContext.create().use { quackContext ->
            val json = "{\"name\":\"XXX\",\"list\":[]}"
            val js = "({name:'XXX'})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<HasList>()
            assertEquals("XXX", data.name)
            assertEquals(0, data.list.size)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testMap() {
        QuackContext.create().use { quackContext ->
            val json = "{\"A\":\"B\"}"
            val js = "({A:'B'})"
            val serializer = MapSerializer(String.serializer(), String.serializer())
            val data = quackContext.evaluateForJavaScriptObject(js).convert(serializer)
            assertEquals("class java.util.LinkedHashMap", data.javaClass.toString())
            assertEquals(mapOf("A" to "B"), data)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testHasMap() {
        QuackContext.create().use { quackContext ->
            val json = "{\"name\":\"XXX\",\"map\":{\"key\":{\"name\":\"YYY\",\"value\":\"\"}}}"
            val js = "({name:'XXX',map:{key:{name:'YYY'}}})"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<HasMap>()
            assertEquals("XXX", data.name)
            assertEquals(Data("YYY"), data.map["key"])

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testContextual() {
        QuackContext.create().use { quackContext ->
            val json = "{\"date\":1455494400000}"
            val expected =
                HasContextual(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
            val js = "($json)"
            val data = quackContext.evaluateForJavaScriptObject(js).convert<HasContextual>()
            assertEquals(expected.date, data.date)

            val any = quackContext.convertFrom(data)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    @Test
    fun testContextualEmptySerializersModule() {
        QuackContext.create().use { quackContext ->
            try {
                val json = "{\"date\":1455494400000}"
                val jsObject = quackContext.evaluateForJavaScriptObject("($json)")
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
    }

    @Test
    fun testPolyOpen() {
        QuackContext.create().use { quackContext ->
            val polyModule = SerializersModule {
                polymorphic(Project::class) {
                    subclass(OwnedProject::class)
                    default { UnknownProject.serializer() }
                }
            }

            val json = """
            [
                {"type":"unknown","name":"example"},
                {"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}
            ]
            """
            val polyJson = Json { serializersModule = polyModule }
            val data0 = polyJson.decodeFromString<List<Project>>(json)

            val js = "($json)"
            val data =
                quackContext.evaluateForJavaScriptObject(js).convert<List<Project>>(polyModule)
            assertEquals(data0, data)
            val unknown = data[0] as UnknownProject
            assertEquals("example", unknown.name)
            val owned = data[1] as OwnedProject
            assertEquals("kotlinx.serialization", owned.name)
            assertEquals("kotlin", owned.owner)

            // NOTE UnknownProject cannot convert
        }
    }

    @Test
    fun testPolyRegistered() {
        QuackContext.create().use { quackContext ->
            val polyModule = SerializersModule {
                polymorphic(Project::class) {
                    subclass(OwnedProject::class)
                    subclass(BasicProject::class)
                }
            }

            val json =
                "[{\"name\":\"example\",\"type\":\"BasicProject\"},{\"name\":\"kotlinx.serialization\",\"owner\":\"kotlin\",\"type\":\"OwnedProject\"}]"
            val polyJson = Json { serializersModule = polyModule }
            val data0 = polyJson.decodeFromString<List<Project>>(json)

            val js = "($json)"
            val data =
                quackContext.evaluateForJavaScriptObject(js).convert<List<Project>>(polyModule)
            assertEquals(data0, data)
            val basicProject = data[0] as BasicProject
            assertEquals("example", basicProject.name)
            val ownedProject = data[1] as OwnedProject
            assertEquals("kotlinx.serialization", ownedProject.name)
            assertEquals("kotlin", ownedProject.owner)

            // NOTE different type member position......
            // val json0 = polyJson.encodeToString(data)
            // assertEquals(json, json0)
            val any = quackContext.convertFrom(data, polyModule)
            assertTrue(any is JavaScriptObject)
            val jsObject = any as JavaScriptObject
            assertEquals(json, jsObject.stringify())
        }
    }

    internal interface DataInterface {
        fun syncMethod(value: Data): Data
        fun asyncMethod(value: Data): QuackFuture<Data>
    }

    @Test
    fun test() {
        QuackContext.create().use { quackContext ->
            quackContext.putToSerializable()
            quackContext.putFromSerializable()

            val js = """({
                syncMethod: (v) => { print(JSON.stringify(v)); return v},
                asyncMethod: async (v) => { print(JSON.stringify(v)); return v},
            })
            """
            val asyncMethods = quackContext.evaluate(js, DataInterface::class.java)
            val data = Data("XXX", "YYY")
            val syncResult = asyncMethods.syncMethod(data)
            val asyncResult = asyncMethods.asyncMethod(data).get()
            assertEquals(data, syncResult)
            assertEquals(data, asyncResult)
        }
    }

    @Test
    fun cast() {
        QuackContext.create().use { quackContext ->
            val jsZero = quackContext.evaluateForJavaScriptObject(
                """
                ({
                    byteVal: 0,
                    shortVal: 0,
                    intVal: 0,
                    longVal: 0,
                    floatVal: 0,
                    doubleVal: 0,
                    charVal: 0,
                })
                """.trimIndent()
            )
            val jZero = jsZero.convert<NumData>()
            assertEquals(0.toByte(), jZero.byteVal)
            assertEquals(0.toShort(), jZero.shortVal)
            assertEquals(0, jZero.intVal)
            assertEquals(0.toLong(), jZero.longVal)
            assertEquals(0.toFloat(), jZero.floatVal)
            assertEquals(0.toDouble(), jZero.doubleVal, 0.0)
            assertEquals(0.toChar(), jZero.charVal)
            val jsNumber = quackContext.evaluateForJavaScriptObject(
                """
                ({
                    byteVal: 0x7F,
                    shortVal: 0x7FFF,
                    intVal: 0x7FFFFFFF,
                    longVal: Number.MAX_SAFE_INTEGER,
                    floatVal: Number.MAX_SAFE_INTEGER,
                    doubleVal: Number.MAX_SAFE_INTEGER,
                    charVal: 0x7F,
                })
                """.trimIndent()
            )
            val jNumber = jsNumber.convert<NumData>()
            assertEquals(0x7F.toByte(), jNumber.byteVal)
            assertEquals(0x7FFF.toShort(), jNumber.shortVal)
            assertEquals(0x7FFFFFFF.toInt(), jNumber.intVal)
            assertEquals(9007199254740991.toLong(), jNumber.longVal)
            assertEquals(9007199254740991.toFloat(), jNumber.floatVal)
            assertEquals(9007199254740991.toDouble(), jNumber.doubleVal, 0.01)
            assertEquals(0x7F.toChar(), jNumber.charVal)
        }
    }
}
