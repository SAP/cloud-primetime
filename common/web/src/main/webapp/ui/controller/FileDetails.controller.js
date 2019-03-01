sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/primetime/ui/model/FileSize",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, Controller, JSONModel, FileSize, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.FileDetails", jQuery.extend({}, util, {
		context: "file",
		formFragments: {},

		onInit: function () {
			this.fileModel = new JSONModel();
			this.refModel = new JSONModel();

			this.fileModel.setSizeLimit(Number.MAX_VALUE);
			this.refModel.setSizeLimit(Number.MAX_VALUE);

			this.getView().setModel(this.fileModel, this.context);
			this.getView().setModel(this.refModel, "ref");
			this.getOwnerComponent().getRouter().getRoute("FileDetails").attachMatched(this.onRouteMatched.bind(this));
		},

		loadData : function(id) {
			this.fileModel.loadData("/s/api/fileservice/files/" + id, null, false);
			this.checkOwnerStatus(this.fileModel);

			if (this.isOwner) {
				this.refModel.loadData("/s/api/fileservice/files/" + id + "/references");
			}

			// FIXME: how to remove the lightbox if not needed instead of making it invisible which causes an error?
		},

		onCreatePageFromFile : function() {
			var model = new sap.ui.model.json.JSONModel();
			model.setProperty("/name", this.fileModel.getProperty("/name"));
			
			var type = "IMAGE";
			switch(this.fileModel.getProperty("/fileType")) {
			case "mp4":
		    	type = "MOVIE";
		        break;
			
			case "pdf":
				type = "PDF";
				break;
				
			}		
			model.setProperty("/pageType", type);
			model.setProperty("/file", {});
			model.setProperty("/file/id", this.getId());

			this.openCreatePageDialog(model);
		},

		onUsageDetails : function() {
			this.usageDialog = sap.ui.xmlfragment("sap.primetime.ui.view.FileUsageDialog", this);
			
			this.getView().addDependent(this.usageDialog);
			this.usageDialog.open();
		},

		onCloseUsageDetails : function() {
			this.usageDialog.close();
		},

		onTestPDF : function() {
			var url = this.getHostPart() + "/s/api/fileservice/files/" + this.getId() + "/content";
			window.open(this.getHostPart() + "/showPDF?content=" + encodeURIComponent(url) + "&page=1");
		},

		onDownloadFile : function() {
			var url = this.getHostPart() + "/s/api/fileservice/files/" + this.getId() + "/content?download=true";
			window.open(url);
		},

		onRotate : function() {
			var updatePromise = jQuery.ajax({
				url : "/s/api/fileservice/files/" + this.getId() + "/rotate",
				type : "POST",
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.loadData(this.getId());
				this.updateScreenshot();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
			}, this));
		},

		onPlay:  function() {
			var playButton = this.getView().byId("playvideo");
			var stopButton = this.getView().byId("stopvideo");
			var grabButton = this.getView().byId("usevideoframe");
			var screenshot = this.getView().byId("screenshot");
			var videoContainer = this.getView().byId("videoContainer");

			playButton.setVisible(false);
			stopButton.setVisible(true);
			videoContainer.setVisible(true);
			screenshot.setVisible(false);
		},

		onUseVideoFrame : function() {
			var video = document.getElementById("video");
			alert(video.currentTime);
			
			this.onStopVideo();
		},

		onStopVideo : function() {
			var playButton = this.getView().byId("playvideo");
			var stopButton = this.getView().byId("stopvideo");
			var grabButton = this.getView().byId("usevideoframe");
			var screenshot = this.getView().byId("screenshot");
			var videoContainer = this.getView().byId("videoContainer");

			playButton.setVisible(true);
			stopButton.setVisible(false);
			videoContainer.setVisible(false);
			screenshot.setVisible(true);
		},

		updateScreenshot: function() {
			var screenshot = this.getView().byId("screenshot");
			var screenshotLb = this.getView().byId("screenshotLb");
			var url = screenshot.getSrc().substr(0, screenshot.getSrc().indexOf("&")) + "&t=" + new Date().getTime();
			var urlLb = screenshotLb.getImageSrc().substr(0, screenshotLb.getImageSrc().indexOf("?")) + "?" + new Date().getTime();

			screenshot.setSrc(url);			
			screenshotLb.setImageSrc(urlLb);
		},

		handleUploadComplete : function() {
			this.loadData(this.getId());
			this.updateScreenshot();
		},

		handleEditPress : function () {
			this.toggleButtonsAndView(true);
		},

		handleCancelPress : function () {
			this.toggleButtonsAndView(false);
			this.loadData(this.getId());
		},

		handleSavePress : function () {
			var updatePromise = jQuery.ajax({
				url : "/s/api/fileservice/files/" + this.getId(),
				type : "PUT",
				data : this.fileModel.getJSON(),
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

			fragment = sap.ui.xmlfragment(this.getView().getId(), "sap.primetime.ui.view.FileDetails" + name, this);

			this.formFragments[name] = fragment;
			return this.formFragments[name];
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