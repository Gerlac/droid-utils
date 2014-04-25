/*
 * Copyright 2013 Luluvise Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.luluvise.droid_utils.lib;

import java.io.File;
import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.github.luluvise.droid_utils.logging.LogUtils;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Helper class containing static utility methods for handling bitmaps.<br>
 * 
 * Documentation references:
 * 
 * <pre>
 * <ul>
 * <li>{@link http://developer.android.com/reference/android/graphics/Bitmap.html}</li>
 * <li>{@link http://developer.android.com/reference/android/graphics/BitmapFactory.html}</li>
 * <li>{@link http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html}</li>
 * <li>{@link http://developer.android.com/training/displaying-bitmaps/index.html}</li>
 * </ul>
 * </pre>
 * 
 * @since 1.0
 * @author Marco Salis
 */
@Beta
public class BitmapUtils {

	public static final int MAX_WIDTH = 1280;
	public static final int MAX_HEIGHT = 1280;

	private static final String TAG = BitmapUtils.class.getSimpleName();

	private BitmapUtils() {
		// hidden constructor, no instantiation needed
	}

	/**
	 * Calculates the expected memory byte occupation of a bitmap with the given
	 * width and height. Use {@link BitmapUtils#getSize(Bitmap)} if you hold a
	 * reference of the Bitmap to get a more accurate measurement.
	 * 
	 * The current implementation just calculates {@code width * height * 4}
	 * 
	 * @param width
	 *            The width of the image
	 * @param height
	 *            The height of the image
	 * @return The actual size in bytes
	 */
	public static int getSize(int width, int height) {
		return width * height * 4;
	}

	/**
	 * Calculates the byte occupation of the passed Bitmap
	 * 
	 * @param bitmap
	 * @return The actual size in bytes
	 */
	public static int getSize(Bitmap bitmap) {
		// getBytesCount() on API 12 does exactly the same
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	/**
	 * Gets the device's default directory for storing pictures
	 * 
	 * @param context
	 *            A {@link Context} to retrieve the folder
	 * @return The File object or null if something went wrong
	 */
	public static File getPublicPicturesDir(@Nonnull Context context) {
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		if (!dir.exists()) {
			if (!dir.mkdirs()) { // fallback to the temporary directory
				dir = DroidUtils.getTempFolder(context);
			}
		}
		return dir;
	}

	/**
	 * Calculates the inSampleSize option from {@link BitmapFactory.Options()}
	 * for decoding a Bitmap which is big enough to fit the required passed
	 * size.
	 * 
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return The calculated inSampleSize
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
			int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}

		// check if the number is <=3 or a power of two
		if (inSampleSize > 3 && (inSampleSize & -inSampleSize) != inSampleSize) {
			// calculate next lower power of two from the number
			inSampleSize = getNextLowerTwoPow(inSampleSize);
		}

		return inSampleSize;
	}

	/**
	 * Calculates the inSampleSize option from {@link BitmapFactory.Options()}
	 * for decoding a Bitmap whose maximum side is always less the passed pixel
	 * value.
	 * 
	 * This is useful when we're dealing with very big bitmaps and we don't want
	 * in any case them to be bigger than the specified value. The inSampleSize
	 * calculated is not necessarily a power of two and can result in an image a
	 * lot smaller than the size we wanted.
	 * 
	 * @param options
	 *            The {@link BitmapFactory.Options()} to use
	 * @param maxSide
	 *            The max size, in pixel, the resulting Bitmap must have
	 * @return The calculated inSampleSize
	 */
	public static int calculateMaxInSampleSize(BitmapFactory.Options options, int maxSide) {
		// raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > maxSide || width > maxSide) {
			if (width > height) {
				inSampleSize = (int) Math.ceil(width / (double) maxSide);
			} else {
				inSampleSize = (int) Math.ceil(height / (double) maxSide);
			}
		}

		return inSampleSize;
	}

	/**
	 * Calculates the next lower power of 2 of a given positive number.
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code number < 0}
	 */
	public static int getNextLowerTwoPow(int number) {
		Preconditions.checkArgument(number >= 0);
		return (int) Math.pow(2, Math.floor(Math.log(number) / Math.log(2)));
	}

