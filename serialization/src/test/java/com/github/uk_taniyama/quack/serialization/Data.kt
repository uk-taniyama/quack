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

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Data(val name: String = "", val value: String = "")

@Serializable
data class HasData(val name: String, val data: Data, val extra: String)

@Serializable
data class HasArray(val name: String, val array: Array<Data>)

@Serializable
data class HasList(val name: String, val list: List<Data> = emptyList())

@Serializable
data class HasMap(val name: String, val map: Map<String, Data>)

@Serializable
class HasContextual(
    @Contextual
    val date: Date
)

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

@Serializable
data class NumData(
    val byteVal: Byte,
    val shortVal: Short,
    val intVal: Int,
    val longVal: Long,
    val floatVal: Float,
    val doubleVal: Double,
    val charVal: Char,
)
