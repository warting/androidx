/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ClickAndWaitTestActivity extends TestActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.click_and_wait_test_activity);
    }

    public void launchNewWindow(@NonNull View v) {
        ((Button) v).append("_clicked");
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(getApplicationContext(),
                    ClickAndWaitTestActivity.class);
            startActivity(intent);
        }, 2_000);
    }
}
