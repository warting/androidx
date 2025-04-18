## Implementing compatibility {#compat}

### Referencing new APIs {#compat-newapi}

Generally, methods on library classes should be available to all devices above
the library's `minSdkVersion`; however, the behavior of the method may vary
based on platform API availability.

For example, a method may delegate to a platform API on SDKs where the API is
available, backport a subset of behavior on earlier SDKs, and no-op on very old
SDKs.

#### Checking device SDK version {#compat-sdk}

The most common way of delegating to platform or backport implementations is to
compare the device's `Build.VERSION.SDK_INT` field to a known-good SDK version;
for example, the SDK in which a method first appeared or in which a critical bug
was first fixed.

**Do not** assume that the `SDK_INT` for the next release will be N+1. The value
is not finalized until SDK finalization happens, at which point the `isAtLeast`
check will be updated. **Never** write your own check for a pre-release SDK.

#### Device-specific issues {#compat-oem}

Library code may work around device- or manufacturer-specific issues -- issues
not present in AOSP builds of Android -- *only* if a corresponding CTS test
and/or CDD policy is added to the next revision of the Android platform. Doing
so ensures that such issues can be detected and fixed by OEMs.

#### Handling `minSdkVersion` disparity {#compat-minsdk}

Methods that only need to be accessible on newer devices, including
`to<PlatformClass>()` methods, may be annotated with `@RequiresApi(<sdk>)` to
indicate they must not be called when running on older SDKs. This annotation is
enforced at build time by the `NewApi` lint check.

#### Handling `targetSdkVersion` behavior changes {#compat-targetsdk}

To preserve application functionality, device behavior at a given API level may
change based on an application's `targetSdkVersion`. For example, if an app with
`targetSdkVersion` set to API level 22 runs on a device with API level 29, all
required permissions will be granted at installation time and the run-time
permissions framework will emulate earlier device behavior.

Libraries do not have control over the app's `targetSdkVersion` and -- in rare
cases -- may need to handle variations in platform behavior. Refer to the
following pages for version-specific behavior changes:

