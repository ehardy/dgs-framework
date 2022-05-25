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

package com.netflix.graphql.dgs.mvc

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

object GraphQLMediaTypes {
    private val GRAPHQL_MEDIA_TYPE = MediaType("application", "graphql")

    internal const val GRAPHQL_MEDIA_TYPE_VALUE = "application/graphql"

    @SuppressWarnings("unused")
    internal val ACCEPTABLE_MEDIA_TYPES = listOf(GRAPHQL_MEDIA_TYPE, MediaType.APPLICATION_JSON)
    @SuppressWarnings("unused")
    internal val ACCEPTABLE_MEDIA_TYPE_VALUES = listOf(GRAPHQL_MEDIA_TYPE_VALUE, MediaType.APPLICATION_JSON_VALUE)

    @SuppressWarnings("unused")
    internal val ACCEPTABLE_MEDIA_TYPE_VALUES_f = arrayOf(GRAPHQL_MEDIA_TYPE_VALUE, MediaType.APPLICATION_JSON_VALUE)

    fun isApplicationGraphQL(headers: HttpHeaders): Boolean {
        return GRAPHQL_MEDIA_TYPE.includes(headers.contentType)
    }
}
