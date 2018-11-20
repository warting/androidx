package org.jetbrains.kotlin.r4a

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.r4a.Component
import com.google.r4a.CompositionContext

import org.jetbrains.kotlin.extensions.KtxControlFlowExtension
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.parsing.KtxParsingExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class KtxCodegenTests : AbstractCodeGenTest() {

    @Test
    fun testCGSimpleTextView(): Unit = ensureSetup {
        compose(
            """
                <TextView text="Hello, world!" id=42 />
            """).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    fun testCGNSimpleTextView(): Unit = newCodeGen {
        compose(
            """
                // composer.emit(-555642258,
                //    { context -> TextView(context) },
                //    {
                //      set("Hello, world!") { text = it }
                //      set(42) { id = it }
                //    })

                <TextView text="Hello, world!" id=42 />
            """).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    fun testCGUpdatedComposition(): Unit = ensureSetup {
        var value = "Hello, world!"

        compose({ mapOf("value" to value) }, """
           <TextView text=value id=42 />
        """).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)

            value = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Other value", textView.text)
        }
    }

    @Test
    fun testCGNUpdatedComposition(): Unit = newCodeGen {
        var value = "Hello, world!"

        compose({ mapOf("value" to value) }, """
           <TextView text=value id=42 />
        """).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)

            value = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Other value", textView.text)
        }
    }

    @Test
    fun testCGViewGroup(): Unit = ensureSetup {
        val tvId = 258
        val llId = 260
        var text = "Hello, world!"
        var orientation = LinearLayout.HORIZONTAL

        compose({ mapOf("text" to text, "orientation" to orientation) }, """
            <LinearLayout orientation id=$llId>
              <TextView text id=$tvId />
            </LinearLayout>
        """).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)

            text = "Other value"
            orientation = LinearLayout.VERTICAL
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)
        }
    }

    @Test
    fun testCGNViewGroup(): Unit = newCodeGen {
        val tvId = 258
        val llId = 260
        var text = "Hello, world!"
        var orientation = LinearLayout.HORIZONTAL

        compose({ mapOf("text" to text, "orientation" to orientation) }, """
             <LinearLayout orientation id=$llId>
               <TextView text id=$tvId />
             </LinearLayout>
        """).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)

            text = "Other value"
            orientation = LinearLayout.VERTICAL
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)
        }
    }

    @Test
    fun testCGNSimpleCall(): Unit = newCodeGen {
        val tvId = 258
        var text = "Hello, world!"

        compose(
            """
                @Composable fun SomeFun(x: String) {
                    <TextView text=x id=$tvId />
                }
            """,
            { mapOf("text" to text) },
            """
                <SomeFun x=text />
            """
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)

            text = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)
        }
    }

    @Test
    fun testCGNSimpleCall2(): Unit = newCodeGen {
            val tvId = 258
            var text = "Hello, world!"
            var someInt = 456

            compose(
                """
                class SomeClass(var x: String) {
                    @Composable
                    operator fun invoke(y: Int) {
                        <TextView text="${"$"}x ${"$"}y" id=$tvId />
                    }
                }
            """,
                { mapOf("text" to text, "someInt" to someInt) },
                """
                <SomeClass x=text y=someInt />
            """
            ).then { activity ->
                val textView = activity.findViewById(tvId) as TextView

                assertEquals("Hello, world! 456", textView.text)

                text = "Other value"
                someInt = 123
            }.then { activity ->
                val textView = activity.findViewById(tvId) as TextView

                assertEquals("Other value 123", textView.text)
            }
        }

    @Test
    fun testCGNSimpleCall3(): Unit = newCodeGen {
        val tvId = 258
        var text = "Hello, world!"
        var someInt = 456

        compose(
            """
                @Memoized
                class SomeClassoawid(var x: String) {
                    @Composable
                    operator fun invoke(y: Int) {
                        <TextView text="${"$"}x ${"$"}y" id=$tvId />
                    }
                }
            """,
            { mapOf("text" to text, "someInt" to someInt) },
            """
                <SomeClassoawid x=text y=someInt />
            """
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals("Hello, world! 456", textView.text)

            text = "Other value"
            someInt = 123
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals("Other value 123", textView.text)
        }
    }

    @Test
    fun testCGNCallWithChildren(): Unit = newCodeGen {
        val tvId = 258
        var text = "Hello, world!"

        compose(
            """
                @Composable
                fun Block(@Children children: () -> Unit) {
                    <children />
                }
            """,
            { mapOf("text" to text) },
            """
                <Block>
                    <Block>
                        <TextView text id=$tvId />
                    </Block>
                </Block>
            """
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)

            text = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)
        }
    }

    @Test
    fun testCGComposableFunctionInvocationOneParameter(): Unit = ensureSetup {
        val tvId = 91
        var phone = "(123) 456-7890"
        compose("""
           fun Phone(value: String) {
             <TextView text=value id=$tvId />
           }
        """, { mapOf("phone" to phone)}, """
           <Phone value=phone />
        """).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            assertEquals(phone, textView.text)

            phone = "(123) 456-7899"
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            assertEquals(phone, textView.text)
        }
    }

    @Test
    fun testCGComposableFunctionInvocationTwoParameters(): Unit = ensureSetup {
        val tvId = 111
        val rsId = 112
        var left = 0
        var right = 0
        compose("""
           var addCalled = 0

           fun AddView(left: Int, right: Int) {
             addCalled++
             <TextView text="${'$'}left + ${'$'}right = ${'$'}{left + right}" id=$tvId />
             <TextView text="${'$'}addCalled" id=$rsId />
           }
        """, { mapOf("left" to left, "right" to right)}, """
           <AddView left right />
        """).then { activity ->
            // Should be called on the first compose
            assertEquals("1", (activity.findViewById(rsId) as TextView).text)
            assertEquals("$left + $right = ${left + right}", (activity.findViewById(tvId) as TextView).text)
        }.then { activity ->
            // Should be skipped on the second compose
            assertEquals("1", (activity.findViewById(rsId) as TextView).text)
            assertEquals("$left + $right = ${left + right}", (activity.findViewById(tvId) as TextView).text)

            left = 1
        }.then { activity ->
            // Should be called again because left changed.
            assertEquals("2", (activity.findViewById(rsId) as TextView).text)
            assertEquals("$left + $right = ${left + right}", (activity.findViewById(tvId) as TextView).text)

            right = 41
        }.then { activity ->
            // Should be called again because right changed
            assertEquals("3", (activity.findViewById(rsId) as TextView).text)
            assertEquals("$left + $right = ${left + right}", (activity.findViewById(tvId) as TextView).text)
        }.then { activity ->
            // Should be skipped because nothing changed
            assertEquals("3", (activity.findViewById(rsId) as TextView).text)
        }
    }

    // b/118610495
    @Test
    fun testCGChildCompose(): Unit = ensureSetup {
        val tvId = 153

        var text = "Test 1"

        compose("""
            var called = 0

            class TestContainer(@Children var children: @Composable() ()->Unit): Component() {
              override fun compose() {
                <LinearLayout>
                  <children />
                </LinearLayout>
              }
            }

            class TestClass(var text: String): Component() {
              override fun compose() {
                <TestContainer>
                  <TextView text id=$tvId />
                </TestContainer>
              }
            }
        """, { mapOf("text" to text) }, """
            <TestClass text />
        """).then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals(text, tv.text)

            text = "Test 2"
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals(text, tv.text)
        }
    }

    override fun setUp() {
        isSetup = true
        super.setUp()
        KtxTypeResolutionExtension.registerExtension(myEnvironment.project, R4aKtxTypeResolutionExtension())
        KtxControlFlowExtension.registerExtension(myEnvironment.project, R4aKtxControlFlowExtension())
        StorageComponentContainerContributor.registerExtension(myEnvironment.project, ComposableAnnotationChecker())
        TypeResolutionInterceptorExtension.registerExtension(myEnvironment.project, R4aTypeResolutionInterceptorExtension())
        SyntheticIrExtension.registerExtension(myEnvironment.project, R4ASyntheticIrExtension())
        KtxParsingExtension.registerExtension(myEnvironment.project, R4aKtxParsingExtension())
//        SyntheticResolveExtension.registerExtension(myEnvironment.project, StaticWrapperCreatorFunctionResolveExtension())
//        SyntheticResolveExtension.registerExtension(myEnvironment.project, WrapperViewSettersGettersResolveExtension())
    }

    private var isSetup = false
    private inline fun <T> ensureSetup(block: () -> T): T {
        if (!isSetup) setUp()
        return block()
    }

    private inline fun <T> newCodeGen(block: () -> T): T {
        ensureSetup {
            val oldFlag = R4AFlags.USE_NEW_TYPE_RESOLUTION
            R4AFlags.USE_NEW_TYPE_RESOLUTION = true
            try {
                return block()
            } finally {
                R4AFlags.USE_NEW_TYPE_RESOLUTION = oldFlag
            }
        }
    }

    fun compose(text: String, dumpClasses: Boolean = false): CompositionTest = compose({mapOf<String, Any>()}, text, dumpClasses)

    fun <T: Any> compose(valuesFactory: () -> Map<String, T>, text: String, dumpClasses: Boolean = false) = compose("", valuesFactory, text, dumpClasses)
    fun <T: Any> compose(prefix: String, valuesFactory: () -> Map<String, T>, text: String, dumpClasses: Boolean = false): CompositionTest {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        val candidateValues = valuesFactory()

        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        val parameterList = candidateValues.map { "${it.key}: ${it.value::class.qualifiedName}" }.joinToString()
        val parameterTypes = candidateValues.map { it.value::class.javaPrimitiveType ?: it.value::class.javaObjectType }.toTypedArray()

        val compiledClasses = classLoader("""
           import android.content.Context
           import android.widget.*
           import com.google.r4a.*

           $prefix

           class $className {

             fun test($parameterList) {
               $text
             }
           }
        """, fileName, dumpClasses)

        val allClassFiles = compiledClasses.allGeneratedFiles.filter { it.relativePath.endsWith(".class") }

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(this.javaClass.classLoader, null, bytes)
                if (loadedClass.name == className) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val testMethod = instanceClass.getMethod("test", *parameterTypes)

        return compose {
            val values = valuesFactory()
            val arguments = values.map { it.value as Any }.toTypedArray()
            testMethod.invoke(instanceOfClass, *arguments)
        }
    }
}

