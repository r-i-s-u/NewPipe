package org.schabi.newpipe.fragments.list.playlist;

import org.schabi.newpipe.player.playqueue.PlayQueue;

/**
 * Interface for {@code 0} view holders
 * to give access to the play queue.
 */
public interface PlaylistControlViewHolder {
    PlayQueue getPlayQueue();
}
