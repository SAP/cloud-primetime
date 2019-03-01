sap.ui.define( [
	"sap/ui/core/UIComponent", 
	"sap/ui/model/json/JSONModel", 
	"./model/DateTime"
	], function (UIComponent, JSONModel) {
	"use strict";
	return UIComponent.extend("sap.primetime.ui", {
		metadata: {
			rootView: "sap.primetime.ui.view.App",
			routing: {
				config: {
					routerClass: "sap.m.routing.Router",
					viewPath: "sap.primetime.ui.view",
					controlId: "rootControl",
					controlAggregation: "pages",
					viewType: "XML",
					async: true
				},
				routes: [
					{
						name: "RotateContent",
						// empty hash for the home page
						pattern: "",
						target: "RotateContent"
					},
					{
						name: "RemoteControl",
						pattern: "remote",
						target: "RemoteControl"
					},
					{
						name: "Companion",
						pattern: "companion",
						target: "Companion"
					},
					{
						name: "PublicPageCatalog",
						pattern: "publicpagecatalog",
						target: "PublicPageCatalog"
					},
					{
						name: "SelfTest",
						pattern: "selftest",
						target: "SelfTest"
					},
					{
						name: "AdminHome",
						pattern: "admin",
						target: "AdminHome"
					},
					{
						name: "PlaylistDetails",
						pattern: "playlist/{id}:?query:",
						target: "PlaylistDetails"
					},
					{
						name: "PageDetails",
						pattern: "page/{id}:?query:",
						target: "PageDetails"
					},
					{
						name: "TemplateDetails",
						pattern: "template/{id}:?query:",
						target: "TemplateDetails"
					},
					{
						name: "FileDetails",
						pattern: "file/{id}",
						target: "FileDetails"
					},
					{
						name: "UserDetails",
						pattern: "user/{id}",
						target: "UserDetails"
					},
					{
						name: "ScreenDetails",
						pattern: "screen/{id}:?query:",
						target: "ScreenDetails"
					}
				],
				targets: {
					RotateContent: {
						viewName: "RotateContent",
						viewLevel: 0
					},
					RemoteControl: {
						viewName: "RemoteControl",
						viewLevel: 3
					},
					Companion: {
						viewName: "Companion",
						viewLevel: 2
					},
					PublicPageCatalog: {
						viewName: "PublicPageCatalog",
						viewLevel: 2
					},
					SelfTest: {
						viewName: "SelfTest",
						viewLevel: 2
					},
					AdminHome: {
						viewName: "AdminHome",
						viewLevel: 1
					},
					PlaylistDetails: {
						viewName: "PlaylistDetails",
						viewLevel: 2
					},
					PageDetails: {
						viewName: "PageDetails",
						viewLevel: 2
					},
					TemplateDetails: {
						viewName: "TemplateDetails",
						viewLevel: 2
					},
					FileDetails: {
						viewName: "FileDetails",
						viewLevel: 2
					},
					UserDetails: {
						viewName: "UserDetails",
						viewLevel: 2
					},
					ScreenDetails: {
						viewName: "ScreenDetails",
						viewLevel: 2
					}
				}
			}
		},

		init : function () {
			UIComponent.prototype.init.apply(this, arguments);

			var editStateModel = new JSONModel();
			this.setModel(editStateModel, "editState");

			// load system model only once
			this.systemModel = new JSONModel();
			this.systemModel.setSizeLimit(Number.MAX_VALUE);
			this.systemModel.loadData("/s/api/systemservice/info", null, false);
			this.setModel(this.systemModel, "system");
			
			// check for successful login and outdated cache
			var buildTime = localStorage.getItem("buildTime");
			if (typeof this.systemModel.getProperty("/version") === "undefined" || buildTime === null || buildTime !== this.systemModel.getProperty("/buildTime")) {
				localStorage.setItem("buildTime", this.systemModel.getProperty("/buildTime"));
				location.reload(true);
			}
			
			// auto-refresh logic
            var intervalId;
            this.getRouter().attachRoutePatternMatched(function (oEvent) {
               if (intervalId) {
                  clearInterval(intervalId);
                  intervalId = null;
               }
               this.getTargets().getViews().getView({ viewName: oEvent.getParameters().config.viewPath + "." + oEvent.getParameters().config.target, type: "XML" }).then(function (view) {
                            
               var controller = view.getController();
               if (controller.autoRefresh) {
                  // this will always start 5 sec after the controller was created 
                  // it does not take into account how long the initial data loading of the controller takes
                  intervalId = setInterval(controller.autoRefresh.bind(controller), controller.autoRefreshInterval);
               }
               });
            });
			
			// Parse the current url and display the targets of the route that matches the hash
			this.getRouter().initialize();
		}

	});
}, /* bExport= */ true);
