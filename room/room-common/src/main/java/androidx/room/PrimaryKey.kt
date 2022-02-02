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

package androidx.room

/**
 * Marks a field in an [Entity] as the primary key.
 *
 * If you would like to define a composite primary key, you should use [Entity.primaryKeys]
 * method.
 *
 * Each [Entity] must declare a primary key unless one of its super classes declares a
 * primary key. If both an [Entity] and its super class defines a [PrimaryKey], the
 * child's [PrimaryKey] definition will override the parent's [PrimaryKey].
 *
 * If `PrimaryKey` annotation is used on a [Embedded] field, all columns inherited
 * from that embedded field becomes the composite primary key (including its grand children
 * fields).
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class PrimaryKey(
    /**
     * Set to true to let SQLite generate the unique id.
     *
     * When set to `true`, the SQLite type affinity for the field should be `INTEGER`.
     *
     * If the field type is `Long` or `Int` (or its TypeConverter converts it to a
     * `Long` or `Int`), [Insert] methods treat `0` as not-set while
     * inserting the item.
     *
     * If the field's type is [Integer] or [Long] (or its TypeConverter converts it to
     * an [Integer] or [Long]), [Insert] methods treat `null` as
     * not-set while inserting the item.
     *
     * @return Whether the primary key should be auto-generated by SQLite or not. Defaults
     * to false.
     */
    val autoGenerate: Boolean = false
)
