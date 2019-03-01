sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/base/util/UriParameters",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, Controller, JSONModel, UriParameters, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.Companion", jQuery.extend({}, util, {
		onInit: function () {
            this.screen = new UriParameters(window.location.href).get("screen");

            this.screenModel = new JSONModel();
			this.getView().setModel(this.screenModel, "screen");

			this.getOwnerComponent().getRouter().getRoute("Companion").attachMatched(this.onRouteMatched.bind(this));
		},

		loadData: function() {
			this.screenModel.loadData("/s/api/screenservice/screens/" + this.screen);
		},

		openPlaylist: function() {
			window.open(this.getHostPart() + "/?playlist=" + this.screenModel.getProperty("/playlist/id"));			
		},

		navScreen: function() {
			this.getOwnerComponent().getRouter().navTo("ScreenDetails", {"id":this.screenModel.getProperty("/id")});
		},
		
		navRemote: function() {
			this.getOwnerComponent().getRouter().navTo("RemoteControl");
		},
		
		onRouteMatched: function(oEvent) {
			this.loadData();
		}
	}));
});