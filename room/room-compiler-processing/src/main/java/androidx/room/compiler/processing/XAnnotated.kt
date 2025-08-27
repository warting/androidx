/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.room.compiler.codegen.XClassName
import com.squareup.javapoet.ClassName
import kotlin.reflect.KClass

/** Common interface implemented by elements that might have annotations. */
interface XAnnotated {
    /**
     * Returns the list of [XAnnotation] elements that have the same qualified name as the given
     * [annotationName]. Otherwise, returns an empty list.
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type, not
     * the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotations(annotationName: ClassName) = getAnnotations(annotationName.canonicalName())

    /**
     * Returns the list of [XAnnotation] elements that have the same qualified name as the given
     * [annotationName]. Otherwise, returns an empty list.
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type, not
     * the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotations(annotationName: XClassName) = getAnnotations(annotationName.canonicalName)

    /**
     * Returns the list of [XAnnotation] elements that have the same qualified name as the given
     * [annotationName]. Otherwise, returns an empty list.
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type, not
     * the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotations(annotationName: String): List<XAnnotation> =
        getAllAnnotations().filter { it.qualifiedName == annotationName }

    /**
     * Gets the list of annotations with the given type.
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type, not
     * the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> getAnnotations(annotation: KClass<T>): List<XAnnotation>

    /**
     * Returns all annotations on this element represented as [XAnnotation].
     *
     * As opposed to other functions like [getAnnotations] this does not require you to have a
     * reference to each annotation class, and thus it can represent annotations in the module
     * sources being compiled. For [XAnnotation] all values must be accessed dynamically.
     */
    fun getAllAnnotations(): List<XAnnotation>

    /**
     * Returns `true` if this element is annotated with the given [annotation].
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type, not
     * the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnyAnnotation]
     */
    fun hasAnnotation(annotation: KClass<out Annotation>): Boolean

    /**
     * Returns `true` if this element is annotated with an [XAnnotation] that has the same qualified
     * name as the given [annotationName].
     *
     * @see [hasAnyAnnotation]
     */
    fun hasAnnotation(annotationName: ClassName) = hasAnnotation(annotationName.canonicalName())

    /**
     * Returns `true` if this element is annotated with an [XAnnotation] that has the same qualified
     * name as the given [annotationName].
     *
     * @see [hasAnyAnnotation]
     */
    fun hasAnnotation(annotationName: XClassName) = hasAnnotation(annotationName.canonicalName)

    /**
     * Returns `true` if this element is annotated with an [XAnnotation] that has the same qualified
     * name as the given [annotationName].
     *
     * @see [hasAnyAnnotation]
     */
    fun hasAnnotation(annotationName: String) = getAnnotation(annotationName) != null

    /**
     * Returns `true` if this element has an annotation that is declared in the given package.
     * Alternatively, all annotations can be accessed with [getAllAnnotations].
     */
    fun hasAnnotationWithPackage(pkg: String): Boolean

    /** Returns `true` if this element has one of the [annotations]. */
    fun hasAnyAnnotation(vararg annotations: ClassName) = annotations.any(this::hasAnnotation)

    /** Returns `true` if this element has one of the [annotations]. */
    fun hasAnyAnnotation(vararg annotations: KClass<out Annotation>) =
        annotations.any(this::hasAnnotation)

    /** Returns `true` if this element has one of the [annotations]. */
    fun hasAnyAnnotation(annotations: Collection<ClassName>) = annotations.any(this::hasAnnotation)

    /** Returns `true` if this element has one of the [annotations]. */
    fun hasAnyAnnotation(vararg annotations: XClassName) = annotations.any(this::hasAnnotation)

    /** Returns `true` if this element has all the [annotations]. */
    fun hasAllAnnotations(vararg annotations: ClassName): Boolean =
        annotations.all(this::hasAnnotation)

    /** Returns `true` if this element has all the [annotations]. */
    fun hasAllAnnotations(vararg annotations: KClass<out Annotation>): Boolean =
        annotations.all(this::hasAnnotation)

    /** Returns `true` if this element has all the [annotations]. */
    fun hasAllAnnotations(annotations: Collection<ClassName>): Boolean =
        annotations.all(this::hasAnnotation)

    /** Returns `true` if this element has all the [annotations]. */
    fun hasAllAnnotations(vararg annotations: XClassName): Boolean =
        annotations.all(this::hasAnnotation)

    /**
     * If the current element has an annotation with the given [annotation] class.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> getAnnotation(annotation: KClass<T>): XAnnotation? {
        return getAnnotations(annotation).firstOrNull()
    }

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotationName]. Otherwise,
     * `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotation(annotationName: ClassName) = getAnnotation(annotationName.canonicalName())

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotationName]. Otherwise,
     * `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotation(annotationName: XClassName) = getAnnotation(annotationName.canonicalName)

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotationName]. Otherwise,
     * `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotation(annotationName: String): XAnnotation? =
        getAnnotations(annotationName).firstOrNull()

    /** Returns the [Annotation]s that are annotated with [annotationName] */
    fun getAnnotationsAnnotatedWith(annotationName: ClassName): Set<XAnnotation> {
        return getAllAnnotations()
            .filter { it.type.typeElement?.hasAnnotation(annotationName) == true }
            .toSet()
    }

    /** Returns the [Annotation]s that are annotated with [annotationName] */
    fun getAnnotationsAnnotatedWith(annotationName: XClassName): Set<XAnnotation> {
        return getAllAnnotations()
            .filter { it.type.typeElement?.hasAnnotation(annotationName) == true }
            .toSet()
    }

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotationName].
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun requireAnnotation(annotationName: ClassName): XAnnotation {
        return getAnnotation(annotationName)!!
    }

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotationName].
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun requireAnnotation(annotationName: XClassName): XAnnotation {
        return getAnnotation(annotationName)!!
    }

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotation].
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> requireAnnotation(annotation: KClass<T>) =
        checkNotNull(getAnnotation(annotation)) { "Cannot find required annotation $annotation" }
}

/** Returns `true` if this element has one of the [annotations]. */
fun XAnnotated.hasAnyAnnotation(annotations: Collection<XClassName>) =
    annotations.any(this::hasAnnotation)

/** Returns `true` if this element has all the [annotations]. */
fun XAnnotated.hasAllAnnotations(annotations: Collection<XClassName>): Boolean =
    annotations.all(this::hasAnnotation)
