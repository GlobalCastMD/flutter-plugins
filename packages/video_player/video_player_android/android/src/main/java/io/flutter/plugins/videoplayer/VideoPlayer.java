// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.Listener;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;
import io.flutter.plugin.common.EventChannel;
import io.flutter.view.TextureRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class VideoPlayer {
  private static final int MEDIA_NOTIFICATION_ID = 64856;
  private static final String MEDIA_NOTIFICATION_CHANNEL_ID = "io.flutter.plugins.videoplayer.media_notification_channel";

  private static final String FORMAT_SS = "ss";
  private static final String FORMAT_DASH = "dash";
  private static final String FORMAT_HLS = "hls";
  private static final String FORMAT_OTHER = "other";

  private ExoPlayer exoPlayer;

  private Surface surface;

  private TextureRegistry.SurfaceTextureEntry textureEntry;

  private QueuingEventSink eventSink;

  private EventChannel eventChannel;

  @VisibleForTesting boolean isInitialized = false;

  private final VideoPlayerOptions options;

  private PlayerNotificationManager playerNotificationManager;
  private MediaSessionCompat mediaSession;
  private MediaSessionConnector mediaSessionConnector;

  VideoPlayer(
      Context context,
      EventChannel eventChannel,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      String dataSource,
      String formatHint,
      @NonNull Map<String, String> httpHeaders,
      VideoPlayerOptions options,
      VideoMetadata metadata) {
    this.eventChannel = eventChannel;
    this.textureEntry = textureEntry;
    this.options = options;

    ExoPlayer.Builder exoPlayerBuilder = new ExoPlayer.Builder(context);

    if (metadata != null) {
      exoPlayerBuilder.setSeekForwardIncrementMs(10000);
      exoPlayerBuilder.setSeekBackIncrementMs(10000);
    }

    ExoPlayer exoPlayer = exoPlayerBuilder.build();

    Uri uri = Uri.parse(dataSource);
    DataSource.Factory dataSourceFactory;

    if (isHTTP(uri)) {
      DefaultHttpDataSource.Factory httpDataSourceFactory =
          new DefaultHttpDataSource.Factory()
              .setUserAgent("ExoPlayer")
              .setAllowCrossProtocolRedirects(true);

      if (httpHeaders != null && !httpHeaders.isEmpty()) {
        httpDataSourceFactory.setDefaultRequestProperties(httpHeaders);
      }
      dataSourceFactory = httpDataSourceFactory;
    } else {
      dataSourceFactory = new DefaultDataSource.Factory(context);
    }

    MediaSource mediaSource = buildMediaSource(uri, dataSourceFactory, formatHint, context);

    exoPlayer.setMediaSource(mediaSource);
    exoPlayer.prepare();

    setUpVideoPlayer(exoPlayer, new QueuingEventSink(), context, metadata);
  }

  // Constructor used to directly test members of this class.
  @VisibleForTesting
  VideoPlayer(
      ExoPlayer exoPlayer,
      EventChannel eventChannel,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      VideoPlayerOptions options,
      QueuingEventSink eventSink) {
    this.eventChannel = eventChannel;
    this.textureEntry = textureEntry;
    this.options = options;

    setUpVideoPlayer(exoPlayer, eventSink, null, null);
  }

  private static boolean isHTTP(Uri uri) {
    if (uri == null || uri.getScheme() == null) {
      return false;
    }
    String scheme = uri.getScheme();
    return scheme.equals("http") || scheme.equals("https");
  }

  private MediaSource buildMediaSource(
      Uri uri, DataSource.Factory mediaDataSourceFactory, String formatHint, Context context) {
    int type;
    if (formatHint == null) {
      type = Util.inferContentType(uri);
    } else {
      switch (formatHint) {
        case FORMAT_SS:
          type = C.CONTENT_TYPE_SS;
          break;
        case FORMAT_DASH:
          type = C.CONTENT_TYPE_DASH;
          break;
        case FORMAT_HLS:
          type = C.CONTENT_TYPE_HLS;
          break;
        case FORMAT_OTHER:
          type = C.CONTENT_TYPE_OTHER;
          break;
        default:
          type = -1;
          break;
      }
    }
    switch (type) {
      case C.CONTENT_TYPE_SS:
        return new SsMediaSource.Factory(
                new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                new DefaultDataSource.Factory(context, mediaDataSourceFactory))
            .createMediaSource(MediaItem.fromUri(uri));
      case C.CONTENT_TYPE_DASH:
        return new DashMediaSource.Factory(
                new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                new DefaultDataSource.Factory(context, mediaDataSourceFactory))
            .createMediaSource(MediaItem.fromUri(uri));
      case C.CONTENT_TYPE_HLS:
        return new HlsMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri));
      case C.CONTENT_TYPE_OTHER:
        return new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri));
      default:
        {
          throw new IllegalStateException("Unsupported type: " + type);
        }
    }
  }

  private void setUpVideoPlayer(ExoPlayer exoPlayer, QueuingEventSink eventSink, @Nullable Context context, @Nullable VideoMetadata metadata) {
    this.exoPlayer = exoPlayer;
    this.eventSink = eventSink;

    eventChannel.setStreamHandler(
        new EventChannel.StreamHandler() {
          @Override
          public void onListen(Object o, EventChannel.EventSink sink) {
            eventSink.setDelegate(sink);
          }

          @Override
          public void onCancel(Object o) {
            eventSink.setDelegate(null);
          }
        });

    surface = new Surface(textureEntry.surfaceTexture());
    exoPlayer.setVideoSurface(surface);
    setAudioAttributes(exoPlayer, options.mixWithOthers);

    exoPlayer.addListener(
        new Listener() {
          private boolean isBuffering = false;

          public void setBuffering(boolean buffering) {
            if (isBuffering != buffering) {
              isBuffering = buffering;
              Map<String, Object> event = new HashMap<>();
              event.put("event", isBuffering ? "bufferingStart" : "bufferingEnd");
              eventSink.success(event);
            }
          }

          @Override
          public void onPlaybackStateChanged(final int playbackState) {
            if (playbackState == Player.STATE_BUFFERING) {
              setBuffering(true);
              sendBufferingUpdate();
            } else if (playbackState == Player.STATE_READY) {
              if (!isInitialized) {
                isInitialized = true;
                sendInitialized();
              }
            } else if (playbackState == Player.STATE_ENDED) {
              Map<String, Object> event = new HashMap<>();
              event.put("event", "completed");
              eventSink.success(event);
            }

            if (playbackState != Player.STATE_BUFFERING) {
              setBuffering(false);
            }
          }

          @Override
          public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
              Map<String, Object> event = new HashMap<>();
              event.put("event", "remotePlaybackUpdate");
              event.put("position", newPosition.positionMs);
              event.put("playing", exoPlayer.isPlaying());
              eventSink.success(event);
            }
          }

          @Override
          public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                Map<String, Object> event = new HashMap<>();
                event.put("event", "remotePlaybackUpdate");
                event.put("position", exoPlayer.getContentPosition());
                event.put("playing", exoPlayer.isPlaying());
                eventSink.success(event);
            }
          }

          @Override
          public void onPlayerError(final PlaybackException error) {
            setBuffering(false);
            if (eventSink != null) {
              eventSink.error("VideoError", "Video player had error " + error, null);
            }
          }
        });

    if (context != null && metadata != null) {
      mediaSession = new MediaSessionCompat(context, "ExoPlayer");
      mediaSession.setActive(true);
      mediaSessionConnector = new MediaSessionConnector(mediaSession);
      mediaSessionConnector.setPlayer(this.exoPlayer);

      PlayerNotificationManager.Builder playerNotificationManagerBuilder = new PlayerNotificationManager.Builder(
              context, MEDIA_NOTIFICATION_ID, MEDIA_NOTIFICATION_CHANNEL_ID);
      playerNotificationManagerBuilder.setMediaDescriptionAdapter(
        new MediaDescriptionAdapter(context, metadata)
      );
      playerNotificationManagerBuilder.setChannelNameResourceId(R.string.media_notification_channel_name);
      playerNotificationManagerBuilder.setChannelDescriptionResourceId(R.string.media_notification_channel_description);

      playerNotificationManagerBuilder.setPauseActionIconResourceId(R.drawable.ic_pause);
      playerNotificationManagerBuilder.setPlayActionIconResourceId(R.drawable.ic_play_arrow);
      playerNotificationManagerBuilder.setRewindActionIconResourceId(R.drawable.ic_replay_10);
      playerNotificationManagerBuilder.setFastForwardActionIconResourceId(R.drawable.ic_forward_10);

      playerNotificationManager = playerNotificationManagerBuilder.build();

      playerNotificationManager.setUseFastForwardAction(true);
      playerNotificationManager.setUseFastForwardActionInCompactView(true);
      playerNotificationManager.setUseRewindAction(true);
      playerNotificationManager.setUseRewindActionInCompactView(true);

      playerNotificationManager.setUseNextAction(false);
      playerNotificationManager.setUseNextActionInCompactView(false);
      playerNotificationManager.setUsePreviousAction(false);
      playerNotificationManager.setUsePreviousActionInCompactView(false);

      playerNotificationManager.setUsePlayPauseActions(true);
      playerNotificationManager.setUseStopAction(false);

      playerNotificationManager.setUseChronometer(true);
      playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());

      playerNotificationManager.setPlayer(this.exoPlayer);
    }
  }

  void sendBufferingUpdate() {
    Map<String, Object> event = new HashMap<>();
    event.put("event", "bufferingUpdate");
    List<? extends Number> range = Arrays.asList(0, exoPlayer.getBufferedPosition());
    // iOS supports a list of buffered ranges, so here is a list with a single range.
    event.put("values", Collections.singletonList(range));
    eventSink.success(event);
  }

  private static void setAudioAttributes(ExoPlayer exoPlayer, boolean isMixMode) {
    exoPlayer.setAudioAttributes(
        new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
        !isMixMode);
  }

  void play() {
    exoPlayer.setPlayWhenReady(true);
  }

  void pause() {
    exoPlayer.setPlayWhenReady(false);
  }

  void setLooping(boolean value) {
    exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
  }

  void setVolume(double value) {
    float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
    exoPlayer.setVolume(bracketedValue);
  }

  void setPlaybackSpeed(double value) {
    // We do not need to consider pitch and skipSilence for now as we do not handle them and
    // therefore never diverge from the default values.
    final PlaybackParameters playbackParameters = new PlaybackParameters(((float) value));

    exoPlayer.setPlaybackParameters(playbackParameters);
  }

  void seekTo(int location) {
    exoPlayer.seekTo(location);
    // TODO update controls notification position
  }

  long getPosition() {
    return exoPlayer.getCurrentPosition();
  }

  @SuppressWarnings("SuspiciousNameCombination")
  @VisibleForTesting
  void sendInitialized() {
    if (isInitialized) {
      Map<String, Object> event = new HashMap<>();
      event.put("event", "initialized");
      event.put("duration", exoPlayer.getDuration());

      if (exoPlayer.getVideoFormat() != null) {
        Format videoFormat = exoPlayer.getVideoFormat();
        int width = videoFormat.width;
        int height = videoFormat.height;
        int rotationDegrees = videoFormat.rotationDegrees;
        // Switch the width/height if video was taken in portrait mode
        if (rotationDegrees == 90 || rotationDegrees == 270) {
          width = exoPlayer.getVideoFormat().height;
          height = exoPlayer.getVideoFormat().width;
        }
        event.put("width", width);
        event.put("height", height);

        // Rotating the video with ExoPlayer does not seem to be possible with a Surface,
        // so inform the Flutter code that the widget needs to be rotated to prevent
        // upside-down playback for videos with rotationDegrees of 180 (other orientations work
        // correctly without correction).
        if (rotationDegrees == 180) {
          event.put("rotationCorrection", rotationDegrees);
        }
      }

      eventSink.success(event);
    }
  }

  void dispose() {
    if (isInitialized) {
      exoPlayer.stop();
    }
    // TODO destroy controls notification
    textureEntry.release();
    eventChannel.setStreamHandler(null);
    if (surface != null) {
      surface.release();
    }
    if (exoPlayer != null) {
      if (playerNotificationManager != null)
        playerNotificationManager.setPlayer(null);
      if (mediaSessionConnector != null)
        mediaSessionConnector.setPlayer(null);
      if (mediaSession != null) {
        mediaSession.setActive(false);
        mediaSession.release();
      }
      exoPlayer.release();
    }
  }
}
