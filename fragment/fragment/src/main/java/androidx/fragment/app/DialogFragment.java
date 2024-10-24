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

package androidx.fragment.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.fragment.app.FragmentManager.TAG;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.ComponentDialog;
import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A fragment that displays a dialog window, floating in the foreground of its
 * activity's window.  This fragment contains a Dialog object, which it
 * displays as appropriate based on the fragment's state.  Control of
 * the dialog (deciding when to show, hide, dismiss it) should be done through
 * the APIs here, not with direct calls on the dialog.
 *
 * <p>Implementations should override this class and implement
 * {@link #onViewCreated(View, Bundle)} to supply the
 * content of the dialog.  Alternatively, they can override
 * {@link #onCreateDialog(Bundle)} to create an entirely custom dialog, such
 * as an AlertDialog, with its own content.
 *
 * <p>Topics covered here:
 * <ol>
 * <li><a href="#Lifecycle">Lifecycle</a>
 * <li><a href="#BasicDialog">Basic Dialog</a>
 * <li><a href="#AlertDialog">Alert Dialog</a>
 * <li><a href="#DialogOrEmbed">Selecting Between Dialog or Embedding</a>
 * </ol>
 *
 * <a name="Lifecycle"></a>
 * <h3>Lifecycle</h3>
 *
 * <p>DialogFragment does various things to keep the fragment's lifecycle
 * driving it, instead of the Dialog.  Note that dialogs are generally
 * autonomous entities -- they are their own window, receiving their own
 * input events, and often deciding on their own when to disappear (by
 * receiving a back key event or the user clicking on a button).
 *
 * <p>DialogFragment needs to ensure that what is happening with the Fragment
 * and Dialog states remains consistent.  To do this, it watches for dismiss
 * events from the dialog and takes care of removing its own state when they
 * happen.  This means you should use {@link #show(FragmentManager, String)},
 * {@link #show(FragmentTransaction, String)}, or {@link #showNow(FragmentManager, String)}
 * to add an instance of DialogFragment to your UI, as these keep track of
 * how DialogFragment should remove itself when the dialog is dismissed.
 *
 * <a name="BasicDialog"></a>
 * <h3>Basic Dialog</h3>
 *
 * <p>The simplest use of DialogFragment is as a floating container for the
 * fragment's view hierarchy.  A simple implementation may look like this:
 *
 * <pre>
 * public class MyDialogFragment extends DialogFragment {
 *     int mNum;
 *
 *     // Create a new instance of MyDialogFragment, providing "num" as an argument.
 *     static MyDialogFragment newInstance(int num) {
 *         MyDialogFragment f = new MyDialogFragment();
 *
 *         // Supply num input as an argument.
 *         Bundle args = new Bundle();
 *         args.putInt("num", num);
 *         f.setArguments(args);
 *
 *         return f;
 *     }
 *
 *     {@literal @}Override
 *     public void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         mNum = getArguments().getInt("num");
 *
 *         // Pick a style based on the num.
 *         int style = DialogFragment.STYLE_NORMAL, theme = 0;
 *         switch ((mNum-1)%6) {
 *             case 1: style = DialogFragment.STYLE_NO_TITLE; break;
 *             case 2: style = DialogFragment.STYLE_NO_FRAME; break;
 *             case 3: style = DialogFragment.STYLE_NO_INPUT; break;
 *             case 4: style = DialogFragment.STYLE_NORMAL; break;
 *             case 5: style = DialogFragment.STYLE_NORMAL; break;
 *             case 6: style = DialogFragment.STYLE_NO_TITLE; break;
 *             case 7: style = DialogFragment.STYLE_NO_FRAME; break;
 *             case 8: style = DialogFragment.STYLE_NORMAL; break;
 *         }
 *         switch ((mNum-1)%6) {
 *             case 4: theme = android.R.style.Theme_Holo; break;
 *             case 5: theme = android.R.style.Theme_Holo_Light_Dialog; break;
 *             case 6: theme = android.R.style.Theme_Holo_Light; break;
 *             case 7: theme = android.R.style.Theme_Holo_Light_Panel; break;
 *             case 8: theme = android.R.style.Theme_Holo_Light; break;
 *         }
 *         setStyle(style, theme);
 *     }
 *
 *     {@literal @}Override
 *     public View onCreateView(LayoutInflater inflater, ViewGroup container,
 *                              Bundle savedInstanceState) {
 *         return inflater.inflate(R.layout.fragment_dialog, container, false);
 *     }
 *
 *     {@literal @}Override
 *     public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
 *         super.onViewCreated(view, savedInstanceState);
 *
 *         // set DialogFragment title
 *         getDialog().setTitle("Dialog #" + mNum);
 *     }
 * }
 * </pre>
 *
 * <p>An example showDialog() method on the Activity could be:
 *
 * <pre>
 * public void showDialog() {
 *     mStackLevel++;
 *
 *     // DialogFragment.show() will take care of adding the fragment
 *     // in a transaction.  We also want to remove any currently showing
 *     // dialog, so make our own transaction and take care of that here.
 *     FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
 *     Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
 *     if (prev != null) {
 *         ft.remove(prev);
 *     }
 *     ft.addToBackStack(null);
 *
 *     // Create and show the dialog.
 *     DialogFragment newFragment = MyDialogFragment.newInstance(mStackLevel);
 *     newFragment.show(ft, "dialog");
 * }
 * </pre>
 *
 * <p>This removes any currently shown dialog, creates a new DialogFragment
 * with an argument, and shows it as a new state on the back stack.  When the
 * transaction is popped, the current DialogFragment and its Dialog will be
 * destroyed, and the previous one (if any) re-shown.  Note that in this case
 * DialogFragment will take care of popping the transaction of the Dialog that
 * is dismissed separately from it.
 *
 * <a name="AlertDialog"></a>
 * <h3>Alert Dialog</h3>
 *
 * <p>Instead of (or in addition to) implementing {@link #onViewCreated(View, Bundle)} to
 * generate the view hierarchy inside of a dialog, you may implement
 * {@link #onCreateDialog(Bundle)} to create your own custom Dialog object.
 *
 * <p>This is most useful for creating an AlertDialog, allowing you
 * to display standard alerts to the user that are managed by a fragment.
 * A simple example implementation of this is:
 *
 * <pre>
 * public static class MyAlertDialogFragment extends DialogFragment {
 *
 *     public static MyAlertDialogFragment newInstance(int title) {
 *         MyAlertDialogFragment frag = new MyAlertDialogFragment();
 *         Bundle args = new Bundle();
 *         args.putInt("title", title);
 *         frag.setArguments(args);
 *         return frag;
 *     }
 *
 *     {@literal @}Override
 *     public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
 *
 *         return new AlertDialog.Builder(getActivity())
 *                 .setIcon(R.drawable.alert_dialog_icon)
 *                 .setTitle(title)
 *                 .setPositiveButton(R.string.alert_dialog_ok,
 *                         (dialogInterface, i) -> ((MainActivity)getActivity()).doPositiveClick())
 *                 .setNegativeButton(R.string.alert_dialog_cancel,
 *                         (dialogInterface, i) -> ((MainActivity)getActivity()).doNegativeClick())
 *                 .create();
 *         return super.onCreateDialog(savedInstanceState);
 *     }
 * }
 * </pre>
 *
 * <p>The activity creating this fragment may have the following methods to
 * show the dialog and receive results from it:
 *
 * <pre>
 * void showDialog() {
 *     DialogFragment newFragment = MyAlertDialogFragment.newInstance(
 *             R.string.alert_dialog_two_buttons_title);
 *     newFragment.show(getSupportFragmentManager(), "dialog");
 * }
 *
 * public void doPositiveClick() {
 *     // Do stuff here.
 *     Log.i("MainActivity", "Positive click!");
 * }
 *
 * public void doNegativeClick() {
 *     // Do stuff here.
 *     Log.i("MainActivity", "Negative click!");
 * }
 * </pre>
 *
 * <p>Note that in this case the fragment is not placed on the back stack, it
 * is just added as an indefinitely running fragment.  Because dialogs normally
 * are modal, this will still operate as a back stack, since the dialog will
 * capture user input until it is dismissed.  When it is dismissed, DialogFragment
 * will take care of removing itself from its fragment manager.
 *
 * <a name="DialogOrEmbed"></a>
 * <h3>Selecting Between Dialog or Embedding</h3>
 *
 * <p>A DialogFragment can still optionally be used as a normal fragment, if
 * desired.  This is useful if you have a fragment that in some cases should
 * be shown as a dialog and others embedded in a larger UI.  This behavior
 * will normally be automatically selected for you based on how you are using
 * the fragment, but can be customized with {@link #setShowsDialog(boolean)}.
 *
 * <p>For example, here is a simple dialog fragment:
 *
 * <pre>
 * public static class MyDialogFragment extends DialogFragment {
 *     static MyDialogFragment newInstance() {
 *         return new MyDialogFragment();
 *     }
 *
 *     {@literal @}Override
 *     public void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *
 *         // this fragment will be displayed in a dialog
 *         setShowsDialog(true);
 *     }
 *
 *     {@literal @}Override
 *     public View onCreateView(LayoutInflater inflater, ViewGroup container,
 *             Bundle savedInstanceState) {
 *         View v = inflater.inflate(R.layout.hello_world, container, false);
 *         View tv = v.findViewById(R.id.text);
 *         ((TextView)tv).setText("This is an instance of MyDialogFragment");
 *         return v;
 *     }
 * }
 * </pre>
 *
 * <p>An instance of this fragment can be created and shown as a dialog:
 *
 * <pre>
 * void showDialog() {
 *     // Create the fragment and show it as a dialog.
 *     DialogFragment newFragment = MyDialogFragment.newInstance();
 *     newFragment.show(getSupportFragmentManager(), "dialog");
 * }
 * </pre>
 *
 * <p>It can also be added as content in a view hierarchy:
 *
 * <pre>
 * FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
 * DialogFragment newFragment = MyDialogFragment.newInstance();
 * ft.add(R.id.embedded, newFragment);
 * ft.commit();
 * </pre>
 */
public class DialogFragment extends Fragment
        implements DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogStyle {}

    /**
     * Style for {@link #setStyle(int, int)}: a basic,
     * normal dialog.
     */
    public static final int STYLE_NORMAL = 0;

    /**
     * Style for {@link #setStyle(int, int)}: don't include
     * a title area.
     */
    public static final int STYLE_NO_TITLE = 1;

    /**
     * Style for {@link #setStyle(int, int)}: don't draw
     * any frame at all; the view hierarchy returned by {@link #onCreateView}
     * is entirely responsible for drawing the dialog.
     */
    public static final int STYLE_NO_FRAME = 2;

    /**
     * Style for {@link #setStyle(int, int)}: like
     * {@link #STYLE_NO_FRAME}, but also disables all input to the dialog.
     * The user can not touch it, and its window will not receive input focus.
     */
    public static final int STYLE_NO_INPUT = 3;

    private static final String SAVED_DIALOG_STATE_TAG = "android:savedDialogState";
    private static final String SAVED_STYLE = "android:style";
    private static final String SAVED_THEME = "android:theme";
    private static final String SAVED_CANCELABLE = "android:cancelable";
    private static final String SAVED_SHOWS_DIALOG = "android:showsDialog";
    private static final String SAVED_BACK_STACK_ID = "android:backStackId";
    /**
     * Copied from {@link Dialog}.
     */
    private static final String SAVED_INTERNAL_DIALOG_SHOWING = "android:dialogShowing";

    private Handler mHandler;
    private Runnable mDismissRunnable = new Runnable() {
        @Override
        public void run() {
            mOnDismissListener.onDismiss(mDialog);
        }
    };

    private DialogInterface.OnCancelListener mOnCancelListener =
            new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(@Nullable DialogInterface dialog) {
            if (mDialog != null) {
                DialogFragment.this.onCancel(mDialog);
            }
        }
    };

    private DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(@Nullable DialogInterface dialog) {
            if (mDialog != null) {
                DialogFragment.this.onDismiss(mDialog);
            }
        }
    };

    private int mStyle = STYLE_NORMAL;
    private int mTheme = 0;
    private boolean mCancelable = true;
    private boolean mShowsDialog = true;
    private int mBackStackId = -1;
    private boolean mCreatingDialog;
    private Observer<LifecycleOwner> mObserver = new Observer<LifecycleOwner>() {
        @Override
        public void onChanged(LifecycleOwner lifecycleOwner) {
            if (lifecycleOwner != null && mShowsDialog) {
                View view = requireView();
                if (view.getParent() != null) {
                    throw new IllegalStateException(
                            "DialogFragment can not be attached to a container view");
                }
                if (mDialog != null) {
                    if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                        Log.d(TAG, "DialogFragment " + this + " setting the content view on "
                                + mDialog);
                    }
                    mDialog.setContentView(view);
                }
            }
        }
    };

    private @Nullable Dialog mDialog;
    private boolean mViewDestroyed;
    private boolean mDismissed;
    private boolean mShownByMe;
    private boolean mDialogCreated = false;


    /**
     * Constructor used by the default {@link FragmentFactory}. You must
     * {@link FragmentManager#setFragmentFactory(FragmentFactory) set a custom FragmentFactory}
     * if you want to use a non-default constructor to ensure that your constructor
     * is called when the fragment is re-instantiated.
     *
     * <p>It is strongly recommended to supply arguments with {@link #setArguments}
     * and later retrieved by the Fragment with {@link #getArguments}. These arguments
     * are automatically saved and restored alongside the Fragment.
     *
     * <p>Applications should generally not implement a constructor. Prefer
     * {@link #onAttach(Context)} instead. It is the first place application code can run where
     * the fragment is ready to be used - the point where the fragment is actually associated with
     * its context.
     */
    public DialogFragment() {
        super();
    }

    /**
     * Alternate constructor that can be called from your default, no argument constructor to
     * provide a default layout that will be inflated by
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     *
     * <pre class="prettyprint">
     * class MyDialogFragment extends DialogFragment {
     *   public MyDialogFragment() {
     *     super(R.layout.dialog_fragment_main);
     *   }
     * }
     * </pre>
     *
     * You must
     * {@link FragmentManager#setFragmentFactory(FragmentFactory) set a custom FragmentFactory}
     * if you want to use a non-default constructor to ensure that your constructor is called
     * when the fragment is re-instantiated.
     *
     * @see #DialogFragment()
     * @see #onCreateView(LayoutInflater, ViewGroup, Bundle)
     */
    public DialogFragment(@LayoutRes int contentLayoutId) {
        super(contentLayoutId);
    }

    /**
     * Call to customize the basic appearance and behavior of the
     * fragment's dialog.  This can be used for some common dialog behaviors,
     * taking care of selecting flags, theme, and other options for you.  The
     * same effect can be achieve by manually setting Dialog and Window
     * attributes yourself.  Calling this after the fragment's Dialog is
     * created will have no effect.
     *
     * @param style Selects a standard style: may be {@link #STYLE_NORMAL},
     * {@link #STYLE_NO_TITLE}, {@link #STYLE_NO_FRAME}, or
     * {@link #STYLE_NO_INPUT}.
     * @param theme Optional custom theme.  If 0, an appropriate theme (based
     * on the style) will be selected for you.
     */
    public void setStyle(@DialogStyle int style, @StyleRes int theme) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.d(TAG, "Setting style and theme for DialogFragment " + this + " to " + style
                    + ", " + theme);
        }
        mStyle = style;
        if (mStyle == STYLE_NO_FRAME || mStyle == STYLE_NO_INPUT) {
            mTheme = android.R.style.Theme_Panel;
        }
        if (theme != 0) {
            mTheme = theme;
        }
    }

    /**
     * Display the dialog, adding the fragment to the given FragmentManager.  This
     * is a convenience for explicitly creating a transaction, adding the
     * fragment to it with the given tag, and {@link FragmentTransaction#commit() committing} it.
     * This does <em>not</em> add the transaction to the fragment back stack.  When the fragment
     * is dismissed, a new transaction will be executed to remove it from
     * the activity.
     * @param manager The FragmentManager this fragment will be added to.
     * @param tag The tag for this fragment, as per
     * {@link FragmentTransaction#add(Fragment, String) FragmentTransaction.add}.
     */
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        mDismissed = false;
        mShownByMe = true;
        FragmentTransaction ft = manager.beginTransaction();
        ft.setReorderingAllowed(true);
        ft.add(this, tag);
        ft.commit();
    }

    /**
     * Display the dialog, adding the fragment using an existing transaction
     * and then {@link FragmentTransaction#commit() committing} the transaction.
     * @param transaction An existing transaction in which to add the fragment.
     * @param tag The tag for this fragment, as per
     * {@link FragmentTransaction#add(Fragment, String) FragmentTransaction.add}.
     * @return Returns the identifier of the committed transaction, as per
     * {@link FragmentTransaction#commit() FragmentTransaction.commit()}.
     */
    public int show(@NonNull FragmentTransaction transaction, @Nullable String tag) {
        mDismissed = false;
        mShownByMe = true;
        transaction.add(this, tag);
        mViewDestroyed = false;
        mBackStackId = transaction.commit();
        return mBackStackId;
    }

    /**
     * Display the dialog, immediately adding the fragment to the given FragmentManager.  This
     * is a convenience for explicitly creating a transaction, adding the
     * fragment to it with the given tag, and calling {@link FragmentTransaction#commitNow()}.
     * This does <em>not</em> add the transaction to the fragment back stack.  When the fragment
     * is dismissed, a new transaction will be executed to remove it from
     * the activity.
     * @param manager The FragmentManager this fragment will be added to.
     * @param tag The tag for this fragment, as per
     * {@link FragmentTransaction#add(Fragment, String) FragmentTransaction.add}.
     */
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        mDismissed = false;
        mShownByMe = true;
        FragmentTransaction ft = manager.beginTransaction();
        ft.setReorderingAllowed(true);
        ft.add(this, tag);
        ft.commitNow();
    }

    /**
     * Dismiss the fragment and its dialog.  If the fragment was added to the
     * back stack, all back stack state up to and including this entry will
     * be popped.  Otherwise, a new transaction will be committed to remove
     * the fragment.
     */
    public void dismiss() {
        dismissInternal(false, false, false);
    }

    /**
     * Version of {@link #dismiss()} that uses {@link FragmentTransaction#commitNow()}.
     * See linked documentation for further details.
     */
    @MainThread
    public void dismissNow() {
        dismissInternal(false, false, true);
    }

    /**
     * Version of {@link #dismiss()} that uses
     * {@link FragmentTransaction#commitAllowingStateLoss()
     * FragmentTransaction.commitAllowingStateLoss()}. See linked
     * documentation for further details.
     */
    public void dismissAllowingStateLoss() {
        dismissInternal(true, false, false);
    }

    private void dismissInternal(boolean allowStateLoss, boolean fromOnDismiss, boolean immediate) {
        if (mDismissed) {
            return;
        }
        mDismissed = true;
        mShownByMe = false;
        if (mDialog != null) {
            // Instead of waiting for a posted onDismiss(), null out
            // the listener and call onDismiss() manually to ensure
            // that the callback happens before onDestroy()
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            if (!fromOnDismiss) {
                // onDismiss() is always called on the main thread, so
                // we mimic that behavior here. The difference here is that
                // we don't post the message to ensure that the onDismiss()
                // callback still happens before onDestroy()
                if (Looper.myLooper() == mHandler.getLooper()) {
                    onDismiss(mDialog);
                } else {
                    mHandler.post(mDismissRunnable);
                }
            }
        }
        mViewDestroyed = true;
        if (mBackStackId >= 0) {
            if (immediate) {
                getParentFragmentManager().popBackStackImmediate(mBackStackId,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                getParentFragmentManager().popBackStack(mBackStackId,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE, allowStateLoss);
            }
            mBackStackId = -1;
        } else {
            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.setReorderingAllowed(true);
            ft.remove(this);
            // allowStateLoss and immediate should not both be true
            if (immediate) {
                ft.commitNow();
            } else if (allowStateLoss) {
                ft.commitAllowingStateLoss();
            } else {
                ft.commit();
            }
        }
    }

    /**
     * Return the {@link Dialog} this fragment is currently controlling.
     *
     * @see #requireDialog()
     */
    public @Nullable Dialog getDialog() {
        return mDialog;
    }

    /**
     * Return the {@link Dialog} this fragment is currently controlling.
     *
     * @throws IllegalStateException if the Dialog has not yet been created (before
     * {@link #onCreateDialog(Bundle)}) or has been destroyed (after {@link #onDestroyView()}.
     * @see #getDialog()
     */
    public final @NonNull Dialog requireDialog() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            throw new IllegalStateException("DialogFragment " + this + " does not have a Dialog.");
        }
        return dialog;
    }

    /**
     * Return the {@link ComponentDialog} this fragment is currently controlling.
     *
     * @throws IllegalStateException if the Dialog found is not a ComponentDialog or
     * if Dialog has not yet been created (before {@link #onCreateDialog(Bundle)})
     * or has been destroyed (after {@link #onDestroyView()}.
     *
     * @see #requireDialog()
     */
    public final @NonNull ComponentDialog requireComponentDialog() {
        Dialog dialog = requireDialog();
        if (!(dialog instanceof ComponentDialog)) {
            throw new IllegalStateException("DialogFragment " + this
                    + " did not return a ComponentDialog instance from "
                    + "requireDialog(). The actual Dialog is " + dialog);
        }
        return (ComponentDialog) dialog;
    }

    @StyleRes
    public int getTheme() {
        return mTheme;
    }

    /**
     * Control whether the shown Dialog is cancelable.  Use this instead of
     * directly calling {@link Dialog#setCancelable(boolean)
     * Dialog.setCancelable(boolean)}, because DialogFragment needs to change
     * its behavior based on this.
     *
     * @param cancelable If true, the dialog is cancelable.  The default
     * is true.
     */
    public void setCancelable(boolean cancelable) {
        mCancelable = cancelable;
        if (mDialog != null) mDialog.setCancelable(cancelable);
    }

    /**
     * Return the current value of {@link #setCancelable(boolean)}.
     */
    public boolean isCancelable() {
        return mCancelable;
    }

    /**
     * Controls whether this fragment should be shown in a dialog.  If not
     * set, no Dialog will be created and the fragment's view hierarchy will
     * thus not be added to it.  This allows you to instead use it as a
     * normal fragment (embedded inside of its activity).
     *
     * <p>This is normally set for you based on whether the fragment is
     * associated with a container view ID passed to
     * {@link FragmentTransaction#add(int, Fragment) FragmentTransaction.add(int, Fragment)}.
     * If the fragment was added with a container, setShowsDialog will be
     * initialized to false; otherwise, it will be true.
     *
     * <p>If calling this manually, it should be called in {@link #onCreate(Bundle)}
     * as calling it any later will have no effect.
     *
     * @param showsDialog If true, the fragment will be displayed in a Dialog.
     * If false, no Dialog will be created and the fragment's view hierarchy
     * left undisturbed.
     */
    public void setShowsDialog(boolean showsDialog) {
        mShowsDialog = showsDialog;
    }

    /**
     * Return the current value of {@link #setShowsDialog(boolean)}.
     */
    public boolean getShowsDialog() {
        return mShowsDialog;
    }

    @MainThread
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getViewLifecycleOwnerLiveData().observeForever(mObserver);
        if (!mShownByMe) {
            // If not explicitly shown through our API, take this as an
            // indication that the dialog is no longer dismissed.
            mDismissed = false;
        }
    }

    @MainThread
    @Override
    public void onDetach() {
        super.onDetach();
        if (!mShownByMe && !mDismissed) {
            // The fragment was not shown by a direct call here, it is not
            // dismissed, and now it is being detached...  well, okay, thou
            // art now dismissed.  Have fun.
            mDismissed = true;
        }
        getViewLifecycleOwnerLiveData().removeObserver(mObserver);
    }

    @MainThread
    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This assumes that onCreate() is being called on the main thread
        mHandler = new Handler();

        mShowsDialog = mContainerId == 0;

        if (savedInstanceState != null) {
            mStyle = savedInstanceState.getInt(SAVED_STYLE, STYLE_NORMAL);
            mTheme = savedInstanceState.getInt(SAVED_THEME, 0);
            mCancelable = savedInstanceState.getBoolean(SAVED_CANCELABLE, true);
            mShowsDialog = savedInstanceState.getBoolean(SAVED_SHOWS_DIALOG, mShowsDialog);
            mBackStackId = savedInstanceState.getInt(SAVED_BACK_STACK_ID, -1);
        }
    }

    @Override
    void performCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        super.performCreateView(inflater, container, savedInstanceState);
        // If no view was set, we need to call onRestoreInstance on the dialog to ensure
        // the state is restored.
        if (mView == null) {
            if (mDialog != null && savedInstanceState != null) {
                Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_TAG);
                if (dialogState != null) {
                    mDialog.onRestoreInstanceState(dialogState);
                }
            }
        }
    }

    @Override
    @NonNull FragmentContainer createFragmentContainer() {
        final FragmentContainer fragmentContainer = super.createFragmentContainer();
        return new FragmentContainer() {
            @Override
            public @Nullable View onFindViewById(int id) {
                if (fragmentContainer.onHasView()) {
                    return fragmentContainer.onFindViewById(id);
                }
                return DialogFragment.this.onFindViewById(id);
            }

            @Override
            public boolean onHasView() {
                return  fragmentContainer.onHasView() || DialogFragment.this.onHasView();
            }
        };
    }

    @Nullable View onFindViewById(int id) {
        if (mDialog != null) {
            return mDialog.findViewById(id);
        }
        return null;
    }

    boolean onHasView() {
        return mDialogCreated;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If this is called from within {@link #onCreateDialog(Bundle)}, the layout inflater from
     * {@link Fragment#onGetLayoutInflater(Bundle)}, without the dialog theme, will be returned.
     */
    @Override
    public @NonNull LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        LayoutInflater layoutInflater = super.onGetLayoutInflater(savedInstanceState);
        if (!mShowsDialog || mCreatingDialog) {
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                String message = "getting layout inflater for DialogFragment " + this;
                if (!mShowsDialog) {
                    Log.d(TAG, "mShowsDialog = false: " + message);
                } else {
                    Log.d(TAG, "mCreatingDialog = true: " + message);
                }
            }
            return layoutInflater;
        }

        prepareDialog(savedInstanceState);

        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.d(TAG, "get layout inflater for DialogFragment " + this + " from dialog context");
        }

        if (mDialog != null) {
            layoutInflater = layoutInflater.cloneInContext(mDialog.getContext());
        }
        return layoutInflater;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setupDialog(@NonNull Dialog dialog, int style) {
        switch (style) {
            case STYLE_NO_INPUT:
                Window window = dialog.getWindow();
                if (window != null) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
                // fall through...
            case STYLE_NO_FRAME:
            case STYLE_NO_TITLE:
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
    }

    /**
     * Override to build your own custom Dialog container.  This is typically
     * used to show an AlertDialog instead of a generic Dialog; when doing so,
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} does not need
     * to be implemented since the AlertDialog takes care of its own content.
     *
     * <p>This method will be called after {@link #onCreate(Bundle)} and
     * immediately before {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.  The
     * default implementation simply instantiates and returns a {@link Dialog}
     * class.
     *
     * <p><em>Note: DialogFragment own the {@link Dialog#setOnCancelListener
     * Dialog.setOnCancelListener} and {@link Dialog#setOnDismissListener
     * Dialog.setOnDismissListener} callbacks.  You must not set them yourself.</em>
     * To find out about these events, override {@link #onCancel(DialogInterface)}
     * and {@link #onDismiss(DialogInterface)}.</p>
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     * or null if this is a freshly created Fragment.
     *
     * @return Return a new Dialog instance to be displayed by the Fragment.
     */
    @MainThread
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "onCreateDialog called for DialogFragment " + this);
        }
        return new ComponentDialog(requireContext(), getTheme());
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
    }

    @CallSuper
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (!mViewDestroyed) {
            // Note: we need to use allowStateLoss, because the dialog
            // dispatches this asynchronously so we can receive the call
            // after the activity is paused.  Worst case, when the user comes
            // back to the activity they see the dialog again.
            if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                Log.d(TAG, "onDismiss called for DialogFragment " + this);
            }
            dismissInternal(true, true, false);
        }
    }

    private void prepareDialog(@Nullable Bundle savedInstanceState) {
        if (!mShowsDialog) {
            return;
        }

        if (!mDialogCreated) {
            try {
                mCreatingDialog = true;
                mDialog = onCreateDialog(savedInstanceState);
                // mShowsDialog might have changed in onCreateDialog, so we should only proceed
                // with setting up the dialog if mShowsDialog is still true
                if (mShowsDialog) {
                    setupDialog(mDialog, mStyle);
                    final Context context = getContext();
                    if (context instanceof Activity) {
                        mDialog.setOwnerActivity((Activity) context);
                    }
                    mDialog.setCancelable(mCancelable);
                    mDialog.setOnCancelListener(mOnCancelListener);
                    mDialog.setOnDismissListener(mOnDismissListener);
                    mDialogCreated = true;
                } else {
                    // Ensure that when mShowsDialog is set to false in onCreateDialog
                    // that getDialog() returns null
                    mDialog = null;
                }
            } finally {
                mCreatingDialog = false;
            }
        }
    }

    @MainThread
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (mDialog != null && savedInstanceState != null) {
            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_TAG);
            if (dialogState != null) {
                mDialog.onRestoreInstanceState(dialogState);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #onCreateDialog} for code touching the dialog created by
     * {@link #onCreateDialog}, {@link #onViewCreated(View, Bundle)} for code touching the
     * view created by {@link #onCreateView} and {@link #onCreate(Bundle)} for other initialization.
     * To get a callback specifically when a Fragment activity's
     * {@link Activity#onCreate(Bundle)} is called, register a
     * {@link androidx.lifecycle.LifecycleObserver} on the Activity's
     * {@link Lifecycle} in {@link #onAttach(Context)}, removing it when it receives the
     * {@link Lifecycle.State#CREATED} callback.
     */
    @SuppressWarnings("deprecation")
    @MainThread
    @Override
    @Deprecated
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @MainThread
    @Override
    public void onStart() {
        super.onStart();

        if (mDialog != null) {
            mViewDestroyed = false;
            mDialog.show();
            // Only after we show does the dialog window actually return a decor view.
            View decorView = mDialog.getWindow().getDecorView();
            ViewTreeLifecycleOwner.set(decorView, this);
            ViewTreeViewModelStoreOwner.set(decorView, this);
            ViewTreeSavedStateRegistryOwner.set(decorView, this);
        }
    }

    @MainThread
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mDialog != null) {
            Bundle dialogState = mDialog.onSaveInstanceState();
            dialogState.putBoolean(SAVED_INTERNAL_DIALOG_SHOWING, false);
            outState.putBundle(SAVED_DIALOG_STATE_TAG, dialogState);
        }
        if (mStyle != STYLE_NORMAL) {
            outState.putInt(SAVED_STYLE, mStyle);
        }
        if (mTheme != 0) {
            outState.putInt(SAVED_THEME, mTheme);
        }
        if (!mCancelable) {
            outState.putBoolean(SAVED_CANCELABLE, mCancelable);
        }
        if (!mShowsDialog) {
            outState.putBoolean(SAVED_SHOWS_DIALOG, mShowsDialog);
        }
        if (mBackStackId != -1) {
            outState.putInt(SAVED_BACK_STACK_ID, mBackStackId);
        }
    }

    @MainThread
    @Override
    public void onStop() {
        super.onStop();
        if (mDialog != null) {
            mDialog.hide();
        }
    }

    /**
     * Remove dialog.
     */
    @MainThread
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDialog != null) {
            // Set removed here because this dismissal is just to hide
            // the dialog -- we don't want this to cause the fragment to
            // actually be removed.
            mViewDestroyed = true;
            // Instead of waiting for a posted onDismiss(), null out
            // the listener and call onDismiss() manually to ensure
            // that the callback happens before onDestroy()
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            if (!mDismissed) {
                // Don't send a second onDismiss() callback if we've already
                // dismissed the dialog manually in dismissInternal()
                onDismiss(mDialog);
            }
            mDialog = null;
            mDialogCreated = false;
        }
    }
}
