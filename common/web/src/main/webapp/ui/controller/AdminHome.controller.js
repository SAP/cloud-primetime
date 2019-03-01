sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/m/MessageToast",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/m/UploadCollectionParameter",
	"sap/primetime/ui/model/DateTime",
	"sap/primetime/ui/model/FileSize",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, MessageToast, Controller, JSONModel, UploadCollectionParameter, DateTime, FileSize, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.AdminHome", jQuery.extend({}, util, {
		autoRefreshInterval: 60 * 1000,

		onInit: function () {
			this.playlistsModel = new JSONModel();
			this.pagesModel = new JSONModel();
			this.adPagesModel = new JSONModel();
			this.screensModel = new JSONModel();
			this.filesModel = new JSONModel();
			this.onboardingsModel = new JSONModel();
			this.settingsModel = new JSONModel();
			this.userModel = new JSONModel();
			this.stateModel = new JSONModel();
			this.data_usersModel = new JSONModel();
			this.data_pagesModel = new JSONModel();
			this.data_filesModel = new JSONModel();
			this.data_playlistsModel = new JSONModel();
			this.data_screensModel = new JSONModel();
			this.data_ecmModel = new JSONModel();
			
			this.adPagesModel.setSizeLimit(Number.MAX_VALUE);
			this.pagesModel.setSizeLimit(Number.MAX_VALUE);
			this.playlistsModel.setSizeLimit(Number.MAX_VALUE);
			this.screensModel.setSizeLimit(Number.MAX_VALUE);
			this.onboardingsModel.setSizeLimit(Number.MAX_VALUE);
			this.settingsModel.setSizeLimit(Number.MAX_VALUE);
			this.data_usersModel.setSizeLimit(Number.MAX_VALUE);
			this.data_pagesModel.setSizeLimit(Number.MAX_VALUE);
			this.data_filesModel.setSizeLimit(Number.MAX_VALUE);
			this.data_playlistsModel.setSizeLimit(Number.MAX_VALUE);
			this.data_screensModel.setSizeLimit(Number.MAX_VALUE);
			this.data_ecmModel.setSizeLimit(Number.MAX_VALUE);
			
			this.getView().setModel(this.playlistsModel, "playlists");
			this.getView().setModel(this.pagesModel, "pages");
			this.getView().setModel(this.adPagesModel, "adpages");
			this.getView().setModel(this.filesModel, "files");
			this.getView().setModel(this.screensModel, "screens");
			this.getView().setModel(this.onboardingsModel, "onboardings");
			this.getView().setModel(this.settingsModel, "settings");
			this.getView().setModel(this.userModel, "user");
			this.getView().setModel(this.stateModel, "state");
			this.getView().setModel(this.data_usersModel, "data_users");
			this.getView().setModel(this.data_pagesModel, "data_pages");
			this.getView().setModel(this.data_filesModel, "data_files");
			this.getView().setModel(this.data_playlistsModel, "data_playlists");
			this.getView().setModel(this.data_screensModel, "data_screens");
			this.getView().setModel(this.data_ecmModel, "data_ecm");

			this.stateModel.setProperty("/pageView", "grid");
			this.stateModel.setProperty("/screenView", "grid");
			this.getOwnerComponent().getRouter().getRoute("AdminHome").attachMatched(this.onRouteMatched.bind(this));

			setInterval(function() {this.getView().byId("detailsCarousel").next();}.bind(this), 15000);
		},

		autoRefresh : function() {
			this.loadData();
		},

		loadData : function() {
			this.screensModel.loadData("/s/api/screenservice/screens");
			this.userModel.loadData("/s/api/userservice/user");
			this.playlistsModel.loadData("/s/api/playlistservice/playlists?expand=true");
			this.pagesModel.loadData("/s/api/pageservice/pages");
			this.filesModel.loadData("/s/api/fileservice/files");
			this.adPagesModel.loadData("/s/api/pageservice/advertisedpages?maxResults=6");

			if (this.isAdmin()) {
				this.settingsModel.loadData("/s/api/systemservice/configsections");
				this.onboardingsModel.loadData("/s/api/screenservice/onboardings");
			}
			if (this.isDBAdmin()) {
				this.data_screensModel.loadData("/s/api/dbservice/screens");
				this.data_usersModel.loadData("/s/api/dbservice/users");
				this.data_playlistsModel.loadData("/s/api/dbservice/playlists");
				this.data_pagesModel.loadData("/s/api/dbservice/pages");
				this.data_filesModel.loadData("/s/api/dbservice/files");
			}
		},

		onLoadECMData : function() {
			this.data_ecmModel.loadData("/s/api/fileservice/ecm");
			MessageToast.show("Loading data in the background...");
		},

		onDeleteFileEcm : function(oEvent) {
			var id = oEvent.getParameter("documentId");
			var deletePromise = jQuery.ajax({
				url : "/s/api/fileservice/ecm/" + id,
				type : "DELETE"
			});
			jQuery.when(deletePromise).then(jQuery.proxy(function() {
				this.onLoadECMData();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
				this.onLoadECMData();
			}, this));
			
		},

		onDeleteThumbnails : function() {
			var createPromise = jQuery.ajax({
				url : "/s/api/fileservice/deletethumbnails",
				type : "POST",
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(createPromise).then(jQuery.proxy(function(data) {
				this.doFullReload();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
			}, this));

			MessageToast.show("Deletion in progress. Page will reload once done...");
		},

		doFullReload : function() {
			this.getView().getModel("system").loadData("/s/api/systemservice/info", null, false);			
			this.loadData();
		},

		onAbout : function(oEvent) {
			this.aboutPopover = sap.ui.xmlfragment("sap.primetime.ui.view.About", this);
			this.getView().addDependent(this.aboutPopover);
			this.aboutPopover.openBy(oEvent.getSource());
		},

		onSampleData : function(oEvent) {
			jQuery.ajax({
				url : "/s/api/systemservice/sampledata",
				async : false
			});
			this.doFullReload();
		},

		onLogoPress : function() {
			location.reload();
		},

		switchTab : function(oEvent) {
			var key = oEvent.getSource().getCustomData()[0].getProperty("value");
			this.byId("tabBar").setSelectedKey(key);
		},

		onShowSamplePlaylist : function() {
			window.open(this.getHostPart() + "/?screen=0");			
		},

		onSelectPlaylist : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("playlists").getObject().id;
			this.getOwnerComponent().getRouter().navTo("PlaylistDetails", {"id":id});
		},
		
		onSelectDBPlaylist : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("data_playlists").getObject().id;
			this.getOwnerComponent().getRouter().navTo("PlaylistDetails", {"id":id});
		},
		
		onSelectPage : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("pages").getObject().id;
			this.getOwnerComponent().getRouter().navTo("PageDetails", {"id":id});
		},
		
		onSelectAdPage : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("adpages").getObject().id;
			this.getOwnerComponent().getRouter().navTo("PageDetails", {"id":id});
		},
		
		onSelectDBPage : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("data_pages").getObject().id;
			this.getOwnerComponent().getRouter().navTo("PageDetails", {"id":id});
		},
		
		onSelectDBFile : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("data_files").getObject().id;
			this.getOwnerComponent().getRouter().navTo("FileDetails", {"id":id});
		},
		
		onSelectScreen : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("screens").getObject().id;
			this.getOwnerComponent().getRouter().navTo("ScreenDetails", {"id":id});
		},

		onSelectDBUser : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("data_users").getObject().id;
			this.getOwnerComponent().getRouter().navTo("UserDetails", {"id":id});
		},
		
		onGotoUserDetails : function(oEvent) {
			var id = this.userModel.getProperty("/id");
			this.getOwnerComponent().getRouter().navTo("UserDetails", {"id":id});
		},
		
		onScreenshotError : function(oEvent) {
			this.screensModel.setProperty(oEvent.getSource().getBindingContext("screens").getPath() + "/screenshotError", true);
		},

		onDBScreenshotError : function(oEvent) {
			this.data_screensModel.setProperty(oEvent.getSource().getBindingContext("data_screens").getPath() + "/screenshotError", true);
		},

		onSelectDBScreen : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("data_screens").getObject().id;
			this.getOwnerComponent().getRouter().navTo("ScreenDetails", {"id":id});
		},

		onSelectOnboarding : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("onboardings").getObject().id;
			this.getOwnerComponent().getRouter().navTo("ScreenDetails", {"id":id});
		},

		onSelectFile : function(oEvent) {
			var id = oEvent.getParameters().selectedItem.getDocumentId();
			oEvent.getParameters().selectedItem.setSelected(false);
			this.getOwnerComponent().getRouter().navTo("FileDetails", {"id":id});
		},
		
		onBeforeUploadStarts: function(oEvent) {
			var fileName = new UploadCollectionParameter({
				name: "filename",
				value: oEvent.getParameter("fileName")
			});
			oEvent.getParameters().addHeaderParameter(fileName);
		},
		
		onTypeMissmatch: function() {
			MessageToast.show("Unsupported file type.");
		},

		onCreatePlaylist : function() {
			this.playlistDialog = sap.ui.xmlfragment("sap.primetime.ui.view.PlaylistDialog", this);
			
			this.newPlaylistModel = new sap.ui.model.json.JSONModel();
			this.newPlaylistModel.setProperty("/name", "");

			this.playlistDialog.setModel(this.newPlaylistModel);
			this.getView().addDependent(this.playlistDialog);
			this.playlistDialog.open();
		},

		savePlaylistPressed : function() {
			this.playlistDialog.setBusy(true);

			var createPromise = jQuery.ajax({
				url : "/s/api/playlistservice/playlists",
				type : "POST",
				data : this.newPlaylistModel.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(createPromise).then(jQuery.proxy(function(data) {
				this.playlistDialog.destroy();
				this.getOwnerComponent().getRouter().navTo("PlaylistDetails", {
					id: data.id,
					query: {
						editMode: true
					}
				});
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.playlistDialog.setBusy(false);
				alert(textStatus);
			}, this));
		},

		cancelPlaylistPressed : function() {
			this.playlistDialog.destroy();
		},

		onCreateScreen : function() {
			this.screenDialog = sap.ui.xmlfragment("sap.primetime.ui.view.ScreenDialog", this);
			
			this.newScreenModel = new sap.ui.model.json.JSONModel();
			this.newScreenModel.setProperty("/name", this.userModel.getProperty("/fullName") + "'s Screen");

			this.screenDialog.setModel(this.newScreenModel);
			this.getView().addDependent(this.screenDialog);
			this.screenDialog.open();
		},

		saveScreenPressed : function() {
			this.screenDialog.setBusy(true);

			var createPromise = jQuery.ajax({
				url : "/s/api/screenservice/screens",
				type : "POST",
				data : this.newScreenModel.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(createPromise).then(jQuery.proxy(function(data) {
				this.screenDialog.destroy();
				this.getOwnerComponent().getRouter().navTo("ScreenDetails", {
					id: data.id,
					query: {
						editMode: true
					}
				});
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.screenDialog.setBusy(false);
				alert(textStatus);
			}, this));
		},

		cancelScreenPressed : function() {
			this.screenDialog.destroy();
		},

		onClaimScreen : function() {
			this.claimScreenDialog = sap.ui.xmlfragment("sap.primetime.ui.view.ScreenClaimDialog", this);
			
			this.claimScreenModel = new sap.ui.model.json.JSONModel();
			this.claimScreenModel.setProperty("/externalKey", "");
			this.claimScreenModel.setProperty("/reuseScreen", false);
			this.claimScreenModel.setProperty("/screenId", this.screensModel.getProperty("/0/id"));

			this.claimScreenDialog.setModel(this.claimScreenModel);
			this.getView().addDependent(this.claimScreenDialog);
			this.claimScreenDialog.open();
		},

		saveClaimScreenPressed : function() {
			this.claimScreenDialog.setBusy(true);
			
			var screenId = this.claimScreenModel.getProperty("/reuseScreen") ? this.claimScreenModel.getProperty("/screenId") : 0;
			var updatePromise = jQuery.ajax({
				url : "/s/api/screenservice/claim/" + encodeURIComponent(this.claimScreenModel.getProperty("/externalKey")) + "?screenId=" + screenId,
				type : "POST",
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.claimScreenDialog.destroy();
				this.loadData();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.claimScreenDialog.setBusy(false);
				alert(textStatus);
			}, this));
		},

		cancelClaimScreenPressed : function() {
			this.claimScreenDialog.destroy();
		},

		handleUploadComplete : function(oEvent) {
			if (oEvent.getParameters().status == 400) {
				alert(oEvent.getParameters().responseRaw);
			}
			this.loadData();
		},

		onStopImpersonation : function() {
			var postPromise = jQuery.ajax({
				url : "/s/api/userservice/stopimpersonation",
				type : "POST"
			});
			jQuery.when(postPromise).then(jQuery.proxy(function() {
				this.doFullReload();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				// TODO: only here since for some reason a parse error is shown after successful delete, find out why
				alert(textStatus);
			}, this));
		},

		onSaveAppConfig : function() {
			this.saveConfig(0);
		},

		onSaveApplianceConfig : function() {
			this.saveConfig(1);
		},

		saveConfig : function(idx) {
			var key = this.settingsModel.getProperty("/" + idx + "/key");
			var section = this.settingsModel.getProperty("/" + idx);

			var updatePromise = jQuery.ajax({
				url : "/s/api/systemservice/configsections/" + key,
				data : JSON.stringify(section),
				type : "PUT",
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.doFullReload();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.doFullReload();
				alert(textStatus);
			}, this));
		},

		onDeleteAppSetting : function(oEvent) {
			this.deleteSetting(oEvent, "app");
		},
		
		onDeleteApplianceSetting : function(oEvent) {
			this.deleteSetting(oEvent, "appliance");
		},
		
		deleteSetting : function(oEvent, group) {
			var key = oEvent.getSource().getBindingContext("settings").getObject().key;

			var deletePromise = jQuery.ajax({
				url : "/s/api/systemservice/configsections/" + group + "/" + key,
				type : "DELETE"
			});
			jQuery.when(deletePromise).then(jQuery.proxy(function() {
				this.doFullReload();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
				this.doFullReload();
			}, this));
		},

		userHasContent : function(screens, playlists, pages, files) {
			return screens.length > 0 || playlists.length > 0 || pages.length > 0 || files.length > 0;
		},

		userHasNoContent : function(screens, playlists, pages, files) {
			return !this.userHasContent(screens, playlists, pages, files);
		},
		
		activeScreens : function(screens) {
			var count = 0;
			for(var i = 0; i < screens.length; ++i){
			    if(screens[i].aliveState === "OK")
			        count++;
			}
			
			return count;
		},

		inactiveScreens : function(screens) {
			return screens.length - this.activeScreens(screens);
		},

		onRouteMatched : function(oEvent) {
			this.loadData();
		}
	}));
});