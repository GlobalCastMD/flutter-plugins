package io.flutter.plugins.videoplayer;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import org.jetbrains.annotations.NotNull;

public class MediaDescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

    private final Context context;
    private final VideoMetadata metadata;

    /**
     * Creates a default {@link PlayerNotificationManager.MediaDescriptionAdapter}.
     *
     * @param context The {@link Context} to be passed to Glide for loading a remote thumbnail.
     * @param metadata The {@link VideoMetadata} that holds the information to display
     *                 in the media notification
     */
    public MediaDescriptionAdapter(@NotNull Context context, @NotNull VideoMetadata metadata) {
        this.context = context;
        this.metadata = metadata;
    }

    @Override
    public CharSequence getCurrentContentTitle(Player player) {
        return metadata.getTitle();
    }

    @Nullable
    @Override
    public PendingIntent createCurrentContentIntent(Player player) {
        // TODO is this needed to open the app from tapping the notification?
        return null;
    }

    @Nullable
    @Override
    public CharSequence getCurrentContentText(Player player) {
        return metadata.getSubtitle();
    }

    @Nullable
    @Override
    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
        if (metadata.getThumbnailBytes() != null && metadata.getThumbnailBytes().length > 0) {
            Glide.with(context)
                    .asBitmap()
                    .load(metadata.getThumbnailBytes())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            callback.onBitmap(resource);
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
            return null;
        }

        if (metadata.getThumbnailUri() != null && metadata.getThumbnailUri().length() > 0) {

            Glide.with(context)
                    .asBitmap()
                    .load(metadata.getThumbnailUri())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            callback.onBitmap(resource);
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
            return null;
        }

        return null;
    }
}
