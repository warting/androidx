/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.XTypeElement
import androidx.room.log.RLog
import androidx.room.processor.Context
import androidx.room.processor.Context.BooleanProcessorOptions.GENERATE_KOTLIN
import androidx.room.processor.DatabaseProcessor
import androidx.room.processor.ProcessorErrors
import androidx.room.vo.DaoFunction
import androidx.room.vo.Warning
import androidx.room.writer.AutoMigrationWriter
import androidx.room.writer.DaoWriter
import androidx.room.writer.DatabaseObjectConstructorWriter
import androidx.room.writer.DatabaseWriter
import androidx.room.writer.TypeWriter
import java.nio.file.Path

class DatabaseProcessingStep : XProcessingStep {

    override fun annotations(): Set<String> {
        return mutableSetOf(Database::class.qualifiedName!!)
    }

    override fun process(
        env: XProcessingEnv,
        elementsByAnnotation: Map<String, Set<XElement>>,
        isLastRound: Boolean
    ): Set<XTypeElement> {
        check(env.config == getEnvConfig(env.options)) {
            "Room Processor expected ${getEnvConfig(env.options)} " +
                "but was invoked with a different " +
                "configuration: ${env.config}"
        }
        val context = Context(env)
        validateLanguageAndTarget(context)

        val rejectedElements = mutableSetOf<XTypeElement>()
        val databases =
            elementsByAnnotation[Database::class.qualifiedName]
                ?.filterIsInstance<XTypeElement>()
                ?.mapNotNull { annotatedElement ->
                    if (isLastRound && !annotatedElement.validate()) {
                        context.reportMissingTypeReference(annotatedElement.qualifiedName)
                        return@mapNotNull null
                    }
                    val (database, logs) =
                        context.collectLogs { subContext ->
                            DatabaseProcessor(subContext, annotatedElement).process()
                        }
                    if (logs.hasMissingTypeErrors()) {
                        if (isLastRound) {
                            // Processing is done yet there are still missing type errors, only
                            // report
                            // those and don't generate code for the database class since
                            // compilation
                            // will fail anyway.
                            logs.writeTo(context, RLog.MissingTypeErrorFilter)
                            return@mapNotNull null
                        } else {
                            // Abandon processing this database class since it needed a type element
                            // that is missing. It is possible that the type will be generated by a
                            // further annotation processing round, so we will try again by adding
                            // this class element to a deferred set.
                            rejectedElements.add(annotatedElement)
                            return@mapNotNull null
                        }
                    } else {
                        logs.writeTo(context)
                        return@mapNotNull database
                    }
                }

        val daoFunctionsMap = databases?.flatMap { db -> db.daoFunctions.map { it to db } }?.toMap()
        daoFunctionsMap?.let {
            prepareDaosForWriting(databases, it.keys.toList())
            it.forEach { (daoFunction, db) ->
                DaoWriter(
                        dao = daoFunction.dao,
                        dbElement = db.element,
                        writerContext = TypeWriter.WriterContext.fromProcessingContext(context)
                    )
                    .write(context.processingEnv)
            }
        }

        databases?.forEach { db ->
            DatabaseWriter(
                    database = db,
                    writerContext = TypeWriter.WriterContext.fromProcessingContext(context)
                )
                .write(context.processingEnv)
            if (db.exportSchema) {
                val qName = db.element.qualifiedName
                val filename = "${db.version}.json"
                val exportToResources =
                    Context.BooleanProcessorOptions.EXPORT_SCHEMA_RESOURCE.getValue(env)
                val schemaInFolderPath = context.schemaInFolderPath
                val schemaOutFolderPath = context.schemaOutFolderPath
                if (exportToResources) {
                    context.logger.w(ProcessorErrors.EXPORTING_SCHEMA_TO_RESOURCES)
                    val schemaFileOutputStream =
                        env.filer.writeResource(
                            filePath = Path.of("schemas", qName, filename),
                            originatingElements = listOf(db.element)
                        )
                    db.exportSchemaOnly(schemaFileOutputStream)
                } else if (schemaInFolderPath != null && schemaOutFolderPath != null) {
                    db.exportSchema(
                        inputPath = Path.of(schemaInFolderPath, qName, filename),
                        outputPath = Path.of(schemaOutFolderPath, qName, filename)
                    )
                } else {
                    context.logger.w(
                        warning = Warning.MISSING_SCHEMA_LOCATION,
                        element = db.element,
                        msg = ProcessorErrors.MISSING_SCHEMA_EXPORT_DIRECTORY
                    )
                }
            }
            db.autoMigrations.forEach { autoMigration ->
                AutoMigrationWriter(
                        autoMigration = autoMigration,
                        dbElement = db.element,
                        writerContext = TypeWriter.WriterContext.fromProcessingContext(context)
                    )
                    .write(context.processingEnv)
            }
            if (db.constructorObject != null) {
                DatabaseObjectConstructorWriter(
                        database = db,
                        constructorObject = db.constructorObject
                    )
                    .write(context.processingEnv)
            }
        }

        return rejectedElements
    }

    private fun validateLanguageAndTarget(context: Context) {
        val onlyAndroidInTargets = context.isAndroidOnlyTarget()
        if (context.codeLanguage == CodeLanguage.JAVA && !onlyAndroidInTargets) {
            // The list of target platforms should only contain Android if we're generating Java.
            context.logger.e(ProcessorErrors.JAVA_CODEGEN_ON_NON_ANDROID_TARGET)
        }
    }

    /**
     * Traverses all dao functions and assigns them suffix if they are used in multiple databases.
     */
    private fun prepareDaosForWriting(
        databases: List<androidx.room.vo.Database>,
        daoFunctions: List<DaoFunction>
    ) {
        daoFunctions
            .groupBy { it.dao.typeName }
            // if used only in 1 database, nothing to do.
            .filter { entry -> entry.value.size > 1 }
            .forEach { entry ->
                entry.value
                    .groupBy { daoFunction ->
                        // first suffix guess: Database's simple name
                        val db = databases.first { db -> db.daoFunctions.contains(daoFunction) }
                        db.typeName.simpleNames.last()
                    }
                    .forEach { (dbName, functions) ->
                        if (functions.size == 1) {
                            // good, db names do not clash, use db name as suffix
                            functions.first().dao.setSuffix(dbName)
                        } else {
                            // ok looks like a dao is used in 2 different databases both of
                            // which have the same name. enumerate.
                            functions.forEachIndexed { index, function ->
                                function.dao.setSuffix("${dbName}_$index")
                            }
                        }
                    }
            }
    }

    companion object {
        internal fun getEnvConfig(options: Map<String, String>) =
            XProcessingEnvConfig.DEFAULT.copy(
                excludeMethodsWithInvalidJvmSourceNames = !GENERATE_KOTLIN.getValue(options)
            )
    }
}