*   Android 14,
    [API level 34](https://developer.android.com/about/versions/14/behavior-changes-14)
*   Android 13,
    [API level 33](https://developer.android.com/about/versions/13/behavior-changes-13)
*   Android 12,
    [API level 31](https://developer.android.com/about/versions/12/behavior-changes-12)
*   Android 11,
    [API level 30](https://developer.android.com/about/versions/11/behavior-changes-11)
*   Android 10,
    [API level 29](https://developer.android.com/about/versions/10/behavior-changes-10)
*   Android Pie (9.0),
    [API level 28](https://developer.android.com/about/versions/pie/android-9.0-changes-28)
*   Android Oreo (8.0),
    [API level 26](https://developer.android.com/about/versions/oreo/android-8.0-changes)
*   Android Nougat(7.0),
    [API level 24](https://developer.android.com/about/versions/nougat/android-7.0-changes)
*   Android Lollipop (5.0),
    [API level 21](https://developer.android.com/about/versions/lollipop/android-5.0-changes)
*   Android KitKat (4.4),
    [API level 19](https://developer.android.com/about/versions/kitkat/android-4.4#Behaviors)

#### Working around Lint issues {#compat-lint}

In rare cases, Lint may fail to interpret API usages and yield a `NewApi` error
and require the use of `@TargetApi` or `@SuppressLint('NewApi')` annotations.
Both of these annotations are strongly discouraged and may only be used
temporarily. They **must never** be used in a stable release. Any usage of these
annotation **must** be associated with an active bug, and the usage must be
removed when the bug is resolved.

#### Java 8+ APIs and core library desugaring {#compat-desugar}

The DEX compiler (D8) supports
[API desugaring](https://developer.android.com/studio/write/java8-support-table)
to enable usage of Java 8+ APIs on a broader range of platform API levels.
Libraries using AGP 8.2+ can express the toolchain requirements necessary for
desugaring to work as intended, but these requirements are only enforced for
**apps** that are also building with AGP 8.2+.
[While adoption of AGP 8.2+ remains low](https://issuetracker.google.com/172590889#comment12),
AndroidX libraries **must not** rely on `coreLibraryDesugaring` to access Java
language APIs on earlier platform API levels. For example, `java.time.*` may
only be used in code paths targeting API level 26 and above.

### Delegating to API-specific implementations {#delegating-to-api-specific-implementations}

#### Referencing SDK constants {#sdk-constants}

Generally speaking, platform and Mainline SDK constants should not be inlined.

Constants that can be inlined by the compiler (most primitives and `String`s)
should be referenced directly from the SDK rather than copying and pasting the
value. This will raise an `InlinedApi` lint warning, which may be suppressed.

```
public static class ViewCompat {
  @Suppress("InlinedApi")
  public static final int SOME_CONSTANT = View.SOME_CONSTANT
}
```

In rare cases, some SDK constants are not defined at compile-time and cannot be
inlined by the compiler. In these cases, you will need to handle them like any
other API using out-of-lining and version gating.

```
public static final int RUNTIME_CONSTANT =
    if (SDK_INT > 34) { Api34Impl.RUNTIME_CONSTANT } else { -1 }
```

Developers **must not** inline platform or Mainline SDK constants that are not
part of a finalized public SDK. **Do not** inline values from `@hide` constants
or public constants in an unfinalized SDK.

#### SDK-dependent reflection {#sdk-reflection}

Note: The
[BanUncheckedReflection](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:lint-checks/src/main/java/androidx/build/lint/BanUncheckedReflection.kt)
lint check detects disallowed usages of reflection.

Starting in API level 28, the platform restricts which
[non-SDK interfaces](https://developer.android.com/distribute/best-practices/develop/restrictions-non-sdk-interfaces)
can be accessed via reflection by apps and libraries. As a general rule, you
will **not** be able to use reflection to access hidden APIs on devices with
`SDK_INT` greater than `Build.VERSION_CODES.P` (28).

In cases where a hidden API is a constant value, **do not** inline the value.
Hidden APIs cannot be tested by CTS and carry no stability guarantees.

Per go/platform-parity, on earlier devices or in cases where an API is marked
with `@UnsupportedAppUsage`, reflection on hidden platform APIs is allowed
**only** when an alternative public platform API exists in a later revision of
the Android SDK. For example, the following implementation is allowed:

```java
public AccessibilityDelegate getAccessibilityDelegate(View v) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Retrieve the delegate using a public API.
        return v.getAccessibilityDelegate();
    } else if (Build.VERSION.SDK_INT >= 11) {
        // Retrieve the delegate by reflecting on a private field. If the
        // field does not exist or cannot be accessed, this will no-op.
        if (sAccessibilityDelegateField == null) {
            try {
                sAccessibilityDelegateField = View.class
                        .getDeclaredField("mAccessibilityDelegate");
                sAccessibilityDelegateField.setAccessible(true);
            } catch (Throwable t) {
                sAccessibilityDelegateCheckFailed = true;
                return null;
            }
        }
        try {
            Object o = sAccessibilityDelegateField.get(v);
            if (o instanceof View.AccessibilityDelegate) {
                return (View.AccessibilityDelegate) o;
            }
            return null;
        } catch (Throwable t) {
            sAccessibilityDelegateCheckFailed = true;
            return null;
        }
    } else {
        // There is no way to retrieve the delegate, even via reflection.
        return null;
    }
```

Calls to public APIs added in pre-release revisions *must* be gated using
`BuildCompat`:

```java
if (BuildCompat.isAtLeastQ()) {
   // call new API added in Q
} else if (Build.SDK_INT.VERSION >= 23) {
   // make a best-effort using APIs that we expect to be available
} else {
   // no-op or best-effort given no information
}
```

#### Shadowing platform classes {#sdk-shadowing}

Generally, libraries should **never** create new classes in the `android.*`
namespace or re-define any classes that may be present in the boot classpath.
**Do not** create a library class with the same fully-qualified name as one in
the platform SDK, a Mainline module, sidecar JAR, or another library. Keep all
classes within your own package based on your Maven group ID.

The reverse also applies: the platform SDK, Mainline modules, sidecar JARs, and
other libraries **must not** define classes in the `androidx.*` namespace.

In *extremely limited* cases, the overhead of reflecting on a platform class may
cause performance issues for apps on a scale that warrants using a compile-only
stub of the platform class to avoid reflection. Any instances of this **must**
be approved by Jetpack Working Group before submitting the change.

### Inter-process communication {#ipc}

Protocols and data structures used for IPC must support interoperability between
different versions of libraries and should be treated similarly to public API.

**Do not** design your own serialization mechanism or wire format for disk
storage or inter-process communication. Preserving and verifying compatibility
is difficult and error-prone.

**Do not** expose your serialization mechanism in your API surface. Neither
Stable AIDL nor Protobuf generate stable language APIs.

Generally, any communication prototcol, handshake, etc. must maintain
compatibility consistent with SemVer guidelines. Consider how your protocol will
handle addition and removal of operations or constants, compatibility-breaking
changes, and other modifications without crashing either the host or client
process.

We recommend the following IPC mechanisms, in order of preference:

#### Stable AIDL <a name="ipc-stableaidl"></a>

Stable AIDL is used by the Android platform and AndroidX to provide a
platform-native IPC mechanism with strong inter-process compatibility
guarantees. It supports a subset of standard AIDL.

Use Stable AIDL if your library:

-   Needs to send and receive Android's `Parcelable` data types
-   Communicates directly with the Android platform, System UI, or other AOSP
    components *or* is likely to do so in the future

**Do not** use Stable AIDL to persist data to disk.

##### Using Stable AIDL {#ipc-stableaidl-using}

To add Stable AIDL definitions to your project:

1.  Add the Stable AIDL plugin to `build.gradle`:

    ```
    plugins {
      id("androidx.stableaidl")
    }
    ```

2.  Enable the AIDL build feature and specify an initial version for your Stable
    AIDL interfaces in `build.gradle`:

    ```
    android {
      buildFeatures {
        aidl = true
      }
      buildTypes.all {
        stableAidl {
          version 1
        }
      }
    }
    ```

3.  Migrate existing AIDL files or create new AIDL files under
    `<project>/src/main/stableAidl`

4.  Generate an initial set of Stable AIDL API tracking files by running

    ```
    ./gradlew :path:to:project:updateAidlApi
    ```

##### Annotating unstable AIDL {#ipc-stableaidl-unstable}

Once an API that relies on an IPC contract ships to production in an app, the
contract is locked in and must maintain compatibility to prevent crashing either
end of an inter-process communication channel.

Developers **should** annotate unstable IPC classes with a `@RequiresOptIn`
annotation explaining that they must not be used in production code. Libraries
**must not** opt-in to these annotations when such classes are referenced
internally, and should instead propagate the annotations to public API surfaces.

A single annotation for this purpose may be defined per library or atomic group:

```java
/**
 * Parcelables and AIDL-generated classes bearing this annotation are not
 * guaranteed to be stable and must not be used for inter-process communication
 * in production.
 */
@RequiresOptIn
public @interface UnstableAidlDefinition {}
```

Generally speaking, at this point in time no libraries should have unstable
`Parcelable` classes defined in source code, but for completeness:

```java
@UnstableAidlDefinition
public class ResultReceiver implements Parcelable { ... }
```

AIDL definition files under `src/aidl` should use `@JavaPassthrough` with a
fully-qualified class name to annotate generated classes:

```java
@JavaPassthrough(annotation="@androidx.core.util.UnstableAidlDefinition")
oneway interface IResultReceiver {
    void send(int resultCode, in Bundle resultData);
}
```

For Stable AIDL, the build system enforces per-CL compatibility guarantees. No
annotations are required for Stable AIDL definition files under
`src/stableAidl`.

#### Protobuf <a name="ipc-protobuf"></a>

Protobuf is used by many Google applications and services to provide an IPC and
disk persistence mechanism with strong inter-process compatibility guarantees.

Use Protobuf if your library:

-   Communicates directly with other applications or services already using
    Protobuf
-   Your data structure is complex and likely to change over time - Needs to
    persist data to disk

If your data includes `FileDescriptor`s, `Binder`s, or other platform-defined
`Parcelable` data structures, consider using Stable AIDL instead. Protobuf
cannot directly handle these types, and they will need to be stored alongside
the serialized Protobuf bytes in a `Bundle`.

See [Protobuf](#dependencies-protobuf) for more information on using protocol
buffers in your library.

WARNING While Protobuf is capable of maintaining inter-process compatibility,
AndroidX does not currently provide compatibility tracking or enforcement.
Library owners must perform their own validation.

NOTE We are currently investigating the suitability of Square's
[`wire` library](https://github.com/square/wire) for handling protocol buffers
in Android libraries. If adopted, it will replace `proto` library dependencies.
Libraries that expose their serialization mechanism in their API surface *will
not be able to migrate*.

#### Bundle <a name="ipc-bundle"></a>

`Bundle` is used by the Android platform and AndroidX as a lightweight IPC
mechanism. It has the weakest type safety and compatibility guarantees of any
recommendation, and it has many caveats that make it a poor choice.

In some cases, you may need to use a `Bundle` to wrap another IPC mechanism so
that it can be passed through Android platform APIs, e.g. a `Bundle` that wraps
a `byte[]` representing a serialized Protobuf.

Use `Bundle` if your library:

-   Has a very simple data model that is unlikely to change in the future
-   Needs to send or receive `Binder`s, `FileDescriptor`s, or platform-defined
    `Parcelable`s
    ([example](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:core/core/src/main/java/androidx/core/graphics/drawable/IconCompat.java;l=820))

Caveats for `Bundle` include:

-   When running on Android S and below, accessing *any* entry in a `Bundle`
    will result in the platform attempting to deserialize *every* entry. This
    has been fixed in Android T and later with "lazy" bundles, but developers
    should be careful when accessing `Bundle` on earlier platforms. If a single
    entry cannot be loaded -- for example if a developer added a custom
    `Parcelable` that doesn't exist in the receiver's classpath -- an exception
    will be thrown when accessing *any* entry.
-   On all platforms, library code that receives `Bundle`s data from outside the
    process **must** read the data defensively. See previous note regarding
    additional concerns for Android S and below.
-   On all platforms, library code that sends `Bundle`s outside the process
    *should* discourage clients from passing custom `Parcelable`s.
-   `Bundle` provides no versioning and Jetpack provides no affordances for
    tracking the keys or value types associated with a `Bundle`. Library owners
    are responsible for providing their own system for guaranteeing wire format
    compatibility between versions.

#### Versioned Parcelable <a name="ipc-versionedparcelable"></a>

`VersionedParcelable` is a deprecated library that was intended to provide
compatibility guarantees around the Android platform's `Parcelable` class;
however, the initial version contained bugs and it was not actively maintained.

Use `VersionedParcelable` if your library:

-   Is already using `VersionedParcelable` and you are aware of its
    compatibility constraints

**Do not** use `VersionedParcelable` in all other cases.

#### Wire <a name="ipc-wire"></a>

We are currently evaluating Square's [Wire](https://github.com/square/wire) as a
front-end to Protobuf. If this library meets your team's needs based on your own
research, feel free to use it.

#### gRPC <a name="ipc-grpc"></a>

Some clients have requested to use Google's [gRPC](https://grpc.io/) library to
align with other Google products. It's okay to use gRPC for network
communication or communication with libraries and services outside of AndroidX
that are already using gRPC.

**Do not** use gRPC to communicate between AndroidX libraries or with the
Android platform.

#### Parcelable <a name="ipc-parcelable"></a>

**Do not** implement `Parcelable` for any class that may be used for IPC or
otherwise exposed as public API. By default, `Parcelable` does not provide any
compatibility guarantees and will result in crashes if fields are added or
removed between library versions. If you are using Stable AIDL, you *may* use
AIDL-defined parcelables for IPC but not public API.
