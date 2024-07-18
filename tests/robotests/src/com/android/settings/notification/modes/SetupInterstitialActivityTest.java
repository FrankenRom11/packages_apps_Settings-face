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

package com.android.settings.notification.modes;

import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.test.core.app.ActivityScenario;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SetupInterstitialActivityTest {
    private static final String MODE_ID = "modeId";

    @Mock
    private ZenModesBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // set global backend instance so that when the interstitial activity launches, it'll get
        // this mock backend
        ZenModesBackend.setInstance(mBackend);
    }

    @Test
    public void enableButton_enablesModeAndRedirectsToModePage() {
        ZenMode mode = new TestModeBuilder().setId(MODE_ID).setEnabled(false).build();
        when(mBackend.getMode(MODE_ID)).thenReturn(mode);

        // Set up scenario with this mode information
        ActivityScenario<SetupInterstitialActivity> scenario =
                ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                        .setClass(RuntimeEnvironment.getApplication(),
                                SetupInterstitialActivity.class)
                        .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, MODE_ID));
        scenario.onActivity(activity -> {
            View.OnClickListener listener = activity.enableButtonListener(MODE_ID);

            // simulate button press even though we don't actually have a button
            listener.onClick(null);

            // verify that the backend got a request to enable the mode
            ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
            verify(mBackend).updateMode(captor.capture());
            ZenMode updatedMode = captor.getValue();
            assertThat(updatedMode.getId()).isEqualTo(MODE_ID);
            assertThat(updatedMode.isEnabled()).isTrue();

            // confirm that the next activity is the mode page
            Intent openModePageIntent = shadowOf(activity).getNextStartedActivity();
            assertThat(openModePageIntent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                    .isEqualTo(ZenModeFragment.class.getName());
            Bundle fragmentArgs = openModePageIntent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            assertThat(fragmentArgs).isNotNull();
            assertThat(fragmentArgs.getString(EXTRA_AUTOMATIC_ZEN_RULE_ID)).isEqualTo(MODE_ID);
        });
        scenario.close();
    }
}
