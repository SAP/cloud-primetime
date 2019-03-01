sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/m/MessageToast",
	"sap/base/util/UriParameters",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, Controller, JSONModel, MessageToast, UriParameters, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.RemoteControl", jQuery.extend({}, util, {
		remoteKey: undefined,

		onInit: function () {
            this.screen = new UriParameters(window.location.href).get("screen");

            this.screenModel = new JSONModel();
			this.getView().setModel(this.screenModel, "screen");

			this.getOwnerComponent().getRouter().getRoute("RemoteControl").attachMatched(this.onRouteMatched.bind(this));

			setInterval(this.loadData.bind(this), 5000);			
			MessageToast.show("You now have 10 minutes to control the screen until playback is resumed. You can also resume manually.", {duration:7000});
		},

		loadData : function() {
			this.screenModel.loadData("/s/api/screenservice/screens/" + this.screen);
		},

		navCompanion : function() {
			this.getOwnerComponent().getRouter().navTo("Companion");
		},

		ensureKey: function() {
			// fetch access key in case non exists yet
			if (!this.remoteKey) {
				$.ajax({
					  url: "/s/api/screenservice/screens/" + this.screenModel.getProperty("/id") + "/control",
					  async: false
					}).done(function(data) {
					    this.remoteKey = data;
					}.bind(this));
			}			
		},
		
		onSelectPage : function(oEvent) {
			this.ensureKey();

			var id = oEvent.getSource().getBindingContext("screen").getProperty("id");
			this.screenModel.setProperty(oEvent.getSource().getBindingContext("screen").getPath() + "/isRequested", true);
			
            var control = new JSONModel();
        	control.setProperty("/pageToShow", id);

        	jQuery.ajax({
				url : "/s/api/screenservice/screens/" + this.screenModel.getProperty("/id") + "/control/" + this.remoteKey,
				type : "PUT",
				data : control.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});            			
		},
		
		onPause : function(oEvent) {
			this.ensureKey();

			var id = this.screenModel.getProperty("/metric_currentPageId");

            var control = new JSONModel();
        	control.setProperty("/pageToShow", id);

        	jQuery.ajax({
				url : "/s/api/screenservice/screens/" + this.screenModel.getProperty("/id") + "/control/" + this.remoteKey,
				type : "PUT",
				data : control.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});            			
		},
		
		onAutoPlay: function() {
			this.screenModel.setProperty("/playlist/pageReferences/0/isRequested", true);

			this.ensureKey();

			var control = new JSONModel();
        	control.setProperty("/pageToShow", -1);

        	jQuery.ajax({
				url : "/s/api/screenservice/screens/" + this.screenModel.getProperty("/id") + "/control/" + this.remoteKey,
				type : "PUT",
				data : control.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});            						
		},
		
		onRouteMatched: function(oEvent) {
			this.loadData();
		}

	}));
});