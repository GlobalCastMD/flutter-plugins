package io.flutter.plugins.videoplayer;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import org.jetbrains.annotations.NotNull;

public class MediaDescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {
    @Nullable
    private final PendingIntent pendingIntent;

    private final Context context;

    /**
     * Creates a default {@link PlayerNotificationManager.MediaDescriptionAdapter}.
     *
     * @param pendingIntent The {@link PendingIntent} to be returned from {@link
     *     #createCurrentContentIntent(Player)}, or null if no intent should be fired.
     */
    public MediaDescriptionAdapter(@NotNull Context context, @Nullable PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
        this.context = context;
    }

    @Override
    public CharSequence getCurrentContentTitle(Player player) {
        return "Testing Title"; // TODO
    }

    @Nullable
    @Override
    public PendingIntent createCurrentContentIntent(Player player) {
        return pendingIntent;
    }

    @Nullable
    @Override
    public CharSequence getCurrentContentText(Player player) {
        return "Testing Subtitle"; // TODO
    }

    @Nullable
    @Override
    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
        Glide.with(context)
                .asBitmap() // TODO
                .load("https://target.scene7.com/is/image/Target/GUEST_02ea766d-006c-4ae7-a099-b40c6aaeffbd?wid=488&hei=488&fmt=pjpeg")
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
}
