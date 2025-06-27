/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.player.view;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SystemClock;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.serialize.MapSerializer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;

/** Public API to create a new RemoteComposeDocument coming from an input stream */
public class RemoteComposeDocument {

    CoreDocument mDocument;

    public RemoteComposeDocument(byte[] inputStream) {
        this(new ByteArrayInputStream(inputStream), new SystemClock());
    }

    public RemoteComposeDocument(InputStream inputStream) {
        this(inputStream, new SystemClock());
    }

    public RemoteComposeDocument(InputStream inputStream, Clock clock) {
        mDocument = new CoreDocument(clock);
        RemoteComposeBuffer buffer = RemoteComposeBuffer.fromInputStream(inputStream);
        mDocument.initFromBuffer(buffer);
    }

    public RemoteComposeDocument(CoreDocument document) {
        mDocument = document;
    }

    public CoreDocument getDocument() {
        return mDocument;
    }

    public void setDocument(CoreDocument document) {
        this.mDocument = document;
    }

    /**
     * Called when an initialization is needed, allowing the document to eg load resources / cache
     * them.
     */
    public void initializeContext(RemoteContext context) {
        mDocument.initializeContext(context);
    }

    /** Returns the width of the document in pixels */
    public int getWidth() {
        return mDocument.getWidth();
    }

    /** Returns the height of the document in pixels */
    public int getHeight() {
        return mDocument.getHeight();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Painting
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Paint the document
     *
     * @param context the provided PaintContext
     * @param theme the theme we want to use for this document.
     */
    public void paint(RemoteContext context, int theme) {
        mDocument.paint(context, theme);
    }

    /**
     * The delay in milliseconds to next repaint -1 = not needed 0 = asap
     *
     * @return delay in milliseconds to next repaint or -1
     */
    public int needsRepaint() {
        return mDocument.needsRepaint();
    }

    /**
     * Returns true if the document can be displayed given this version of the player
     *
     * @param majorVersion the max major version supported by the player
     * @param minorVersion the max minor version supported by the player
     * @param capabilities a bitmask of capabilities the player supports (unused for now)
     */
    public boolean canBeDisplayed(int majorVersion, int minorVersion, long capabilities) {
        return mDocument.canBeDisplayed(majorVersion, minorVersion, capabilities);
    }

    @Override
    public String toString() {
        return "Document{\n" + mDocument + '}';
    }

    /**
     * Gets a array of Names of the named colors defined in the loaded doc.
     *
     * @return
     */
    public String[] getNamedColors() {
        return mDocument.getNamedColors();
    }

    /**
     * Gets a array of Names of the named variables of a specific type defined in the doc.
     *
     * @param type the type of variable NamedVariable.COLOR_TYPE, STRING_TYPE, etc
     * @return array of name or null
     */
    public String[] getNamedVariables(int type) {
        return mDocument.getNamedVariables(type);
    }

    /**
     * Return a component associated with id
     *
     * @param id the component id
     * @return the corresponding component or null if not found
     */
    public Component getComponent(int id) {
        return mDocument.getComponent(id);
    }

    /** Invalidate the document for layout measures. This will trigger a layout remeasure pass. */
    public void invalidate() {
        mDocument.invalidateMeasure();
    }

    /**
     * @hide Returns a list of useful statistics for the runtime document
     * @return array of strings representing some useful statistics
     */
    public String[] getStats() {
        if (mDocument == null) {
            return new String[0];
        }
        return mDocument.getStats();
    }

    /**
     * @hide Returns the number of sensor listeners
     * @param ids
     * @return
     */
    public int hasSensorListeners(int[] ids) {
        return 0;
    }

    /**
     * @hide Returns the current clock
     * @return
     */
    public Clock getClock() {
        return getDocument().getClock();
    }

    /**
     * @hide Returns true if the current document is an update-only document
     * @return
     */
    public boolean isUpdateDoc() {
        return mDocument.isUpdateDoc();
    }

    /**
     * @hide Serialize the document
     * @param serializer
     */
    public void serialize(MapSerializer serializer) {
        mDocument.serialize(serializer);
    }
}
