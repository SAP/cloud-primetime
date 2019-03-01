package com.sap.primetime.dao.dto;

import java.util.ArrayList;
import java.util.List;

import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;

public class PlaylistReferences {
	private List<Screen> userScreens = new ArrayList<>();
	private long foreignScreens;
	private List<Playlist> userPlaylists = new ArrayList<>();
	private long foreignPlaylists;

	public List<Screen> getUserScreens() {
		return userScreens;
	}

	public void setUserScreens(List<Screen> userScreens) {
		this.userScreens = userScreens;
	}

	public void addUserScreen(Screen screen) {
		this.userScreens.add(screen);
	}

	public long getForeignScreens() {
		return foreignScreens;
	}

	public void setForeignScreens(long foreignScreens) {
		this.foreignScreens = foreignScreens;
	}

	public List<Playlist> getUserPlaylists() {
		return userPlaylists;
	}

	public void setUserPlaylists(List<Playlist> userPlaylists) {
		this.userPlaylists = userPlaylists;
	}

	public void addUserPlaylist(Playlist playlist) {
		this.userPlaylists.add(playlist);
	}

	public long getForeignPlaylists() {
		return foreignPlaylists;
	}

	public void setForeignPlaylists(long foreignPlaylists) {
		this.foreignPlaylists = foreignPlaylists;
	}
}
