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
package com.android.nfc;

import android.bluetooth.BluetoothProtoEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.nfc.handover.HandoverDataParser;

import java.util.concurrent.Executor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoSession;

@RunWith(AndroidJUnit4.class)
public final class NfcTagOccurredTest {

    private static final String TAG = NfcTagOccurredTest.class.getSimpleName();
    private boolean mNfcSupported;

    private MockitoSession mStaticMockSession;
    private NfcDispatcher mNfcDispatcher;
    private boolean onLocked;

    @Before
    public void setUp() {
        mStaticMockSession = ExtendedMockito.mockitoSession()
                .mockStatic(NfcStatsLog.class)
                .startMocking();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PackageManager pm = context.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC_ANY)) {
            mNfcSupported = false;
            return;
        }
        mNfcSupported = true;

        ScreenStateHelper screenStateHelper = new ScreenStateHelper(context);
        if (screenStateHelper.checkScreenState() == ScreenStateHelper.SCREEN_STATE_ON_LOCKED) {
            onLocked = true;
            return;
        }
        onLocked = false;

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
              () -> mNfcDispatcher = new NfcDispatcher(context, new HandoverDataParser(), false));
        Assert.assertNotNull(mNfcDispatcher);
    }

    @After
    public void tearDown() {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testLogOthers() {
        if (!mNfcSupported || onLocked) return;

        Tag tag = Tag.createMockTag(null, new int[0], new Bundle[0], 0L);
        mNfcDispatcher.dispatchTag(tag);
        ExtendedMockito.verify(() -> NfcStatsLog.write(
                NfcStatsLog.NFC_TAG_OCCURRED,
                NfcStatsLog.NFC_TAG_OCCURRED__TYPE__OTHERS,
                -1,
                tag.getTechCodeList(),
                BluetoothProtoEnums.MAJOR_CLASS_UNCATEGORIZED,
                ""));
    }
}
