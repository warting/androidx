/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.processor

import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XTypeElement
import androidx.room.vo.EntityOrView
import androidx.room.vo.Properties

interface EntityOrViewProcessor {
    fun process(): EntityOrView
}

/**
 * A no-op implementation of [EntityOrViewProcessor] that just prints a processor error for use of
 * an invalid class as [EntityOrView].
 */
private class NonEntityOrViewProcessor(
    val context: Context,
    val element: XTypeElement,
    private val referenceStack: LinkedHashSet<String>,
) : EntityOrViewProcessor {

    override fun process(): EntityOrView {
        context.logger.e(element, ProcessorErrors.NOT_ENTITY_OR_VIEW)
        // Parse this as a Pojo in case there are more errors.
        DataClassProcessor.createFor(
                context = context,
                element = element,
                bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                parent = null,
                referenceStack = referenceStack,
            )
            .process()
        return object : EntityOrView {
            override val properties: Properties = Properties()
            override val tableName: String
                get() = typeName.toString()

            override val typeName: XTypeName
                get() = element.type.asTypeName()
        }
    }
}

@Suppress("FunctionName")
fun EntityOrViewProcessor(
    context: Context,
    element: XTypeElement,
    referenceStack: LinkedHashSet<String> = LinkedHashSet(),
): EntityOrViewProcessor {
    return when {
        element.hasAnnotation(Entity::class) -> EntityProcessor(context, element, referenceStack)
        element.hasAnnotation(DatabaseView::class) -> DatabaseViewProcessor(context, element)
        else -> NonEntityOrViewProcessor(context, element, referenceStack)
    }
}
