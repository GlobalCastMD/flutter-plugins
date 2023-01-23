package io.flutter.plugins.videoplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VideoMetadata {
    private @NonNull
    String title;
    public @NonNull String getTitle() { return title; }
    public void setTitle(@NonNull String setterArg) {
        if (setterArg == null) {
            throw new IllegalStateException("Nonnull field \"title\" is null.");
        }
        this.title = setterArg;
    }

    private @NonNull String subtitle;
    public @NonNull String getSubtitle() { return subtitle; }
    public void setSubtitle(@NonNull String setterArg) {
        if (setterArg == null) {
            throw new IllegalStateException("Nonnull field \"subtitle\" is null.");
        }
        this.subtitle = setterArg;
    }

    private @Nullable
    String thumbnailUri;
    public @Nullable String getThumbnailUri() { return thumbnailUri; }
    public void setThumbnailUri(@Nullable String setterArg) {
        this.thumbnailUri = setterArg;
    }

    private @Nullable byte[] thumbnailBytes;
    public @Nullable byte[] getThumbnailBytes() { return thumbnailBytes; }
    public void setThumbnailBytes(@Nullable byte[] setterArg) {
        this.thumbnailBytes = setterArg;
    }

    /** Constructor is private to enforce null safety; use Builder. */
    private VideoMetadata() {}
    public static final class Builder {
        private @Nullable String title;
        public @NonNull
        VideoMetadata.Builder setTitle(@NonNull String setterArg) {
            this.title = setterArg;
            return this;
        }
        private @Nullable String subtitle;
        public @NonNull
        VideoMetadata.Builder setSubtitle(@NonNull String setterArg) {
            this.subtitle = setterArg;
            return this;
        }
        private @Nullable String thumbnailUri;
        public @NonNull
        VideoMetadata.Builder setThumbnailUri(@Nullable String setterArg) {
            this.thumbnailUri = setterArg;
            return this;
        }
        private @Nullable byte[] thumbnailBytes;
        public @NonNull
        VideoMetadata.Builder setThumbnailBytes(@Nullable byte[] setterArg) {
            this.thumbnailBytes = setterArg;
            return this;
        }
        public @NonNull
        VideoMetadata build() {
            VideoMetadata metadata = new VideoMetadata();
            metadata.setTitle(title);
            metadata.setSubtitle(subtitle);
            metadata.setThumbnailUri(thumbnailUri);
            metadata.setThumbnailBytes(thumbnailBytes);
            return metadata;
        }
    }
}
