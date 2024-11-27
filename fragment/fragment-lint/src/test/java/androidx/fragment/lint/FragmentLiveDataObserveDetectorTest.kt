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

@file:Suppress("UnstableApiUsage")

package androidx.fragment.lint

import androidx.fragment.lint.stubs.LIVEDATA_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FragmentLiveDataObserveDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = UnsafeFragmentLifecycleObserverDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnsafeFragmentLifecycleObserverDetector.LIVEDATA_ISSUE)

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, *LIVEDATA_STUBS).run()
    }

    @Test
    fun pass() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        liveData.observe(getViewLifecycleOwner(), Observer<String> {})
    }

    override fun onViewCreated() {
        test()
        val foo = Foo()
        foo.observeData(this)
        foo.observe(this)
    }

    private fun test() {
        val liveData = MutableLiveData<String>()
        liveData.observe(getViewLifecycleOwner(), Observer<String> {})
        test()
    }
}
            """
                ),
                kotlin(
                    """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData

class Foo {
    fun observeData(fragment: Fragment) {
        val liveData = MutableLiveData<String>()
        liveData.observe(LifecycleOwner(), Observer<String> {})
        liveData.observe(fragment.viewLifecycleOwner, Observer<String> {}, true)
    }

    fun observe(fragment: Fragment) {}
}
            """
                )
            )
            .expectClean()
    }

    @Test
    fun javaLintFixTest() {
        check(
                java(
                    """
package com.example;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

class TestFragment extends Fragment {

    @Override
    void onCreateView() {
        MutableLiveData<String> liveData = new MutableLiveData<String>();
        liveData.observe(this, new Observer<String>() {});
    }
}
            """
                )
            )
            .expect(
                """
src/com/example/TestFragment.java:12: Error: Use getViewLifecycleOwner() as the LifecycleOwner. [FragmentLiveDataObserve]
        liveData.observe(this, new Observer<String>() {});
                         ~~~~
1 errors, 0 warnings
            """
            )
            .checkFix(
                null,
                java(
                    """
package com.example;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

class TestFragment extends Fragment {

    @Override
    void onCreateView() {
        MutableLiveData<String> liveData = new MutableLiveData<String>();
        liveData.observe(getViewLifecycleOwner(), new Observer<String>() {});
    }
}
            """
                )
            )
    }

    @Test
    fun dialogJavaTestPass() {
        check(
                java(
                    """
package com.example;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;

class TestFragment extends DialogFragment {

    @Override
    void onCreateView() {
        MutableLiveData<String> liveData = new MutableLiveData<String>();
        liveData.observe(this, new Observer<String>() {});
    }
}
            """
                )
            )
            .expectClean()
    }

    @Test
    fun inMethodFails() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        liveData.observe(this, Observer<String> {})
    }
}
            """
                )
            )
            .expect(
                """
src/com/example/TestFragment.kt:11: Error: Use viewLifecycleOwner as the LifecycleOwner. [FragmentLiveDataObserve]
        liveData.observe(this, Observer<String> {})
                         ~~~~
1 errors, 0 warnings
            """
            )
            .checkFix(
                null,
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        liveData.observe(viewLifecycleOwner, Observer<String> {})
    }
}
            """
                )
            )
    }

    @Test
    fun inMethodDialogPass() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData

class TestFragment : DialogFragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        liveData.observe(this, Observer<String> {})
    }
}
            """
                )
            )
            .expectClean()
    }

    @Test
    fun helperMethodFails() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val liveData = MutableLiveData<String>()
        liveData.observe(this, Observer<String> {})
    }
}
            """
                )
            )
            .expect(
                """
src/com/example/TestFragment.kt:15: Error: Use viewLifecycleOwner as the LifecycleOwner. [FragmentLiveDataObserve]
        liveData.observe(this, Observer<String> {})
                         ~~~~
1 errors, 0 warnings
            """
            )
            .checkFix(
                null,
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val liveData = MutableLiveData<String>()
        liveData.observe(viewLifecycleOwner, Observer<String> {})
    }
}
            """
                )
            )
    }

    @Test
    fun externalCallFails() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.observeData(this)
    }
}
            """
                ),
                kotlin(
                    """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class Foo {
    fun observeData(fragment: Fragment) {
        val liveData = MutableLiveData<String>()
        liveData.observe(fragment, Observer<String> {})
    }
}
            """
                )
            )
            .expect(
                """
