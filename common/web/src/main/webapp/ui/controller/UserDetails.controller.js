sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, Controller, JSONModel, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.UserDetails", jQuery.extend({}, util, {
		context: "user",
		formFragments: {},

		onInit: function () {
			this.userModel = new JSONModel();
			this.pagesModel = new JSONModel();
			this.screensModel = new JSONModel();
			this.filesModel = new JSONModel();
			this.playlistsModel = new JSONModel();
			
			this.userModel.setSizeLimit(Number.MAX_VALUE);
			this.pagesModel.setSizeLimit(Number.MAX_VALUE);
			this.screensModel.setSizeLimit(Number.MAX_VALUE);
			this.filesModel.setSizeLimit(Number.MAX_VALUE);
			this.playlistsModel.setSizeLimit(Number.MAX_VALUE);
			
			this.getView().setModel(this.userModel, this.context);
			this.getView().setModel(this.pagesModel, "pages");
			this.getView().setModel(this.playlistsModel, "playlists");
			this.getView().setModel(this.filesModel, "files");
			this.getView().setModel(this.screensModel, "screens");
			this.getOwnerComponent().getRouter().getRoute("UserDetails").attachMatched(this.onRouteMatched.bind(this));
		},

		loadData : function(id) {
			this.userModel.loadData("/s/api/userservice/users/" + id, null, false);
			this.pagesModel.loadData("/s/api/pageservice/pages?userId=" + this.userModel.getProperty("/userId"));
			this.playlistsModel.loadData("/s/api/playlistservice/playlists?expand=true&userId=" + this.userModel.getProperty("/userId"));
			this.filesModel.loadData("/s/api/fileservice/files?userId=" + this.userModel.getProperty("/userId"));
			this.screensModel.loadData("/s/api/screenservice/screens?userId=" + this.userModel.getProperty("/userId"));
		},

		onDeleteUser: function(oEvent) {
			var id = this.userModel.getProperty("/userId");

			sap.m.MessageBox.confirm("Do you really want to delete the user '" + id + '"', {
				onClose : jQuery.proxy(function(action) {
					if (action === "OK") {
						this.deleteUser(this.getId());
					}
				}, this)
			});
		},

		deleteUser : function(id) {
			var deletePromise = jQuery.ajax({
				url : "/s/api/userservice/users/" + id,
				type : "DELETE"
			});
			jQuery.when(deletePromise).then(jQuery.proxy(function() {
				this.navHome();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				// TODO: only here since for some reason a parse error is shown after successful delete, find out why
				// alert(textStatus);
				this.navHome();
			}, this));
		},

		onImpersonateUser : function() {
			var postPromise = jQuery.ajax({
				url : "/s/api/userservice/users/" + this.getId() + "/impersonate",
				type : "POST"
			});
			jQuery.when(postPromise).then(jQuery.proxy(function() {
				window.location.href = this.getHostPart();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				// TODO: only here since for some reason a parse error is shown after successful delete, find out why
				alert(textStatus);
			}, this));
		},

		onRouteMatched: function(oEvent) {
			this.loadData(oEvent.getParameter("arguments").id);
		}
	}));
});