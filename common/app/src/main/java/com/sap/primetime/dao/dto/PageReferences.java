package com.sap.primetime.dao.dto;

import java.util.ArrayList;
import java.util.List;

import com.sap.primetime.dao.entities.Playlist;

public class PageReferences {
	private List<Playlist> userPlaylists = new ArrayList<>();
	private long foreignPlaylists;

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
