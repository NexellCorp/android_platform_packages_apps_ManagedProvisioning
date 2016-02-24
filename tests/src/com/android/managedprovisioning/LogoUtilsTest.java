/*
 * Copyright 2016, The Android Open Source Project
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

package com.android.managedprovisioning;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.Exception;

@SmallTest
public class LogoUtilsTest extends AndroidTestCase {

    private static final int SAMPLE_COLOR = Color.RED;

    public void testPartiallyResizedBitmap() throws Exception {
        Bitmap bitmap = createSampleBitmap(20, 30);
        File tempFile = writeBitmapToTempFile(bitmap);
        try {
            Bitmap newBitmap = LogoUtils.getBitmapPartiallyResized(tempFile.getPath(), 10, 15);
            // It should have been able to get only the half bitmap.
            assertEquals(10, newBitmap.getWidth());
            assertEquals(15, newBitmap.getHeight());
            assertEquals(SAMPLE_COLOR, newBitmap.getPixel(5, 5));
        } finally {
            tempFile.delete();
        }
    }

    public void testPartiallyResizedElongatedBitmap() throws Exception {
        Bitmap bitmap = createSampleBitmap(8, 32);
        File tempFile = writeBitmapToTempFile(bitmap);
        try {
            Bitmap newBitmap = LogoUtils.getBitmapPartiallyResized(tempFile.getPath(), 8, 8);
            // It should have been able to get only the quarter bitmap.
            assertEquals(2, newBitmap.getWidth());
            assertEquals(8, newBitmap.getHeight());
            assertEquals(SAMPLE_COLOR, newBitmap.getPixel(1, 1));
        } finally {
            tempFile.delete();
        }
    }

    public void testResizeBitmapKeepRatio() throws Exception {
        Bitmap bitmap = createSampleBitmap(16, 32);
        Bitmap newBitmap = LogoUtils.resizeBitmap(bitmap, 8, 8);
        // Should have kept the ratio.
        assertEquals(4, newBitmap.getWidth());
        assertEquals(8, newBitmap.getHeight());
        assertEquals(SAMPLE_COLOR, newBitmap.getPixel(2, 2));
    }

    public void testResizeBitmapNoScalingNeeded() throws Exception {
        Bitmap bitmap = createSampleBitmap(16, 32);
        Bitmap newBitmap = LogoUtils.resizeBitmap(bitmap, 20, 40);
        // The maximum dimensions are larger than the actual ones: should
        // not have changed the dimensions.
        assertEquals(16, newBitmap.getWidth());
        assertEquals(32, newBitmap.getHeight());
        assertEquals(SAMPLE_COLOR, newBitmap.getPixel(2, 2));
    }

    public void testResizeBitmapNoIntegerRatio() throws Exception {
        Bitmap bitmap = createSampleBitmap(15, 15);
        Bitmap newBitmap = LogoUtils.resizeBitmap(bitmap, 10, 10);
        assertEquals(10, newBitmap.getWidth());
        assertEquals(10, newBitmap.getHeight());
        assertEquals(SAMPLE_COLOR, newBitmap.getPixel(2, 2));
    }

    public void testSaveGetOrganisationLogo() throws Exception {
        Bitmap bitmap = createSampleBitmap(7, 5);
        File tempFile = writeBitmapToTempFile(bitmap);
        try {
            LogoUtils.saveOrganisationLogo(getContext(), Uri.fromFile(tempFile));
            Drawable drawable = LogoUtils.getOrganisationLogo(getContext());
            // We should have the original drawable.
            assertEquals(7, drawable.getIntrinsicWidth());
            assertEquals(5, drawable.getIntrinsicHeight());
        } finally {
            LogoUtils.cleanUp(getContext());
        }
    }

    public void testDontSaveOrganisationLogo() throws Exception {
        Drawable logo = LogoUtils.getOrganisationLogo(getContext());
        // We didn't save the organisation logo: it should be the default one.
        Drawable expected = getContext().getResources().getDrawable(R.drawable.briefcase_icon);
        assertEquals(logo.getIntrinsicWidth(), expected.getIntrinsicWidth());
        assertEquals(logo.getIntrinsicHeight(), expected.getIntrinsicHeight());
    }

    private Bitmap createSampleBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(SAMPLE_COLOR);
        canvas.drawRect(0, 0, width, height, paint);
        return bitmap;
    }

    // Returns the temporary file where the bitmap was written.
    private File writeBitmapToTempFile(Bitmap bitmap) throws Exception {
        File tempFile = File.createTempFile("temp_bitmap", "", getContext().getCacheDir());
        FileOutputStream fos = new FileOutputStream(tempFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
        fos.close();
        return tempFile;
    }
}
