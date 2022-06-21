/*
 * Copyright 2022 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.graphql.dgs.internal

import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JBarInput
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JFooInput
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JGenericField
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JInputObject
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JInputObjectWithPublicAndPrivateFields
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JInputObjectWithSet
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JInstrumentedInput
import com.netflix.graphql.dgs.internal.java.test.inputobjects.JListOfListsOfLists
import com.netflix.graphql.dgs.internal.java.test.inputobjects.sortby.JMovieSortBy
import com.netflix.graphql.dgs.internal.java.test.inputobjects.sortby.JMovieSortByField
import com.netflix.graphql.dgs.internal.java.test.inputobjects.sortby.JMovieSortByPublicField
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.reflect.ParameterizedType
import java.time.LocalDateTime

class AccessorTest {

    @Test
    fun `can tell if object has property`() {
        val accessor = Accessor(JInputObject::class.java)

        assertThat(accessor.hasProperty("simpleString")).isTrue
        assertThat(accessor.hasProperty("someDate")).isTrue
        assertThat(accessor.hasProperty("someObject")).isTrue
        assertThat(accessor.hasProperty("nonExistent")).isFalse
    }

    @Test
    fun `can tell if object has field`() {
        val accessor = Accessor(JInputObjectWithPublicAndPrivateFields::class.java)

        assertThat(accessor.hasProperty("simpleString")).isTrue
        assertThat(accessor.hasProperty("nonExistent")).isFalse
    }

    @Test
    fun `finds types of properties with basic types`() {
        val accessor = Accessor(JInputObject::class.java)

        assertThat(accessor.getPropertyType("simpleString")).isEqualTo(String::class.java)
        assertThat(accessor.getPropertyType("someDate")).isEqualTo(LocalDateTime::class.java)
        assertThat(accessor.getPropertyType("someObject")).isEqualTo(JInputObject.SomeObject::class.java)
    }

    @Test
    fun `finds direct field types when no setter methods are exposed`() {
        val accessor = Accessor(JInputObjectWithPublicAndPrivateFields::class.java)

        assertThat(accessor.getPropertyType("simpleString")).isEqualTo(String::class.java)
    }

    @Test
    fun `finds contained object type of generic collection property`() {
        val accessor = Accessor(JFooInput::class.java)

        assertThat(accessor.getPropertyType("bars")).isEqualTo(JBarInput::class.java)
    }

    @Test
    fun `finds contained object type of generic collection field`() {
        val accessor = Accessor(JGenericField::class.java)

        assertThat(accessor.getPropertyType("bars")).isEqualTo(JBarInput::class.java)
    }

    @Test
    fun `finds type of generic subclass property`() {
        val accessor = Accessor(JMovieSortBy::class.java)

        assertThat(accessor.getPropertyType("field")).isEqualTo(JMovieSortByField::class.java)
    }

    @Test
    fun `finds type of generic subclass field`() {
        val accessor = Accessor(JMovieSortByPublicField::class.java)

        assertThat(accessor.getPropertyType("field")).isEqualTo(JMovieSortByField::class.java)
    }

    @Test
    fun `finds raw property type`() {
        assertThat(Accessor(JFooInput::class.java).getRawPropertyType("bars")).isEqualTo(List::class.java)
        assertThat(Accessor(JInputObjectWithSet::class.java).getRawPropertyType("items")).isEqualTo(Set::class.java)
        assertThat(Accessor(JInputObjectWithPublicAndPrivateFields::class.java).getRawPropertyType("simpleString")).isEqualTo(String::class.java)
    }

    @Test
    fun `finds type of generic collection containing generic collections`() {
        val accessor = Accessor(JListOfListsOfLists.JListOfListOfFilters::class.java)

        assertThat(accessor.getPropertyType("lists")).isInstanceOf(ParameterizedType::class.java)
    }

    @Test
    fun `calls setter method when setting property`() {
        val instance = JInstrumentedInput()
        val accessor = Accessor(JInstrumentedInput::class.java)

        accessor.trySet(instance, "simpleString", "hello")

        assertThat(instance.simpleString).isEqualTo("hello")
        assertThat(instance.wasSetterCalled()).isTrue
    }

    @Test
    fun `setting property value of wrong type should throw exception`() {
        val instance = JInputObject()
        val accessor = Accessor(JInputObject::class.java)

        assertThatThrownBy { accessor.trySet(instance, "simpleString", 1) }.isInstanceOf(
            DgsInvalidInputArgumentException::class.java
        ).hasMessageStartingWith("Invalid input argument `1` for field `simpleString` on type `com.netflix.graphql.dgs.internal.java.test.inputobjects.JInputObject`")
    }

    @Test
    fun `can set field directly`() {
        val instance = JInputObjectWithPublicAndPrivateFields()
        val accessor = Accessor(JInputObjectWithPublicAndPrivateFields::class.java)

        assertThat(accessor.hasProperty("simpleString")).isTrue
        assertThat(accessor.hasProperty("simplePrivateInt")).isTrue

        accessor.trySet(instance, "simpleString", "hello")
        accessor.trySet(instance, "simplePrivateInt", 1)

        assertThat(instance.simpleString).isEqualTo("hello")
        assertThat(instance.simplePrivateInt).isEqualTo(1)
    }

    @Test
    fun `setting field value of wrong type should throw exception`() {
        val instance = JInputObjectWithPublicAndPrivateFields()
        val accessor = Accessor(JInputObjectWithPublicAndPrivateFields::class.java)

        assertThatThrownBy { accessor.trySet(instance, "simpleString", 1) }.isInstanceOf(
            DgsInvalidInputArgumentException::class.java
        ).hasMessageStartingWith("Invalid input argument `1` for field `simpleString` on type `com.netflix.graphql.dgs.internal.java.test.inputobjects.JInputObjectWithPublicAndPrivateFields`")
    }
}
