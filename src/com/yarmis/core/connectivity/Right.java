package com.yarmis.core.connectivity;

public enum Right {

	/**
	 * Right for changing the current song. It allows you to navigate the player
	 * to the previous and next song.
	 */
	PLAY_ORDER,
	/**
	 * Right for changing the way that songs are traversed in the playlist.
	 * Allows you to shuffle and repeat specific songs.
	 */
	PLAY_ORDER_ADVANCED,

	/**
	 * Right for changing the current play state. It allows you to pause, play
	 * and stop the music.
	 */
	PLAY_STATE,

	/**
	 * Right for seeing what the current play state is. It allows you to request
	 * the current song, whether a song is currently playing and more of the
	 * likes.
	 */
	PLAY_STATE_INFO, 
	
	
	LIBRARY_SEARCH_ACCESS, VOTE_REQUEST, VOTE_REMOVE, PLAYLIST_EDIT, PLAYLIST_DOWNLOAD, CONNECTIVITY_CHANGE_SETTINGS;
}
