package com.google.r4a.examples.explorerapp.ui.screens

import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import com.google.r4a.Component
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.R
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.text.method.PasswordTransformationMethod
import com.google.r4a.CompositionContext
import com.google.r4a.examples.explorerapp.common.data.AuthenticationService
import com.google.r4a.examples.explorerapp.common.data.RedditRepository
import com.google.r4a.examples.explorerapp.common.data.RedditUser


class SignupScreen : Component() {
    private val authentication get() = CompositionContext.getAmbient(AuthenticationService.Ambient, this)
    private val navigator get() = CompositionContext.getAmbient(Ambients.NavController, this)

    private var username: String = ""
    private var password: String = ""
    private var isLoading: Boolean = false

    fun onSubmit() {
        authentication.signup(username, password, object : Function2<Throwable?, RedditUser?, Unit> {
            override fun invoke(p1: Throwable?, p2: RedditUser?) {
                // TODO(lmr): add errors here, or navigate away
            }
        })
        isLoading = true
        recompose()
    }

    override fun compose() {
//        val username = username
//        val password = password
        val buttonEnabled = username.length > 0 && password.length > 0
        <ScrollView
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
        >
            <LinearLayout
                layoutWidth=MATCH_PARENT
                layoutHeight=MATCH_PARENT
                padding=24.dp
                orientation=LinearLayout.VERTICAL
            >
                <ImageView
                    layoutWidth=160.dp
                    layoutHeight=160.dp
                    layoutGravity=Gravity.CENTER_HORIZONTAL
                    marginBottom=24.dp
                    imageResource=R.drawable.reddit_verticallockup_onwhite
                />
                <TextInputLayout
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutGravity=Gravity.CENTER_HORIZONTAL
                >
                    <EditText
                        paddingHorizontal=16.dp
                        paddingVertical=16.dp
                        textSize=6.sp
                        hint="Username"
                        controlledText=username
                        onTextChange={
                            username = it
                            recomposeSync()
                        }
                        singleLine=true
                        imeOptions=EditorInfo.IME_ACTION_NEXT
                    />
                </TextInputLayout>
                <TextInputLayout
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutGravity=Gravity.CENTER_HORIZONTAL
                    passwordVisibilityToggleEnabled=true
                >
                    <TextInputEditText
                        paddingHorizontal=16.dp
                        paddingVertical=16.dp
                        textSize=6.sp
                        hint="Password"
                        controlledText=password
                        onTextChange={
                            password = it
                            recomposeSync()
                        }
//                        transformationMethod={PasswordTransformationMethod.getInstance()}
                        singleLine=true
                        imeOptions=EditorInfo.IME_ACTION_DONE
                        inputType=InputType.TYPE_TEXT_VARIATION_PASSWORD
                        onEditorAction={ _, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                onSubmit()
                            }
                            false
                        }
                    />
                </TextInputLayout>
                <Button
                    text="Sign Up"
                    textSize=6.sp
                    marginBottom=10.dp
                    backgroundColor=Colors.PRIMARY
                    textColor=Colors.TEXT_LIGHT
                    enabled=buttonEnabled
                    onClick={ _ ->
                        onSubmit()
                    }
                />
                <TextView
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutGravity=Gravity.CENTER_HORIZONTAL
                    padding=10.dp
                    textAlignment=TextView.TEXT_ALIGNMENT_CENTER
                    text="Already a member? Login."
                    onClick={ _ ->
                        navigator.navigate(R.id.nav_to_login)
                    }
                    textSize=6.sp
                    textColor=Colors.TEXT_MUTED
                />
                <TextView
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutGravity=Gravity.CENTER_HORIZONTAL
                    padding=10.dp
                    textAlignment=TextView.TEXT_ALIGNMENT_CENTER
                    text="Use app without logging in."
                    onClick={ _ ->
                        navigator.navigate(R.id.screen_link_list)
                    }
                    textSize=6.sp
                    textColor=Colors.TEXT_MUTED
                />
            </LinearLayout>
        </ScrollView>
    }
}