var uniqueNumber = 0

fun loadClass(loader: ClassLoader, name: String?, bytes: ByteArray): Class<*> {
    val defineClassMethod = ClassLoader::class.javaObjectType.getDeclaredMethod(
        "defineClass",
        String::class.javaObjectType,
        ByteArray::class.javaObjectType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType)
    defineClassMethod.isAccessible = true
    return defineClassMethod.invoke(loader, name, bytes, 0, bytes.size) as Class<*>
}

const val ROOT_ID = 18284847

private class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ROOT_ID })
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

private class Root : Component() {
    override fun compose() {}
}

class CompositionTest(val composable: () -> Unit) {

    inner class ActiveTest(val activity: Activity, val cc: CompositionContext, val component: Component) {

        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            val previous = CompositionContext.current
            CompositionContext.current = cc
            try {
                cc.startRoot()
                composable()
                cc.endRoot()
                cc.applyChanges()
            } finally {
                CompositionContext.current = previous
            }
            block(activity)
            return this
        }
    }

    fun then(block: (activity: Activity) -> Unit): ActiveTest {
        val controller = Robolectric.buildActivity(TestActivity::class.java)
        val activity = controller.create().get()
        val root = activity.root
        val component = Root()
        val cc = CompositionContext.create(root.context, root, component, null)
        cc.context = activity
        return ActiveTest(activity, cc, component).then(block)
    }
}

fun compose(composable: () -> Unit) = CompositionTest(composable)