	/**
	 * Decodes a Bitmap from resources which is big enough to fit the required
	 * passed size.
	 * 
	 * @param res
	 *            The {@link Resources} to get the drawable from
	 * @param resId
	 *            The drawable resource ID
	 * @param reqWidth
	 * @param reqHeight
	 * @return The decoded Bitmap or null if something went wrong
	 */
	@CheckForNull
	public static Bitmap decodeSampledBitmapFromResource(@Nonnull Resources res, int resId,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds = true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	private static final ImmutableList<String> MEDIA_COLUMNS = ImmutableList.of(
			MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION);

	/**
	 * Returns the available columns for the media content provider for images.
	 * 
	 * Only available from API >= 16:<br>
	 * MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT
	 */
	@Nonnull
	public static final String[] getImagesMediaColumns() {
		return MEDIA_COLUMNS.toArray(new String[MEDIA_COLUMNS.size()]);
	}

	/**
	 * Loads a Bitmap from an URI using the passed ContentResolver, which is
	 * scaled to match at least the required width and height.
	 * 
	 * @param cr
	 *            The {@link ContentResolver} to use
	 * @param picUri
	 *            The picture Uri
	 * @param reqWidth
	 *            The minimum required width
	 * @param reqHeight
	 *            The minimum required height
	 * @return The Bitmap, or null if the URI didn't match any resource
	 */
	@CheckForNull
	public static Bitmap loadBitmapFromUri(ContentResolver cr, Uri picUri, int reqWidth,
			int reqHeight) {
		// TODO: handle picture orientation
		final String[] mediaColumns = getImagesMediaColumns();
		Cursor cursor = cr.query(picUri, mediaColumns, null, null, null);
		if (cursor != null && cursor.moveToFirst()) { // we've found the image
			String picturePath = cursor.getString(cursor.getColumnIndex(mediaColumns[0]));
			cursor.close();
			return loadBitmapFromPath(picturePath, reqWidth, reqHeight);
		} else {
			return null;
		}
	}

	/**
	 * See {@link #loadBitmapFromPath(String, int, int, boolean)}
	 * 
	 * Rotate flag set to true by default
	 */
	@CheckForNull
	public static Bitmap loadBitmapFromPath(String picturePath, int reqWidth, int reqHeight) {
		return loadBitmapFromPath(picturePath, reqWidth, reqHeight, true);
	}

	/**
	 * Loads an immutable Bitmap object from the given path
	 * 
	 * @param picturePath
	 * @param reqWidth
	 *            The minimum required width
	 * @param reqHeight
	 *            The minimum required height
	 * @param rotate
	 *            true to check for the EXIF orientation tag (if existing) and
	 *            rotate the Bitmap accordingly if necessary, false to leave the
	 *            default orientation
	 * @return The Bitmap, or null if the path wasn't valid
	 */
	@CheckForNull
	public static Bitmap loadBitmapFromPath(String picturePath, int reqWidth, int reqHeight,
			boolean rotate) {
		int rotateValue = 0;
		if (rotate) { // check for orientation
			int orientation = getExifOrientation(picturePath);
			LogUtils.log(Log.VERBOSE, TAG, "Image orientation: " + orientation);

			rotateValue = getRotationFromOrientation(orientation);
			// invert width and height if the image flips
			if (rotateValue == 90 || rotateValue == 270) {
				int oldW = reqWidth;
				reqWidth = reqHeight;
				reqHeight = oldW;
			}
		}
		// First decode with inJustDecodeBounds = true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(picturePath, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		// actually decode the sampled bitmap
		options.inJustDecodeBounds = false;

		Bitmap bitmap;

		if (rotateValue != 0 && Build.VERSION.SDK_INT >= 11) { // HONEYCOMB
			// we can rotate the mutable bitmap in-place without copying it
			bitmap = decodeMutableBitmap(picturePath, options);
			Canvas canvas = new Canvas(bitmap);
			canvas.rotate(rotateValue);
		} else { // pre-HONEYCOMB or no rotation needed
			bitmap = BitmapFactory.decodeFile(picturePath, options);
			if (rotateValue != 0) {
				// TODO: find a less memory-consuming way to do this?
				Matrix matrix = new Matrix();
				matrix.setRotate(rotateValue);
				// matrix.postRotate(rotateValue);
				Bitmap originalBitmap = bitmap;
				bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, bitmap.getWidth(),
						bitmap.getHeight(), matrix, false);
				originalBitmap.recycle();
				LogUtils.log(Log.INFO, TAG, "loadBitmapFromPath: recycling bitmap");
			}
		}
		return bitmap;
	}

	@TargetApi(11)
	private static Bitmap decodeMutableBitmap(String picturePath, BitmapFactory.Options options) {
		options.inMutable = true;
		return BitmapFactory.decodeFile(picturePath, options);
	}

