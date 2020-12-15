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

package androidx.car.app;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.Template;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link AppManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class AppManagerTest {
    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private IAppHost.Stub mMockAppHost;
    @Mock
    private IOnDoneCallback mMockOnDoneCallback;

    private TestCarContext mTestCarContext;
    private final HostDispatcher mHostDispatcher = new HostDispatcher();

    private AppManager mAppManager;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

        mTestCarContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

        IAppHost appHost =
                new IAppHost.Stub() {
                    @Override
                    public void invalidate() throws RemoteException {
                        mMockAppHost.invalidate();
                    }

                    @Override
                    public void showToast(CharSequence text, int duration) throws
                            RemoteException {
                        mMockAppHost.showToast(text, duration);
                    }

                    @Override
                    public void setSurfaceCallback(@Nullable ISurfaceCallback surfaceCallback)
                            throws RemoteException {
                        mMockAppHost.setSurfaceCallback(surfaceCallback);
                    }
                };
        when(mMockCarHost.getHost(any())).thenReturn(appHost.asBinder());

        mHostDispatcher.setCarHost(mMockCarHost);

        mAppManager = AppManager.create(mTestCarContext, mHostDispatcher);
    }

    @Test
    public void getTemplate_serializationFails_throwsIllegalStateException()
            throws RemoteException {
        mTestCarContext
                .getCarService(ScreenManager.class)
                .push(new Screen(mTestCarContext) {
                    @NonNull
                    @Override
                    public Template onGetTemplate() {
                        return new Template() {
                        };
                    }
                });

        assertThrows(
                HostException.class,
                () -> mAppManager.getIInterface().getTemplate(mMockOnDoneCallback));
        verify(mMockOnDoneCallback).onFailure(any());
    }

    @Test
    public void invalidate_forwardsRequestToHost() throws RemoteException {
        mAppManager.invalidate();

        verify(mMockAppHost).invalidate();
    }

    @Test
    public void invalidate_hostThrowsRemoteException_throwsHostException() throws
            RemoteException {
        doThrow(new RemoteException()).when(mMockAppHost).invalidate();

        assertThrows(HostException.class, () -> mAppManager.invalidate());
    }

    @Test
    public void invalidate_hostThrowsRuntimeException_throwsHostException() throws
            RemoteException {
        doThrow(new IllegalStateException()).when(mMockAppHost).invalidate();

        assertThrows(HostException.class, () -> mAppManager.invalidate());
    }

    @Test
    public void showToast_forwardsRequestToHost() throws RemoteException {
        String text = "Toast";
        int duration = 10;
        mAppManager.showToast(text, duration);

        verify(mMockAppHost).showToast(text, duration);
    }

    @Test
    public void showToast_hostThrowsRemoteException_throwsHostException() throws RemoteException {
        doThrow(new RemoteException()).when(mMockAppHost).showToast(anyString(), anyInt());

        assertThrows(HostException.class, () -> mAppManager.showToast("foo", 1));
    }

    @Test
    public void showToast_hostThrowsRuntimeException_throwsHostException() throws
            RemoteException {
        doThrow(new IllegalStateException()).when(mMockAppHost).showToast(anyString(), anyInt());

        assertThrows(HostException.class, () -> mAppManager.showToast("foo", 1));
    }

    @Test
    public void setSurfaceListener_forwardsRequestToHost() throws RemoteException {
        mAppManager.setSurfaceCallback(null);

        verify(mMockAppHost).setSurfaceCallback(null);
    }

    @Test
    public void setSurfaceListener_hostThrowsSecurityException_throwsSecurityException()
            throws RemoteException {
        doThrow(new SecurityException()).when(mMockAppHost).setSurfaceCallback(any());

        assertThrows(SecurityException.class, () -> mAppManager.setSurfaceCallback(null));
    }

    @Test
    public void etSurfaceListener_hostThrowsRemoteException_throwsHostException()
            throws RemoteException {
        doThrow(new RemoteException()).when(mMockAppHost).setSurfaceCallback(any());

        assertThrows(HostException.class, () -> mAppManager.setSurfaceCallback(null));
    }

    @Test
    public void setSurfaceListener_hostThrowsRuntimeException_throwsHostException()
            throws RemoteException {
        doThrow(new IllegalStateException()).when(mMockAppHost).setSurfaceCallback(any());

        assertThrows(HostException.class, () -> mAppManager.setSurfaceCallback(null));
    }
}
