/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.isNotError
import androidx.room.ext.isNotNone
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SqlParser
import androidx.room.processor.EntityProcessor.Companion.createIndexName
import androidx.room.processor.EntityProcessor.Companion.extractForeignKeys
import androidx.room.processor.EntityProcessor.Companion.extractIndices
import androidx.room.processor.EntityProcessor.Companion.extractTableName
import androidx.room.processor.ProcessorErrors.INDEX_COLUMNS_CANNOT_BE_EMPTY
import androidx.room.processor.ProcessorErrors.INVALID_INDEX_ORDERS_SIZE
import androidx.room.processor.ProcessorErrors.RELATION_IN_ENTITY
import androidx.room.processor.cache.Cache
import androidx.room.vo.DataClass
import androidx.room.vo.EmbeddedProperty
import androidx.room.vo.Entity
import androidx.room.vo.ForeignKey
import androidx.room.vo.Index
import androidx.room.vo.PrimaryKey
import androidx.room.vo.Properties
import androidx.room.vo.Property
import androidx.room.vo.Warning
import androidx.room.vo.columnNames
import androidx.room.vo.findPropertyByColumnName

class TableEntityProcessor
internal constructor(
    baseContext: Context,
    val element: XTypeElement,
    private val referenceStack: LinkedHashSet<String> = LinkedHashSet()
) : EntityProcessor {
    val context = baseContext.fork(element)

    override fun process(): Entity {
        return context.cache.entities.get(Cache.EntityKey(element)) { doProcess() }
    }

    private fun doProcess(): Entity {
        if (!element.validate()) {
            context.reportMissingTypeReference(element.qualifiedName)
            return Entity(
                element = element,
                tableName = element.name,
                type = element.type,
                properties = emptyList(),
                embeddedProperties = emptyList(),
                indices = emptyList(),
                primaryKey = PrimaryKey.MISSING,
                foreignKeys = emptyList(),
                constructor = null,
                shadowTableName = null
            )
        }
        context.checker.hasAnnotation(
            element,
            androidx.room.Entity::class,
            ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY
        )
        val annotation = element.getAnnotation(androidx.room.Entity::class)
        val tableName: String
        val entityIndices: List<IndexInput>
        val foreignKeyInputs: List<ForeignKeyInput>
        val inheritSuperIndices: Boolean
        if (annotation != null) {
            tableName = extractTableName(element, annotation)
            entityIndices = extractIndices(annotation, tableName)
            inheritSuperIndices = annotation["inheritSuperIndices"]?.asBoolean() == true
            foreignKeyInputs = extractForeignKeys(annotation)
        } else {
            tableName = element.name
            foreignKeyInputs = emptyList()
            entityIndices = emptyList()
            inheritSuperIndices = false
        }
        context.checker.notBlank(
            tableName,
            element,
            ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_BE_EMPTY
        )
        context.checker.check(
            !tableName.startsWith("sqlite_", true),
            element,
            ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_START_WITH_SQLITE
        )

        val pojo =
            DataClassProcessor.createFor(
                    context = context,
                    element = element,
                    bindingScope = PropertyProcessor.BindingScope.TWO_WAY,
                    parent = null,
                    referenceStack = referenceStack
                )
                .process()
        context.checker.check(pojo.relations.isEmpty(), element, RELATION_IN_ENTITY)

        val propertyIndices =
            pojo.properties
                .filter { it.indexed }
                .mapNotNull {
                    if (it.parent != null) {
                        it.indexed = false
                        context.logger.w(
                            Warning.INDEX_FROM_EMBEDDED_PROPERTY_IS_DROPPED,
                            it.element,
                            ProcessorErrors.droppedEmbeddedPropertyIndex(
                                it.getPath(),
                                element.qualifiedName
                            )
                        )
                        null
                    } else if (it.element.enclosingElement != element && !inheritSuperIndices) {
                        it.indexed = false
                        context.logger.w(
                            Warning.INDEX_FROM_PARENT_PROPERTY_IS_DROPPED,
                            ProcessorErrors.droppedSuperClassPropertyIndex(
                                it.columnName,
                                element.qualifiedName,
                                it.element.enclosingElement
                                    .asClassName()
                                    .toString(context.codeLanguage)
                            )
                        )
                        null
                    } else {
                        IndexInput(
                            name = createIndexName(listOf(it.columnName), tableName),
                            unique = false,
                            columnNames = listOf(it.columnName),
                            orders = emptyList()
                        )
                    }
                }
        val superIndices = loadSuperIndices(element.superClass, tableName, inheritSuperIndices)
        val indexInputs = entityIndices + propertyIndices + superIndices
        val indices = validateAndCreateIndices(indexInputs, pojo)

        val primaryKey = findAndValidatePrimaryKey(pojo.properties, pojo.embeddedProperties)
        val affinity = primaryKey.properties.firstOrNull()?.affinity ?: SQLTypeAffinity.TEXT
        context.checker.check(
            !primaryKey.autoGenerateId || affinity == SQLTypeAffinity.INTEGER,
            primaryKey.properties.firstOrNull()?.element ?: element,
            ProcessorErrors.AUTO_INCREMENTED_PRIMARY_KEY_IS_NOT_INT
        )

        val entityForeignKeys = validateAndCreateForeignKeyReferences(foreignKeyInputs, pojo)
        checkIndicesForForeignKeys(entityForeignKeys, primaryKey, indices)

        context.checker.check(
            SqlParser.isValidIdentifier(tableName),
            element,
            ProcessorErrors.INVALID_TABLE_NAME
        )
        pojo.properties.forEach {
            context.checker.check(
                SqlParser.isValidIdentifier(it.columnName),
                it.element,
                ProcessorErrors.INVALID_COLUMN_NAME
            )
        }

        val entity =
            Entity(
                element = element,
                tableName = tableName,
                type = pojo.type,
                properties = pojo.properties,
                embeddedProperties = pojo.embeddedProperties,
                indices = indices,
                primaryKey = primaryKey,
                foreignKeys = entityForeignKeys,
                constructor = pojo.constructor,
                shadowTableName = null
            )

        return entity
    }

    private fun checkIndicesForForeignKeys(
        entityForeignKeys: List<ForeignKey>,
        primaryKey: PrimaryKey,
        indices: List<Index>
    ) {
        fun covers(columnNames: List<String>, properties: List<Property>): Boolean =
            properties.size >= columnNames.size &&
                columnNames.withIndex().all { properties[it.index].columnName == it.value }

        entityForeignKeys.forEach { fKey ->
            val columnNames = fKey.childProperties.map { it.columnName }
            val exists =
                covers(columnNames, primaryKey.properties) ||
                    indices.any { index -> covers(columnNames, index.properties) }
            if (!exists) {
                if (columnNames.size == 1) {
                    context.logger.w(
                        Warning.MISSING_INDEX_ON_FOREIGN_KEY_CHILD,
                        element,
                        ProcessorErrors.foreignKeyMissingIndexInChildColumn(columnNames[0])
                    )
                } else {
                    context.logger.w(
                        Warning.MISSING_INDEX_ON_FOREIGN_KEY_CHILD,
                        element,
                        ProcessorErrors.foreignKeyMissingIndexInChildColumns(columnNames)
                    )
                }
            }
        }
    }

    /** Does a validation on foreign keys except the parent table's columns. */
    private fun validateAndCreateForeignKeyReferences(
        foreignKeyInputs: List<ForeignKeyInput>,
        dataClass: DataClass
    ): List<ForeignKey> {
        return foreignKeyInputs
            .map {
                if (it.onUpdate == null) {
                    context.logger.e(element, ProcessorErrors.INVALID_FOREIGN_KEY_ACTION)
                    return@map null
                }
                if (it.onDelete == null) {
                    context.logger.e(element, ProcessorErrors.INVALID_FOREIGN_KEY_ACTION)
                    return@map null
                }
                if (it.childColumns.isEmpty()) {
                    context.logger.e(element, ProcessorErrors.FOREIGN_KEY_EMPTY_CHILD_COLUMN_LIST)
                    return@map null
                }
                if (it.parentColumns.isEmpty()) {
                    context.logger.e(element, ProcessorErrors.FOREIGN_KEY_EMPTY_PARENT_COLUMN_LIST)
                    return@map null
                }
                if (it.childColumns.size != it.parentColumns.size) {
                    context.logger.e(
                        element,
                        ProcessorErrors.foreignKeyColumnNumberMismatch(
                            it.childColumns,
                            it.parentColumns
                        )
                    )
                    return@map null
                }
                val parentElement = it.parent.typeElement
                if (parentElement == null) {
                    context.logger.e(element, ProcessorErrors.FOREIGN_KEY_CANNOT_FIND_PARENT)
                    return@map null
                }
                val parentAnnotation = parentElement.getAnnotation(androidx.room.Entity::class)
                if (parentAnnotation == null) {
                    context.logger.e(
                        element,
                        ProcessorErrors.foreignKeyNotAnEntity(parentElement.qualifiedName)
                    )
                    return@map null
                }
                val tableName = extractTableName(parentElement, parentAnnotation)
                val properties =
                    it.childColumns.mapNotNull { columnName ->
                        val property = dataClass.findPropertyByColumnName(columnName)
                        if (property == null) {
                            context.logger.e(
                                dataClass.element,
                                ProcessorErrors.foreignKeyChildColumnDoesNotExist(
                                    columnName,
                                    dataClass.columnNames
                                )
                            )
                        }
                        property
                    }
                if (properties.size != it.childColumns.size) {
                    return@map null
                }
                ForeignKey(
                    parentTable = tableName,
                    childProperties = properties,
                    parentColumns = it.parentColumns,
                    onDelete = it.onDelete,
                    onUpdate = it.onUpdate,
                    deferred = it.deferred
                )
            }
            .filterNotNull()
    }

    private fun findAndValidatePrimaryKey(
        properties: List<Property>,
        embeddedProperties: List<EmbeddedProperty>
    ): PrimaryKey {
        val candidates =
            collectPrimaryKeysFromEntityAnnotations(element, properties) +
                collectPrimaryKeysFromPrimaryKeyAnnotations(properties) +
                collectPrimaryKeysFromEmbeddedProperties(embeddedProperties)

        context.checker.check(candidates.isNotEmpty(), element, ProcessorErrors.MISSING_PRIMARY_KEY)

        // 1. If a key is not autogenerated, but is Primary key or is part of Primary key we
        // force the @NonNull annotation. If the key is a single Primary Key, Integer or Long, we
        // don't force the @NonNull annotation since SQLite will automatically generate IDs.
        // 2. If a key is autogenerate, we generate NOT NULL in table spec, but we don't require
        // @NonNull annotation on the property itself.
        val verifiedProperties =
            mutableSetOf<Property>() // track verified properties to not over report
        candidates
            .filterNot { it.autoGenerateId }
            .forEach { candidate ->
                candidate.properties.forEach { property ->
                    if (
                        candidate.properties.size > 1 ||
                            (candidate.properties.size == 1 &&
                                property.affinity != SQLTypeAffinity.INTEGER)
                    ) {
                        if (!verifiedProperties.contains(property)) {
                            context.checker.check(
                                property.nonNull,
                                property.element,
                                ProcessorErrors.primaryKeyNull(property.getPath())
                            )
                            verifiedProperties.add(property)
                        }
                        // Validate parents for nullability
                        var parent = property.parent
                        while (parent != null) {
                            val parentProperty = parent.property
                            if (!verifiedProperties.contains(parentProperty)) {
                                context.checker.check(
                                    parentProperty.nonNull,
                                    parentProperty.element,
                                    ProcessorErrors.primaryKeyNull(parentProperty.getPath())
                                )
                                verifiedProperties.add(parentProperty)
                            }
                            parent = parentProperty.parent
                        }
                    }
                }
            }

        if (candidates.size == 1) {
            // easy :)
            return candidates.first()
        }

        return choosePrimaryKey(candidates, element)
    }

    /** Check fields for @PrimaryKey. */
    private fun collectPrimaryKeysFromPrimaryKeyAnnotations(
        fields: List<Property>
    ): List<PrimaryKey> {
        return fields.mapNotNull { field ->
            val primaryKeyAnnotation =
                field.element.getAnnotation(androidx.room.PrimaryKey::class)
                    ?: return@mapNotNull null
            if (field.parent != null) {
                // the field in the entity that contains this error.
                val grandParentField = field.parent.mRootParent.property.element
                // bound for entity.
                context
                    .fork(grandParentField)
                    .logger
                    .w(
                        Warning.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED,
                        grandParentField,
                        ProcessorErrors.embeddedPrimaryKeyIsDropped(
                            element.qualifiedName,
                            field.name
                        )
                    )
                null
            } else {
                PrimaryKey(
                    declaredIn = field.element.enclosingElement,
                    properties = Properties(field),
                    autoGenerateId = primaryKeyAnnotation["autoGenerate"]?.asBoolean() == true
                )
            }
        }
    }

    /** Check classes for @Entity(primaryKeys = ?). */
    private fun collectPrimaryKeysFromEntityAnnotations(
        typeElement: XTypeElement,
        availableProperties: List<Property>
    ): List<PrimaryKey> {
        val myPkeys =
            typeElement.getAnnotation(androidx.room.Entity::class)?.let {
                val primaryKeyColumns = it["primaryKeys"]?.asStringList() ?: emptyList()
                if (primaryKeyColumns.isEmpty()) {
                    emptyList()
                } else {
                    val properties =
                        primaryKeyColumns.mapNotNull { pKeyColumnName ->
                            val property =
                                availableProperties.firstOrNull { it.columnName == pKeyColumnName }
                            context.checker.check(
                                property != null,
                                typeElement,
                                ProcessorErrors.primaryKeyColumnDoesNotExist(
                                    pKeyColumnName,
                                    availableProperties.map { it.columnName }
                                )
                            )
                            property
                        }
                    listOf(
                        PrimaryKey(
                            declaredIn = typeElement,
                            properties = Properties(properties),
                            autoGenerateId = false
                        )
                    )
                }
            } ?: emptyList()
        // checks supers.
        val mySuper = typeElement.superClass
        val superPKeys =
            if (mySuper != null && mySuper.isNotNone() && mySuper.isNotError()) {
                // my super cannot see my properties so remove them.
                val remainingProperties =
                    availableProperties.filterNot { it.element.enclosingElement == typeElement }
                collectPrimaryKeysFromEntityAnnotations(mySuper.typeElement!!, remainingProperties)
            } else {
                emptyList()
            }
        return superPKeys + myPkeys
    }

    private fun collectPrimaryKeysFromEmbeddedProperties(
        embeddedProperties: List<EmbeddedProperty>
    ): List<PrimaryKey> {
        return embeddedProperties.mapNotNull { embeddedProperty ->
            embeddedProperty.property.element.getAnnotation(androidx.room.PrimaryKey::class)?.let {
                val autoGenerate = it["autoGenerate"]?.asBoolean() == true
                context.checker.check(
                    !autoGenerate || embeddedProperty.dataClass.properties.size == 1,
                    embeddedProperty.property.element,
                    ProcessorErrors.AUTO_INCREMENT_EMBEDDED_HAS_MULTIPLE_PROPERTIES
                )
                PrimaryKey(
                    declaredIn = embeddedProperty.property.element.enclosingElement,
                    properties = embeddedProperty.dataClass.properties,
                    autoGenerateId = autoGenerate
                )
            }
        }
    }

    // start from my element and check if anywhere in the list we can find the only well defined
    // pkey, if so, use it.
    private fun choosePrimaryKey(
        candidates: List<PrimaryKey>,
        typeElement: XTypeElement
    ): PrimaryKey {
        // If 1 of these primary keys is declared in this class, then it is the winner. Just print
        //    a note for the others.
        // If 0 is declared, check the parent.
        // If more than 1 primary key is declared in this class, it is an error.
        val myPKeys = candidates.filter { candidate -> candidate.declaredIn == typeElement }
        return if (myPKeys.size == 1) {
            // just note, this is not worth an error or warning
            (candidates - myPKeys).forEach {
                context.logger.d(
                    element,
                    "${it.toHumanReadableString()} is" +
                        " overridden by ${myPKeys.first().toHumanReadableString()}"
                )
            }
            myPKeys.first()
        } else if (myPKeys.isEmpty()) {
            // i have not declared anything, delegate to super
            val mySuper = typeElement.superClass
            if (mySuper != null && mySuper.isNotNone() && mySuper.isNotError()) {
                return choosePrimaryKey(candidates, mySuper.typeElement!!)
            }
            PrimaryKey.MISSING
        } else {
            context.logger.e(
                element,
                ProcessorErrors.multiplePrimaryKeyAnnotations(
                    myPKeys.map(PrimaryKey::toHumanReadableString)
                )
            )
            PrimaryKey.MISSING
        }
    }

    private fun validateAndCreateIndices(
        inputs: List<IndexInput>,
        dataClass: DataClass
    ): List<Index> {
        // check for columns
        val indices =
            inputs.mapNotNull { input ->
                context.checker.check(
                    input.columnNames.isNotEmpty(),
                    element,
                    INDEX_COLUMNS_CANNOT_BE_EMPTY
                )
                val properties =
                    input.columnNames.mapNotNull { columnName ->
                        val property = dataClass.findPropertyByColumnName(columnName)
                        context.checker.check(
                            property != null,
                            element,
                            ProcessorErrors.indexColumnDoesNotExist(
                                columnName,
                                dataClass.columnNames
                            )
                        )
                        property
                    }
                if (input.orders.isNotEmpty()) {
                    context.checker.check(
                        input.columnNames.size == input.orders.size,
                        element,
                        INVALID_INDEX_ORDERS_SIZE
                    )
                }
                if (properties.isEmpty()) {
                    null
                } else {
                    Index(
                        name = input.name,
                        unique = input.unique,
                        properties = Properties(properties),
                        orders = input.orders
                    )
                }
            }

        // check for duplicate indices
        indices
            .groupBy { it.name }
            .filter { it.value.size > 1 }
            .forEach { context.logger.e(element, ProcessorErrors.duplicateIndexInEntity(it.key)) }

        // see if any embedded property is an entity with indices, if so, report a warning
        dataClass.embeddedProperties.forEach { embedded ->
            val embeddedElement = embedded.dataClass.element
            embeddedElement.getAnnotation(androidx.room.Entity::class)?.let {
                val subIndices = extractIndices(it, "")
                if (subIndices.isNotEmpty()) {
                    context.logger.w(
                        Warning.INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED,
                        embedded.property.element,
                        ProcessorErrors.droppedEmbeddedIndex(
                            entityName = embedded.dataClass.typeName.toString(context.codeLanguage),
                            propertyPath = embedded.property.getPath(),
                            grandParent = element.qualifiedName
                        )
                    )
                }
            }
        }
        return indices
    }

    // check if parent is an Entity, if so, report its annotation indices
    private fun loadSuperIndices(
        typeMirror: XType?,
        tableName: String,
        inherit: Boolean
    ): List<IndexInput> {
        if (typeMirror == null || typeMirror.isNone() || typeMirror.isError()) {
            return emptyList()
        }
        val parentTypeElement = typeMirror.typeElement
        @Suppress("FoldInitializerAndIfToElvis")
        if (parentTypeElement == null) {
            // this is coming from a parent, shouldn't happen so no reason to report an error
            return emptyList()
        }
        val myIndices =
            parentTypeElement.getAnnotation(androidx.room.Entity::class)?.let { annotation ->
                val indices = extractIndices(annotation, tableName = "super")
                if (indices.isEmpty()) {
                    emptyList()
                } else if (inherit) {
                    // rename them
                    indices.map {
                        IndexInput(
                            name = createIndexName(it.columnNames, tableName),
                            unique = it.unique,
                            columnNames = it.columnNames,
                            orders = it.orders
                        )
                    }
                } else {
                    context.logger.w(
                        Warning.INDEX_FROM_PARENT_IS_DROPPED,
                        parentTypeElement,
                        ProcessorErrors.droppedSuperClassIndex(
                            childEntity = element.qualifiedName,
                            superEntity = parentTypeElement.qualifiedName
                        )
                    )
                    emptyList()
                }
            } ?: emptyList()
        return myIndices + loadSuperIndices(parentTypeElement.superClass, tableName, inherit)
    }
}
