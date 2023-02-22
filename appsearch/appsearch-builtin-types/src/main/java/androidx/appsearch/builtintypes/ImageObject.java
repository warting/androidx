/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import static androidx.core.util.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.builtintypes.properties.Keywords;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an image file.
 *
 * <p>See <a href="http://schema.org/ImageObject">http://schema.org/ImageObject</a> for more
 * context.
 */
@Document(name = "builtin:ImageObject")
public final class ImageObject extends Thing {

    @NonNull
    @Document.DocumentProperty(name = "keywords", indexNestedProperties = true)
    private final List<Keywords> mKeywordsList;

    @Nullable
    @Document.StringProperty
    private final String mSha256;

    @Nullable
    @Document.StringProperty
    private final String mThumbnailSha256;

    ImageObject(@NonNull String namespace, @NonNull String id, int documentScore,
            long creationTimestampMillis, long documentTtlMillis, @Nullable String name,
            @Nullable List<String> alternateNames, @Nullable String description,
            @Nullable String image, @Nullable String url, @NonNull List<Keywords> keywordsList,
            @Nullable String sha256, @Nullable String thumbnailSha256) {
        super(namespace, id, documentScore, creationTimestampMillis, documentTtlMillis, name,
                alternateNames, description, image, url);
        mKeywordsList = checkNotNull(keywordsList);
        mSha256 = sha256;
        mThumbnailSha256 = thumbnailSha256;
    }

    /**
     * Keywords or tags used to describe some item.
     *
     * <p>See <a href="http://schema.org/keywords">http://schema.org/keywords</a> for more context.
     */
    @NonNull
    public List<Keywords> getKeywordsList() {
        return mKeywordsList;
    }

    /**
     * The SHA-2 SHA256 hash of the content of the item.
     * For example, a zero-length input has value
     * 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'.
     *
     * <p>See <a href="http://schema.org/sha256">http://schema.org/sha256</a> for more context.
     */
    @Nullable
    public String getSha256() {
        return mSha256;
    }

    /**
     * Returns the {@code sha256} for the thumbnail of this image or video.
     */
    @Nullable
    public String getThumbnailSha256() {
        return mThumbnailSha256;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageObject that = (ImageObject) o;
        return mKeywordsList.equals(that.mKeywordsList) && Objects.equals(mSha256, that.mSha256)
                && Objects.equals(mThumbnailSha256, that.mThumbnailSha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mKeywordsList, mSha256, mThumbnailSha256);
    }

    /**
     * Builder for {@link ImageObject}.
     */
    public static final class Builder extends BuilderImpl<Builder> {
        /**
         * Constructor for an empty {@link Builder}.
         *
         * @param namespace Namespace for the Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Copy constructor.
         */
        public Builder(@NonNull ImageObject copyFrom) {
            super(copyFrom);
        }
    }

    @SuppressWarnings("unchecked")
    static class BuilderImpl<Self extends BuilderImpl<Self>> extends Thing.BuilderImpl<Self> {
        @NonNull
        protected final List<Keywords> mKeywordsList;

        @Nullable
        protected String mSha256;

        @Nullable
        protected String mThumbnailSha256;

        BuilderImpl(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
            mKeywordsList = new ArrayList<>();
            mSha256 = null;
            mThumbnailSha256 = null;
        }

        BuilderImpl(@NonNull ImageObject copyFrom) {
            super(new Thing.Builder(checkNotNull(copyFrom)).build());
            mKeywordsList = new ArrayList<>(copyFrom.getKeywordsList());
            mSha256 = copyFrom.getSha256();
            mThumbnailSha256 = copyFrom.getThumbnailSha256();
        }

        @NonNull
        @Override
        public ImageObject build() {
            return new ImageObject(mNamespace, mId, mDocumentScore, mCreationTimestampMillis,
                    mDocumentTtlMillis, mName, mAlternateNames, mDescription, mImage, mUrl,
                    new ArrayList<>(mKeywordsList), mSha256, mThumbnailSha256);
        }

        /**
         * Adds a {@code keywords} as a Text i.e. {@link String}.
         */
        // Atypical overloads in the Builder to model union types.
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Self addKeywords(@NonNull String text) {
            mKeywordsList.add(new Keywords(checkNotNull(text)));
            return (Self) this;
        }

        /**
         * Adds the {@link Keywords} to the {@code keywordsList}.
         */
        @NonNull
        // TODO(b/268353464): Remove suppression once FR is addressed
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Self addKeywords(@NonNull Keywords keywords) {
            mKeywordsList.add(checkNotNull(keywords));
            return (Self) this;
        }

        /**
         * Adds all the {@link Keywords} values to the {@code keywordsList}.
         */
        @NonNull
        // TODO(b/268353464): Remove suppression once FR is addressed
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Self addAllKeywords(@NonNull Iterable<Keywords> values) {
            for (Keywords value : checkNotNull(values)) {
                mKeywordsList.add(checkNotNull(value));
            }
            return (Self) this;
        }

        /**
         * Clears the {@code keywordsList}.
         */
        @NonNull
        public Self clearKeywords() {
            mKeywordsList.clear();
            return (Self) this;
        }

        /**
         * Sets the {@code sha256}.
         */
        @NonNull
        public Self setSha256(@Nullable String text) {
            mSha256 = text;
            return (Self) this;
        }

        /**
         * Clears the {@code sha256}.
         */
        @NonNull
        public Self clearSha256() {
            mSha256 = null;
            return (Self) this;
        }

        /**
         * Sets the {@code sha256} of the thumbnail of this image of video.
         */
        @NonNull
        public Self setThumbnailSha256(@Nullable String text) {
            mThumbnailSha256 = text;
            return (Self) this;
        }

        /**
         * Clears the {@code sha256} of the thumbnail of this image of video.
         */
        @NonNull
        public Self clearThumbnailSha256() {
            mThumbnailSha256 = null;
            return (Self) this;
        }
    }
}