src/com/example/test/Foo.kt:10: Error: Unsafe call to observe with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentLiveDataObserve]
        liveData.observe(fragment, Observer<String> {})
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun externalCallFails_recursionViaInline() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.observeData(false, this)
    }
}
            """
                ),
                kotlin(
                    """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class Foo {
    inline fun <reified T> observeData(flag: T, fragment: Fragment) {
        if (flag is Boolean)
            anotherObserveData(flag, fragment)
        val liveData = MutableLiveData<String>()
        liveData.observe(fragment, Observer<String> {})
    }

    inline fun <reified T> anotherObserveData(flag: T, fragment: Fragment) {
        observeData(flag, fragment)
    }
}
            """
                )
            )
            .expect(
                """
src/com/example/test/Foo.kt:12: Error: Unsafe call to observe with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentLiveDataObserve]
        liveData.observe(fragment, Observer<String> {})
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun externalCallFails_butTooDeep() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.f1(this)
    }
}
            """
                ),
                kotlin(
                    """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class Foo {
    fun f1(fragment: Fragment) {
        f2(fragment)
    }
    fun f2(fragment: Fragment) {
        f3(fragment)
    }
    fun f3(fragment: Fragment) {
        f4(fragment)
    }
    fun f4(fragment: Fragment) {
        f5(fragment)
    }
    fun f5(fragment: Fragment) {
        f6(fragment)
    }
    fun f6(fragment: Fragment) {
        f7(fragment)
    }
    fun f7(fragment: Fragment) {
        f8(fragment)
    }
    fun f8(fragment: Fragment) {
        f9(fragment)
    }
    fun f9(fragment: Fragment) {
        f10(fragment)
    }
    fun f10(fragment: Fragment) {
        f11(fragment)
    }
    fun f11(fragment: Fragment) {
        f12(fragment)
    }
    fun f12(fragment: Fragment) {
        f13(fragment)
    }
    fun f13(fragment: Fragment) {
        f14(fragment)
    }
    fun f14(fragment: Fragment) {
        f15(fragment)
    }
    fun f15(fragment: Fragment) {
        f16(fragment)
    }
    fun f16(fragment: Fragment) {
        f17(fragment)
    }
    fun f17(fragment: Fragment) {
        f18(fragment)
    }
    fun f18(fragment: Fragment) {
        f19(fragment)
    }
    fun f19(fragment: Fragment) {
        f20(fragment)
    }
    fun f20(fragment: Fragment) {
        val liveData = MutableLiveData<String>()
        liveData.observe(fragment, Observer<String> {})
    }
}
            """
                )
            )
            .expectClean()
    }

    @Test
    fun externalHelperMethodFails() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.test.Foo

class TestFragment : Fragment {

    override fun onCreateView() {
        test()
    }

    private fun test() {
        val foo = Foo()
        foo.observeData(this)
    }
}
            """
                ),
                kotlin(
                    """
package com.example.test

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class Foo {
    private lateinit val fragment: Fragment

    fun observeData(fragment: Fragment) {
        this.fragment = fragment
        observe()
    }

    private fun observe() {
        val liveData = MutableLiveData<String>()
        liveData.observe(fragment, Observer<String> {})
    }
}
            """
                )
            )
            .expect(
                """
src/com/example/test/Foo.kt:17: Error: Unsafe call to observe with Fragment instance as LifecycleOwner from TestFragment.onCreateView. [FragmentLiveDataObserve]
        liveData.observe(fragment, Observer<String> {})
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun failWhenUsingLiveDataExtensionWithTheWrongLifecycleOwner() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveDataKt

class TestFragment: Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        LiveDataKt.observe(liveData, this) {
        }
    }
}
            """
                )
            )
            .expect(
                """
                src/com/example/TestFragment.kt:12: Error: Use viewLifecycleOwner as the LifecycleOwner. [FragmentLiveDataObserve]
                        LiveDataKt.observe(liveData, this) {
                                                     ~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .checkFix(
                null,
                kotlin(
                    """
                package com.example

                import androidx.fragment.app.Fragment
                import androidx.lifecycle.MutableLiveData
                import androidx.lifecycle.LiveDataKt

                class TestFragment: Fragment {

                    override fun onCreateView() {
                        val liveData = MutableLiveData<String>()
                        LiveDataKt.observe(liveData, viewLifecycleOwner) {
                        }
                    }
                }
                    """
                        .trimIndent()
                )
            )
    }

    @Test
    fun passWhenUsingExtensionCorrectly() {
        check(
                kotlin(
                    """
package com.example

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveDataKt

class TestFragment: Fragment {

    override fun onCreateView() {
        val liveData = MutableLiveData<String>()
        LiveDataKt.observe(liveData, viewLifecycleOwner) {
        }
    }
}
            """
                )
            )
            .expectClean()
    }
}
