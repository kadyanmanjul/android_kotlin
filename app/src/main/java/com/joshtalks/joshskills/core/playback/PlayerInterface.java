/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.joshtalks.joshskills.core.playback;

import android.app.Activity;
import android.media.MediaPlayer;
import androidx.annotation.NonNull;
import com.joshtalks.joshskills.repository.local.entity.AudioType;
import com.joshtalks.joshskills.repository.local.entity.ChatModel;
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity;
import java.util.List;


/**
 * Allows {@link } to control media playback of {@link MediaPlayerHolder}.
 */
public interface PlayerInterface {

    void initMediaPlayer(@NonNull final ChatModel chatModel, @NonNull final AudioType song);

    void release();

    boolean isMediaPlayer();

    boolean isPlaying();

    void resumeOrPause();

    void reset();

    boolean isReset();

    void instantReset();

    AudioType getCurrentSong();

    String getNavigationArtist();

    void setNavigationArtist(@NonNull final String navigationArtist);
/*

    Album getNavigationAlbum();

    void setNavigationAlbum(@Nullable final Album navigationAlbum);
*/

    void setCurrentSong(final InboxEntity inboxEntity, final ChatModel chatModel, @NonNull final AudioType song, @NonNull final List<AudioType> songs);

    void skip(final boolean isNext);

    void openEqualizer(@NonNull final Activity activity);

    void seekTo(final int position);

    void setPlaybackInfoListener(final PlaybackInfoListener playbackInfoListener);

    @PlaybackInfoListener.State
    int getState();

    int getPlayerPosition();

    void registerNotificationActionsReceiver(final boolean isRegister);

    MediaPlayer getMediaPlayer();

    void onPauseActivity();

    void onResumeActivity();

    InboxEntity getConversation();

    void clearNotification();
}
