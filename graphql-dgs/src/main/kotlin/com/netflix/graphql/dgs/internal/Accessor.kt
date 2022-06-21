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
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class Accessor(private val targetClass: Class<*>) {

    fun hasProperty(propertyName: String): Boolean {
        val method = methodFor(propertyName)

        if (method != null) {
            return true
        }

        return fieldFor(propertyName) != null
    }

    private fun fieldFor(propertyName: String): Field? {
        return ReflectionUtils.findField(targetClass, propertyName)
    }

    private fun methodFor(propertyName: String): Method? {
        val methodName = toMethodName(propertyName)
        val methods = ReflectionUtils.getAllDeclaredMethods(targetClass)

        //We need to get the Method instance from the class it is actually declared in (base class compared to subclass),
        //in order to have all the necessary parameter type information.
        return methods.findLast { m -> methodName == m.name }
    }

    private fun toMethodName(propertyName: String): String {
        if (propertyName.isEmpty()) {
            return propertyName
        }

        return "set" + propertyName.substring(0, 1).uppercase() + propertyName.substring(1)
    }

    fun getPropertyType(propertyName: String): Type {
        val genericSuperclass = targetClass.genericSuperclass
        val method = methodFor(propertyName)
        if (method != null) {
            return methodParameterType(method, genericSuperclass)
        }

        val field = fieldFor(propertyName)
            ?: throw IllegalStateException("No property named `$propertyName` found, have you checked with hasProperty()?")

        return fieldType(field, genericSuperclass)
    }

    private fun fieldType(field: Field, genericSuperclass: Type?): Type {
        return determineType(field.genericType, genericSuperclass, field.type)
    }

    private fun methodParameterType(method: Method, genericSuperclass: Type?): Type {
        val genericTypes = method.genericParameterTypes
        val parameterType = parameterTypeOf(method)

        return if (genericTypes.size == 1) determineType(genericTypes[0], genericSuperclass, parameterType) else parameterType
    }

    private fun determineType(genericType: Type, genericSuperclass: Type?, declaredType: Class<*>): Type {
        if (genericType is ParameterizedType && genericType.actualTypeArguments.size == 1) {
            return genericType.actualTypeArguments[0]
        }

        if (genericSuperclass is ParameterizedType && genericType != declaredType) {
            val typeParameters = (genericSuperclass.rawType as Class<*>).typeParameters
            val indexOfTypeParameter = typeParameters.indexOfFirst { it.name == genericType.typeName }

            return genericSuperclass.actualTypeArguments[indexOfTypeParameter]
        }

        return declaredType
    }

    private fun parameterTypeOf(method: Method): Class<*> {
        val types = method.parameterTypes

        if (types.size == 1) {
            return types[0]
        }

        return Void::class.java
    }

    fun trySet(instance: Any, propertyName: String, value: Any?) {
        val method = methodFor(propertyName)

        if (method != null) {
            if (value != null && !parameterTypeOf(method).isAssignableFrom(value.javaClass)) {
                throw DgsInvalidInputArgumentException("Invalid input argument `$value` for field `$propertyName` on type `${targetClass.name}`")
            }

            ReflectionUtils.makeAccessible(method)
            method.invoke(instance, value)
        } else {
            val field = fieldFor(propertyName)

            if (field != null) {
                if (value != null && !field.type.isAssignableFrom(value.javaClass)) {
                    throw DgsInvalidInputArgumentException("Invalid input argument `$value` for field `$propertyName` on type `${targetClass.name}`")
                }

                ReflectionUtils.makeAccessible(field)
                ReflectionUtils.setField(field, instance, value)
            }
        }
    }

    fun getRawPropertyType(propertyName: String): Class<*> {
        val method = methodFor(propertyName)
        if (method != null) {
            return parameterTypeOf(method)
        }

        val field = fieldFor(propertyName)
            ?: throw IllegalStateException("No property named `$propertyName` found, have you checked with hasProperty()?")

        return field.type
    }
}
