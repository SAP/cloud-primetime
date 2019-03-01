package com.sap.primetime.dao.dto;

import java.util.ArrayList;
import java.util.List;

import com.sap.primetime.dao.entities.Page;

public class FileReferences {
	private List<Page> userPages = new ArrayList<>();
	private long foreignPages;

	public List<Page> getUserPages() {
		return userPages;
	}

	public void setUserPages(List<Page> userPages) {
		this.userPages = userPages;
	}

	public void addUserPage(Page page) {
		this.userPages.add(page);
	}

	public long getForeignPages() {
		return foreignPages;
	}

	public void setForeignPages(long foreignPages) {
		this.foreignPages = foreignPages;
	}

}
