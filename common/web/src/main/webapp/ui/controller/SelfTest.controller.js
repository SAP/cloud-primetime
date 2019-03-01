sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/ui/model/Filter",
	"sap/m/MessageToast",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, Controller, JSONModel, Filter, MessageToast, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.SelfTest", jQuery.extend({}, util, {
		onInit: function () {
			this.infoModel = new JSONModel();
			this.getView().setModel(this.infoModel, "info");
			this.getOwnerComponent().getRouter().getRoute("SelfTest").attachMatched(this.onRouteMatched.bind(this));
		},

		loadData : function() {
			this.infoModel.setProperty("/mobile", sap.ui.Device.browser.mobile);
			this.infoModel.setProperty("/browserName", sap.ui.Device.browser.name);
			this.infoModel.setProperty("/browserVersion", sap.ui.Device.browser.version);

			this.infoModel.setProperty("/touchSupported", sap.ui.Device.support.touch);
			this.infoModel.setProperty("/fullscreen", sap.ui.Device.browser.fullscreen);
			this.infoModel.setProperty("/orientation", sap.ui.Device.orientation.landscape);

			this.infoModel.setProperty("/osName", sap.ui.Device.os.name);
			this.infoModel.setProperty("/osVersion", sap.ui.Device.os.version);

			this.infoModel.setProperty("/isCombi", sap.ui.Device.system.combi);
			this.infoModel.setProperty("/isDesktop", sap.ui.Device.system.desktop);
			this.infoModel.setProperty("/isPhone", sap.ui.Device.system.phone);
			this.infoModel.setProperty("/isTablet", sap.ui.Device.system.tablet);
		},

		onRouteMatched: function(oEvent) {
			this.loadData();
		}
	}));
});