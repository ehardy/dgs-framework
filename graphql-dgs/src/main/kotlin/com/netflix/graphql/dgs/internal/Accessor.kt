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
import org.springframework.beans.ConfigurablePropertyAccessor
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.core.convert.TypeDescriptor
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class Accessor(target: Any) {
    private val propertyAccessor = PropertyAccessorFactory.forBeanPropertyAccess(target)
    private val fieldAccessor = PropertyAccessorFactory.forDirectFieldAccess(target)

    fun hasProperty(propertyName: String): Boolean {
        return propertyAccessor.isWritableProperty(propertyName) || fieldAccessor.isWritableProperty(propertyName)
    }

    fun getPropertyType(propertyName: String): Type {
        var descriptor = propertyAccessor.getPropertyTypeDescriptor(propertyName)

        if (descriptor != null) {
            return typeOf(propertyName, descriptor, propertyAccessor)
        }

        descriptor = fieldAccessor.getPropertyTypeDescriptor(propertyName)
        if (descriptor == null) {
            throw IllegalStateException("No property named `$propertyName` found, have you checked with hasProperty()?")
        }

        return typeOf(propertyName, descriptor, fieldAccessor)
    }

    private fun typeOf(propertyName: String, descriptor: TypeDescriptor, accessor: ConfigurablePropertyAccessor): Type {
        val resolvable = descriptor.resolvableType
        val type = resolvable?.type

        if (type is ParameterizedType && type.actualTypeArguments.size == 1) {
            return type.actualTypeArguments[0]
        }

        return accessor.getPropertyType(propertyName)!!
    }

    fun trySet(propertyName: String, value: Any?) {
        if (value != null && !getRawPropertyType(propertyName).isAssignableFrom(value?.javaClass)) {
            throw DgsInvalidInputArgumentException("Invalid input argument `$value` for field `$propertyName` on type `${propertyAccessor.wrappedInstance?.javaClass?.name}`")
        }

        if (propertyAccessor.isWritableProperty(propertyName)) {
            propertyAccessor.setPropertyValue(propertyName, value)
        } else {
            fieldAccessor.setPropertyValue(propertyName, value)
        }
    }

    fun getRawPropertyType(propertyName: String): Class<*> {
        val property = propertyAccessor.getPropertyType(propertyName)

        if (property != null) {
            return property
        }

        return fieldAccessor.getPropertyType(propertyName)!!
    }
}
