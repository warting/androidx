/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.androidx.webkit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebViewRenderProcess;
import androidx.webkit.WebViewRenderProcessClient;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** An activity demonstrating the behaviour of renderer APIs. */
public class RendererTerminationActivity extends AppCompatActivity {
    private TextView mStatus;
    private WebView mWebView;
    private JSBlocker mBlocker;

    private Button mTerminateButton;
    private Button mBlockButton;
    private Button mBlockTransientButton;
    private Button mUnblockButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.renderer_termination_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        setContentView(R.layout.activity_renderer_termination);
        WebkitHelpers.enableEdgeToEdge(this);

        mWebView = findViewById(R.id.renderer_termination_webview);
        mStatus = findViewById(R.id.renderer_termination_status);

        mTerminateButton = findViewById(R.id.renderer_termination_terminate_button);
        mBlockButton = findViewById(R.id.renderer_termination_block_button);
        mBlockTransientButton = findViewById(R.id.renderer_termination_block_transient_button);
        mUnblockButton = findViewById(R.id.renderer_termination_unblock_button);

        recreateWebView();

        if (!WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE)) {
            mStatus.setText("API not available");
        }

        mTerminateButton.setOnClickListener((View view) -> terminateWebViewRenderer());
        mBlockButton.setOnClickListener((View view) -> blockWebViewRenderer());
        mBlockTransientButton.setOnClickListener((View view) -> blockWebViewRenderer(10000));
        mUnblockButton.setOnClickListener((View view) -> unblockWebViewRenderer());
    }

    /** A renderer terminated {@link DialogFragment}. */
    public static class RendererTerminatedDialogFragment extends DialogFragment {
        /** Creates a new RendererTerminatedDialogFragment instance. */
        public static @NonNull RendererTerminatedDialogFragment newInstance() {
            RendererTerminatedDialogFragment dialog = new RendererTerminatedDialogFragment();
            dialog.setCancelable(false);
            return dialog;
        }

        @Override
        public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            View v = LayoutInflater.from(getActivity())
                    .inflate(R.layout.fragment_renderer_terminated, null);
            final Dialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Renderer terminated")
                    .setView(v)
                    .setPositiveButton(android.R.string.ok,
                        (DialogInterface dialogInterface, int button) ->
                                ((RendererTerminationActivity) getActivity()).recreateWebView())
                    .create();
            return dialog;
        }
    }

    /** A renderer unresponsive {@link DialogFragment}. */
    public static class RendererUnresponsiveDialogFragment extends DialogFragment {
        /** Creates a new RendererUnresponsiveDialogFragment instance. */
        public static @NonNull RendererUnresponsiveDialogFragment newInstance() {
            RendererUnresponsiveDialogFragment dialog = new RendererUnresponsiveDialogFragment();
            dialog.setCancelable(false);
            return dialog;
        }

        @Override
        public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            View v = LayoutInflater.from(getActivity())
                    .inflate(R.layout.fragment_renderer_unresponsive, null);
            final Dialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Renderer unresponsive")
                    .setView(v)
                    .setNegativeButton("Terminate",
                            (DialogInterface dialogInterface, int button) ->
                                    ((RendererTerminationActivity) getActivity())
                                            .terminateWebViewRenderer())
                    .setPositiveButton("Wait", (DialogInterface dialogInterface, int button) -> {})
                    .create();
            return dialog;
        }
    }

    /**
     * Injected Javascript interface that provides a blocking call.
     *
     * Uses a lock to wait until the notify call arrives, which blocks the renderer.
     */
    private class JSBlocker {

        private final Object mLock = new Object();

        boolean mBlocked = false;

        JSBlocker() {
            updateButtonState(false);
        }

        void unblock() {
            synchronized (mLock) {
                mBlocked = false;
                mLock.notify();
                updateButtonState(false);
            }
        }

        void beginBlocking() {
            synchronized (mLock) {
                mBlocked = true;
                evaluateBeginBlockingJavascript();
                updateButtonState(true);
            }
        }

        @SuppressWarnings("unused") // used from javascript
        @JavascriptInterface
        public void block() throws Exception {
            synchronized (mLock) {
                while (mBlocked) {
                    mLock.wait();
                }
            }
        }
    }

    // Called from inside inner class to avoid synthetic accessors
    void evaluateBeginBlockingJavascript() {
        mWebView.evaluateJavascript("__blocker__.block();", null);
    }

    @SuppressWarnings("deprecation")
    private void blockWebViewRenderer(int durationMs) {
        mBlocker.beginBlocking();
        new Handler().postDelayed(this::unblockWebViewRenderer, durationMs);
    }

    private void blockWebViewRenderer() {
        mBlocker.beginBlocking();
    }

    private void unblockWebViewRenderer() {
        mBlocker.unblock();
    }

    void terminateWebViewRenderer() {
        if (WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE)) {
            WebViewCompat.getWebViewRenderProcess(mWebView).terminate();
        }
    }

    void updateButtonState(boolean isBlocked) {
        mBlockButton.setEnabled(!isBlocked);
        mBlockTransientButton.setEnabled(!isBlocked);
        mUnblockButton.setEnabled(isBlocked);
        mTerminateButton.setEnabled(WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE));
    }

    @SuppressLint("SetJavaScriptEnabled")
    void recreateWebView() {
        FrameLayout layout = (FrameLayout) mWebView.getParent();
        LayoutParams params = mWebView.getLayoutParams();
        layout.removeView(mWebView);
        mWebView.destroy();
        mWebView = new WebView(this);
        mWebView.setLayoutParams(params);
        layout.addView(mWebView);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                mWebView.destroy();
                mBlocker.unblock();

                DialogFragment dialog = RendererTerminatedDialogFragment.newInstance();
                dialog.show(getSupportFragmentManager(), "dialog-terminated");

                mStatus.setText("terminated");
                return true;
            }
        });

        mWebView.getSettings().setJavaScriptEnabled(true);
        if (mBlocker != null) {
            mBlocker.unblock();
        }
        mBlocker = new JSBlocker();
        mWebView.addJavascriptInterface(mBlocker, "__blocker__");
        mStatus.setText("started");

        mWebView.loadUrl("http://www.wikipedia.org/wiki/Cat");
        if (WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE)) {
            WebViewCompat.setWebViewRenderProcessClient(mWebView, new WebViewRenderProcessClient() {
                @Override
                public void onRenderProcessUnresponsive(
                        @NonNull WebView view, @Nullable WebViewRenderProcess renderer) {
                    mStatus.setText("unresponsive");
                    DialogFragment dialog = (DialogFragment) getSupportFragmentManager()
                            .findFragmentByTag("dialog-unresponsive");
                    if (dialog == null) {
                        dialog = RendererUnresponsiveDialogFragment.newInstance();
                        dialog.show(getSupportFragmentManager(), "dialog-unresponsive");
                    }
                }
                @Override
                public void onRenderProcessResponsive(@NonNull WebView view,
                        @Nullable WebViewRenderProcess renderer) {
                    mStatus.setText("responsive");
                    DialogFragment dialog = (DialogFragment) getSupportFragmentManager()
                            .findFragmentByTag("dialog-unresponsive");
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            });
        }

    }
}
