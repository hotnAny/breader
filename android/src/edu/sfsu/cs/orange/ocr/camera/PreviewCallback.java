/*
 * Copyright (C) 2010 ZXing authors
 * Copyright 2011 Robert Theis
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

package edu.sfsu.cs.orange.ocr.camera;

import java.io.ByteArrayOutputStream;
import java.nio.IntBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Called when the next preview frame is received.
 * 
 * The code for this class was adapted from the ZXing project:
 * http://code.google.com/p/zxing
 */
final class PreviewCallback implements Camera.PreviewCallback {

	private static final String TAG = PreviewCallback.class.getSimpleName();

	private final CameraConfigurationManager configManager;
	private Handler previewHandler;
	private int previewMessage;

	PreviewCallback(CameraConfigurationManager configManager) {
		this.configManager = configManager;
	}

	void setHandler(Handler previewHandler, int previewMessage) {
		this.previewHandler = previewHandler;
		this.previewMessage = previewMessage;
	}

	// Since we're not calling setPreviewFormat(int), the data arrives here in
	// the YCbCr_420_SP
	// (NV21) format.
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Point cameraResolution = configManager.getCameraResolution();
		Handler thePreviewHandler = previewHandler;
		if (cameraResolution != null && thePreviewHandler != null) {
			Message message = thePreviewHandler.obtainMessage(
					previewMessage,
					cameraResolution.x,
					cameraResolution.y,
					data
//					rotate(data, cameraResolution.x,
//							cameraResolution.y)
					);
			message.sendToTarget();
			previewHandler = null;
		} else {
			Log.d(TAG,
					"Got preview callback, but no handler or resolution available");
		}
	}
	
	byte[] rotate(byte[] data, int imageWidth, int imageHeight) {
		Bitmap bmp = YUV2Bitmap(data, imageWidth, imageHeight);
		Bitmap bmpRotated = rotateImage(90, bmp);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmpRotated.compress(Bitmap.CompressFormat.PNG, 100, stream);
		return stream.toByteArray();
	}

	Bitmap YUV2Bitmap(byte[] data, int imageWidth, int imageHeight) {
		// the bitmap we want to fill with the image
		Bitmap bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
		int numPixels = imageWidth*imageHeight;
		
		// the buffer we fill up which we then fill the bitmap with
		IntBuffer intBuffer = IntBuffer.allocate(imageWidth*imageHeight);
		// If you're reusing a buffer, next line imperative to refill from the start,
		// if not good practice
		intBuffer.position(0);

		// Set the alpha for the image: 0 is transparent, 255 fully opaque
		final byte alpha = (byte) 255;

		// Get each pixel, one at a time
		for (int y = 0; y < imageHeight; y++) {
		    for (int x = 0; x < imageWidth; x++) {
		        // Get the Y value, stored in the first block of data
		        // The logical "AND 0xff" is needed to deal with the signed issue
		        int Y = data[y*imageWidth + x] & 0xff;

		        // Get U and V values, stored after Y values, one per 2x2 block
		        // of pixels, interleaved. Prepare them as floats with correct range
		        // ready for calculation later.
		        int xby2 = x/2;
		        int yby2 = y/2;

		        // make this V for NV12/420SP
		        float U = (float)(data[numPixels + 2*xby2 + yby2*imageWidth] & 0xff) - 128.0f;

		        // make this U for NV12/420SP
		        float V = (float)(data[numPixels + 2*xby2 + 1 + yby2*imageWidth] & 0xff) - 128.0f;

		        // Do the YUV -> RGB conversion
		        float Yf = 1.164f*((float)Y) - 16.0f;
		        int R = (int)(Yf + 1.596f*V);
		        int G = (int)(Yf - 0.813f*V - 0.391f*U);
		        int B = (int)(Yf            + 2.018f*U);

		        // Clip rgb values to 0-255
		        R = R < 0 ? 0 : R > 255 ? 255 : R;
		        G = G < 0 ? 0 : G > 255 ? 255 : G;
		        B = B < 0 ? 0 : B > 255 ? 255 : B;

		        // Put that pixel in the buffer
		        intBuffer.put(alpha*16777216 + R*65536 + G*256 + B);
		    }
		}

		// Get buffer ready to be read
		intBuffer.flip();

		// Push the pixel information from the buffer onto the bitmap.
		bitmap.copyPixelsFromBuffer(intBuffer);
		
		return bitmap;
	}
	
	public static Bitmap rotateImage(int angle, Bitmap bitmapSrc) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(bitmapSrc, 0, 0, bitmapSrc.getWidth(),
				bitmapSrc.getHeight(), matrix, true);
	}

	private byte[] rotateYUV420Degree90(byte[] data, int imageWidth,
			int imageHeight) {
		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
		// Rotate the Y luma
		int i = 0;
		for (int x = 0; x < imageWidth; x++) {
			for (int y = imageHeight - 1; y >= 0; y--) {
				yuv[i] = data[y * imageWidth + x];
				i++;
			}
		}
		// Rotate the U and V color components
		i = imageWidth * imageHeight * 3 / 2 - 1;
		for (int x = imageWidth - 1; x > 0; x = x - 2) {
			for (int y = 0; y < imageHeight / 2; y++) {
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
				i--;
				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
						+ (x - 1)];
				i--;
			}
		}
		return yuv;
	}
}