	/**
	 * Gets the orientation of a JPEG file with EXIF attributes
	 * 
	 * @param path
	 *            The path of the JPEG to analyse
	 * @return The orientation constant, see {@link ExifInterface}
	 * @throws IOException
	 */
	public static int getExifOrientation(String path) {
		ExifInterface exif = null;
		final int defOrientation = ExifInterface.ORIENTATION_NORMAL;
		try {
			exif = new ExifInterface(path);
		} catch (IOException e) {
			LogUtils.logException(e);
			return defOrientation;
		}
		return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, defOrientation);
	}

	/**
	 * Gets the rotation to apply to a picture given its EXIF orientation tag
	 */
	public static int getRotationFromOrientation(int orientation) {
		int rotateValue = 0;
		switch (orientation) {
		// sums up rotation value
		case ExifInterface.ORIENTATION_ROTATE_270:
			rotateValue += 90;
		case ExifInterface.ORIENTATION_ROTATE_180:
			rotateValue += 90;
		case ExifInterface.ORIENTATION_ROTATE_90:
			rotateValue += 90;
			break;
		}
		return rotateValue;
	}

	/**
	 * Loads and crops an image to be used as a squared picture, mostly to be
	 * used as a "profile" picture of some sort: if the image is in portrait
	 * format, it is cropped from its bottom. If the image is in landscape
	 * format, the image is cropped to its center to make it squared.
	 * 
	 * @param url
	 *            The path where to retrieve the image (must be not null)
	 * @param maxSide
	 *            The maximum required image side (the cropped resulting image
	 *            may be smaller)
	 * @return The cropped {@link Bitmap} or null if an error occurred
	 */
	@CheckForNull
	public static Bitmap cropProfileBitmap(@Nonnull String picturePath, int maxSide) {
		Preconditions.checkNotNull(picturePath);
		Preconditions.checkArgument(maxSide > 0);

		int rotateValue = 0;
		// check for orientation
		int orientation = getExifOrientation(picturePath);
		rotateValue = getRotationFromOrientation(orientation);

		// First decode with inJustDecodeBounds = true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(picturePath, options);

		// Calculate inSampleSize "memory-defensively"
		options.inSampleSize = calculateMaxInSampleSize(options, maxSide);
		// actually decode the sampled bitmap
		options.inJustDecodeBounds = false;

		Bitmap bitmap = BitmapFactory.decodeFile(picturePath, options);
		if (bitmap == null) {
			return null;
		}
		// do the actual cropping
		return cropSquaredBitmap(bitmap, rotateValue);
	}

	/**
	 * Crops the passed bitmap to be used as a squared "profile" picture. The
	 * same {@link Bitmap} is returned if already squared.
	 * 
	 * See {@link #cropProfileBitmap(String, int)}
	 * 
	 * @param bitmap
	 *            The bitmap to crop (must already be resized if necessary)
	 * @return The cropped {@link Bitmap} or null if an error occurred
	 */
	@CheckForNull
	public static Bitmap cropProfileBitmap(@Nonnull Bitmap bitmap) {
		if (bitmap.getWidth() == bitmap.getHeight()) {
			return bitmap; // already squared, just return
		} else {
			return cropSquaredBitmap(bitmap, 0);
		}
	}

	/**
	 * Crops the passed bitmap in the center to make it squared.
	 * 
	 * @param bitmap
	 *            The bitmap to crop (must already be resized if necessary)
	 * @param rotateValue
	 *            The rotate value for the {@link Matrix} if needed
	 * @return
	 */
	@CheckForNull
	private static Bitmap cropSquaredBitmap(Bitmap bitmap, int rotateValue) {
		// cropping calculations
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		boolean portrait = height > width;
		int squaredSize, cropOffset, vertOffset = 0;
		if (portrait) { // crop from the bottom of the picture
			squaredSize = width;
			cropOffset = 0;
			vertOffset = 1; // using 0 here doesn't work
		} else { // landscape: crop to the center
			squaredSize = height;
			cropOffset = (width - squaredSize) / 2;
		}

		Bitmap originalBitmap = bitmap;
		if (rotateValue == 0 && cropOffset == 0 && width == height) {
			// we don't need to do anything, return the original image
			return originalBitmap;
		} else {
			Matrix matrix = new Matrix();
			matrix.setRotate(rotateValue);
			Bitmap cropped = Bitmap.createBitmap(originalBitmap, cropOffset, vertOffset,
					squaredSize, squaredSize, matrix, true);
			return cropped;
		}
	}

	/**
	 * Fast method to blur an image.
	 * 
	 * Taken from
	 * http://stackoverflow.com/questions/2067955/fast-bitmap-blur-for
	 * -android-sdk
	 * 
	 * TODO Use ScriptIntrinsicBlur to create a blurry image in android devices
	 * API>17.
	 * 
	 */
	public static Bitmap fastblur(@Nonnull Bitmap sentBitmap, int radius) {

		// Stack Blur v1.0 from
		// http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
		//
		// Java Author: Mario Klingemann <mario at quasimondo.com>
		// http://incubator.quasimondo.com
		// created Feburary 29, 2004
		// Android port : Yahel Bouaziz <yahel at kayenko.com>
		// http://www.kayenko.com
		// ported april 5th, 2012

		// This is a compromise between Gaussian Blur and Box blur
		// It creates much better looking blurs than Box Blur, but is
		// 7x faster than my Gaussian Blur implementation.
		//
		// I called it Stack Blur because this describes best how this
		// filter works internally: it creates a kind of moving stack
		// of colors whilst scanning through the image. Thereby it
		// just has to add one new block of color to the right side
		// of the stack and remove the leftmost color. The remaining
		// colors on the topmost layer of the stack are either added on
		// or reduced by one, depending on if they are on the right or
		// on the left side of the stack.
		//
		// If you are using this algorithm in your code please add
		// the following line:
		//
		// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

		Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

		if (radius < 1) {
			return (null);
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int[] pix = new int[w * h];
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);

		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];

		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}

		yw = yi = 0;

		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);
				rbs = r1 - Math.abs(i);
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];

				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;

				sir = stack[i + radius];

				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				rbs = r1 - Math.abs(i);

				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];

				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi += w;
			}
		}

		bitmap.setPixels(pix, 0, w, 0, 0, w, h);
		return (bitmap);
	}

}