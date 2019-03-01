sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, Controller, JSONModel, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.PublicPageCatalog", jQuery.extend({}, util, {
		onInit: function () {
			this.adPagesModel = new JSONModel();
			
			this.adPagesModel.setSizeLimit(Number.MAX_VALUE);
			
			this.getView().setModel(this.adPagesModel, "adpages");
			this.getOwnerComponent().getRouter().getRoute("PublicPageCatalog").attachMatched(this.onRouteMatched.bind(this));
		},

		loadData: function() {
			this.adPagesModel.loadData("/s/api/pageservice/advertisedpages");
		},

		onSelectAdPage: function(oEvent) {
			var id = oEvent.getSource().getBindingContext("adpages").getObject().id;
			this.getOwnerComponent().getRouter().navTo("PageDetails", {"id":id});
		},		

		onRouteMatched: function(oEvent) {
			this.loadData();
		}		
	}));
});