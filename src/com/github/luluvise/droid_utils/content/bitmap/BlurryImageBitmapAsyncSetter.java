package com.github.luluvise.droid_utils.content.bitmap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.github.luluvise.droid_utils.cache.keys.CacheUrlKey;
import com.github.luluvise.droid_utils.lib.BitmapUtils;

/**
 * 
 * Extension of {@link BitmapAnimatedAsyncSetter} that calculates a blurry
 * bitmap and sets it to the provided {@link ImageView} when this is loaded from
 * the disk or network using an animation.
 * 
 * @since 2.4
 * @author Gerlac Farrus
 */
public class BlurryImageBitmapAsyncSetter extends BitmapAnimatedAsyncSetter {

	@SuppressWarnings("unused")
	private static final String TAG = BitmapAnimatedAsyncSetter.class.getSimpleName();

	public static final int BLUR_RADIUS = 110;

	/* Constructors from superclass */

	public BlurryImageBitmapAsyncSetter(@Nonnull ImageView imgView) {
		super(imgView);
	}

	public BlurryImageBitmapAsyncSetter(@Nonnull ImageView imgView,
			@Nullable OnBitmapImageSetListener listener) {
		super(imgView, listener);
	}

	/**
	 * @see BitmapAsyncSetter#BitmapAsyncSetter(ImageView,
	 *      OnBitmapImageSetListener)
	 * 
	 * @param mode
	 *            The {@link AnimationMode} to use
	 * @param customAnimationId
	 *            The ID of a custom animation to load, or -1 to use the default
	 *            Android fade-in animation.
	 */
	public BlurryImageBitmapAsyncSetter(@Nonnull ImageView imgView, @Nonnull AnimationMode mode,
			@Nullable OnBitmapImageSetListener listener, int customAnimationId) {
		super(imgView, mode, listener, customAnimationId);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void setBitmapSync(@Nonnull CacheUrlKey key, @Nonnull Bitmap bitmap) {

		// starts task to get a blurry image
		final GetBlurImageTask task = new GetBlurImageTask(bitmap, null, key, null, true);
		task.execute();
	}

	@Override
	protected void setImageBitmap(@Nonnull ImageView imageView, @Nonnull Bitmap bitmap,
			@Nonnull BitmapSource source) {

		// starts task to get a blurry image
		final GetBlurImageTask task = new GetBlurImageTask(bitmap, source, null, imageView, false);
		task.execute();
	}

	/**
	 * AsyncTask that applies a blur effect to bitmap and sets it to the
	 * {@link ImageView}.
	 * 
	 */
	private class GetBlurImageTask extends AsyncTask<Void, Void, Bitmap> {

		private final Bitmap mOriginalBitmap;
		private final BitmapSource mBitmapSource;
		private final ImageView mImageView;
		private final CacheUrlKey mCacheUrlKey;
		private final boolean mIsImageSync;

		public GetBlurImageTask(@Nonnull Bitmap bitmap, @Nonnull BitmapSource source,
				@Nonnull CacheUrlKey key, @Nullable ImageView imageView, boolean isImageSync) {
			mOriginalBitmap = bitmap;
			mBitmapSource = source;
			mIsImageSync = isImageSync;
			mCacheUrlKey = key;
			mImageView = imageView;
		}

		@Override
		protected Bitmap doInBackground(@Nullable Void... params) {
			return BitmapUtils.fastblur(mOriginalBitmap, BLUR_RADIUS);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (mIsImageSync) {
				BlurryImageBitmapAsyncSetter.super.setBitmapSync(mCacheUrlKey, result);
			} else {
				if (mImageView != null) {
					BlurryImageBitmapAsyncSetter.super.setImageBitmap(mImageView, result,
							mBitmapSource);
				}
			}
		}
	}

}
