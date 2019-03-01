sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/m/MessageToast",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/ui/model/Filter",
	"sap/base/util/UriParameters",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, MessageToast, Controller, JSONModel, Filter, UriParameters, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.ScreenDetails", jQuery.extend({}, util, {
		context: "screen",
		formFragments: {},
		autoRefreshInterval: 5 * 1000,

		onInit: function () {
			this.screenModel = new JSONModel();
			this.playlistsModel = new JSONModel();
			this.screenEventsModel = new JSONModel();
			
			this.screenModel.setSizeLimit(Number.MAX_VALUE);
			this.playlistsModel.setSizeLimit(Number.MAX_VALUE);
			this.screenEventsModel.setSizeLimit(Number.MAX_VALUE);
			
			this.getView().setModel(this.screenModel, this.context);
			this.getView().setModel(this.playlistsModel, "playlists");
			this.getView().setModel(this.screenEventsModel, "screenEvents");
			this.getOwnerComponent().getRouter().getRoute("ScreenDetails").attachMatched(this.onRouteMatched.bind(this));
			
			this.getView().byId("uploadDebugForm").setVisible(new UriParameters(window.location.href).get("debug") === "true");
		},

		loadData : function(id) {
			this.playlistsModel.loadData("/s/api/playlistservice/catalog");
			this.refreshData(id);
		},

		refreshData: function(id) {
			this.screenEventsModel.loadData("/s/api/screenservice/screens/" + id + "/eventsperhour");
			this.screenModel.loadData("/s/api/screenservice/screens/" + id, null, false);
			
			this.screenModel.setProperty("/transitionIdx", this.transitionModes.indexOf(this.screenModel.getProperty("/transitionMode")));

			// prepare model so that selection can be saved
			if (!this.screenModel.getProperty("/playlist")) {
				this.screenModel.setProperty("/playlist", {});
			}			
		},

		autoRefresh : function() {
			if (this.editMode) return;

			this.refreshData(this.getId());
			var screenshot = this.getView().byId("screenshot");
			var screenshotLb = this.getView().byId("screenshotLb");
			var pos = screenshot.getSrc().lastIndexOf("?");
			var url;
			if (pos > 0) {
				url = screenshot.getSrc().substr(0, pos) + "?" + new Date().getTime();
			} else {
				url = screenshot.getSrc() + "?" + new Date().getTime();				
			}

			screenshot.setSrc(url);
			// screenshotLb.setImageSrc(url);
		},

		showLog: function() {
			MessageBox.show(this.screenModel.getProperty("/metric_applianceLog"), {
				icon: MessageBox.Icon.INFORMATION,
				title: "Appliance Log",
				actions: [MessageBox.Action.CLOSE]
			});
		},

		onScreenshotError: function() {
			var screenshot = this.getView().byId("screenshot");
			screenshot.setSrc("/ui/img/missing_screenshot.png");			
		},

		formatPageDetails: function(pageId) {
			var pageRefs = this.screenModel.getProperty("/playlist/pageReferences");
			if (pageRefs) {
				for (var i = 0; i < pageRefs.length; i++) {
					if (pageRefs[i].id === pageId) {
						return pageRefs[i].page.name + " (" + pageRefs[i].stats_viewCount + " times shown, " + this.formatSeconds(pageRefs[i].stats_showTime) + " total)" + (this.screenModel.getProperty("/pageToShow") < 0 ? "" : ", paused by " + this.screenModel.getProperty("/remoteUser"));
					}
				}
			}
			
			return "-nothing shown-";
		},

		formatShortPageDetails: function(pageId) {
			var pageRefs = this.screenModel.getProperty("/playlist/pageReferences");
			if (pageRefs) {
				for (var i = 0; i < pageRefs.length; i++) {
					if (pageRefs[i].id === pageId) {
						return pageRefs[i].page.name;
					}
				}
			}
			
			return "-nothing shown-";
		},

		onShowScreenDetails: function(oEvent) {
			this.getView().byId("extendedScreenDetails").setVisible(true);		
			oEvent.getSource().setVisible(false);
		},

		onShowScreenSettings: function(oEvent) {
			this.getView().byId("extendedScreenSettings").setVisible(true);		
			oEvent.getSource().setVisible(false);
		},

		onTestScreen : function() {
			window.open(this.getHostPart() + "/?screen=" + this.getId());
		},

		handleChangeOnboardingOwner: function() {
			// TODO: replace with UI5 dialog
			var owner = prompt("User-Id of new owner", this.getView().getModel("screen").getProperty("/requestedOwner"));

			if (owner !== null && owner !== "") {
				var tmpModel = new JSONModel();
				tmpModel.setProperty("/requestedOwner", owner);
				var updatePromise = jQuery.ajax({
					url : "/s/api/screenservice/screens/" + this.getId() + "/requestedOwner",
					type : "PUT",
					data : tmpModel.getJSON(),
					contentType : "application/json; charset=UTF-8"
				});
				jQuery.when(updatePromise).then(jQuery.proxy(function() {
					this.loadData(this.getId());
				}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
					alert(textStatus);
				}, this));
			}			
		},

		handleConfirmOnboarding: function() {
			var updatePromise = jQuery.ajax({
				url : "/s/api/screenservice/screens/" + this.getId() + "/onboard",
				type : "POST",
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.navHome();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
			}, this));
		},

		handleSavePress : function () {
			// translate index to ENUM value
			this.screenModel.setProperty("/transitionMode", this.transitionModes[this.screenModel.getProperty("/transitionIdx")]);
			if (!this.screenModel.getProperty("/playlist/id")) {
				this.screenModel.setProperty("/playlist", null);
			}
			
			var updatePromise = jQuery.ajax({
				url : "/s/api/screenservice/screens/" + this.getId(),
				type : "PUT",
				data : this.screenModel.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.toggleButtonsAndView(false);
				this.loadData(this.getId());
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
			}, this));
		},

		getFormFragment: function(name) {
			var fragment = this.formFragments[name];
			if (fragment) {
				return fragment;
			}

			fragment = sap.ui.xmlfragment(this.getView().getId(), "sap.primetime.ui.view.ScreenDetails" + name, this);

			this.formFragments[name] = fragment;
			return this.formFragments[name];
		},

		onTestUploadPressed: function(oEvent) {
			var uploader = this.getView().byId("testUploader");
			var key = this.screenModel.getProperty("/externalKey");
			uploader.setUploadUrl("/s/api/fileservice/screenkey/" + key);
			uploader.upload();
		},

		handleTestUploadComplete : function() {
			this.loadData(this.getId());
		},

		onCreatePlaylist : function() {
			this.playlistDialog = sap.ui.xmlfragment("sap.primetime.ui.view.PlaylistDialog", this);
			
			this.newPlaylistModel = new sap.ui.model.json.JSONModel();
			this.newPlaylistModel.setProperty("/name", "My Playlist");

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
				this.playlistsModel.loadData("/s/api/playlistservice/catalog", null, false);
				this.screenModel.setProperty("/playlist/id", data.id);
				
				MessageToast.show("New playlist was created.");				
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.playlistDialog.setBusy(false);
				alert(textStatus);
			}, this));
		},

		cancelPlaylistPressed : function() {
			this.playlistDialog.destroy();
		},

		onRouteMatched: function(oEvent) {
			this.loadData(oEvent.getParameter("arguments").id);
			
			if (oEvent.getParameter("arguments")["?query"] && oEvent.getParameter("arguments")["?query"].editMode) {
				this.toggleButtonsAndView(true);
			} else {
				this.toggleButtonsAndView(false);
			}
		}
	}));
});