sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/m/MessageToast",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/ui/model/Filter",
	"sap/ui/model/FilterOperator",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, MessageToast, Controller, JSONModel, Filter, FilterOperator, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.PageDetails", jQuery.extend({}, util, {
		context: "page",
		formFragments: {},

		onInit: function () {
			this.pageModel = new JSONModel();
			this.filesModel = new JSONModel();
			this.parametersModel = new JSONModel();
			this.refModel = new JSONModel();
			this.pageListModel = new JSONModel();
			
			this.pageModel.setSizeLimit(Number.MAX_VALUE);
			this.filesModel.setSizeLimit(Number.MAX_VALUE);
			this.parametersModel.setSizeLimit(Number.MAX_VALUE);
			this.refModel.setSizeLimit(Number.MAX_VALUE);
			this.pageListModel.setSizeLimit(Number.MAX_VALUE);
			
			this.getView().setModel(this.pageModel, this.context);
			this.getView().setModel(this.filesModel, "files");
			this.getView().setModel(this.parametersModel, "parameters");
			this.getView().setModel(this.refModel, "ref");
			this.getView().setModel(this.pageListModel, "pageList");
			this.getOwnerComponent().getRouter().getRoute("PageDetails").attachMatched(this.onRouteMatched.bind(this));
		},

		loadData : function(id) {
			this.filesModel.loadData("/s/api/fileservice/files");
			this.refModel.loadData("/s/api/pageservice/pages/" + id + "/references");
			this.pageModel.loadData("/s/api/pageservice/pages/" + id, null, false);

			this.checkOwnerStatus(this.pageModel);
			
			if (typeof this.pageModel.getProperty("/file") === "undefined") {
				this.pageModel.setProperty("/file", {});
			}
			if (typeof this.pageModel.getProperty("/file/parameters") !== "undefined") {
				this.parametersModel.setJSON(this.pageModel.getProperty("/file/parameters"));
			}

			// set parameter values
			this.valuesModel = new JSONModel();
			if (this.isValidJson(this.pageModel.getProperty("/templateValues"))) {
				this.valuesModel.setJSON(this.pageModel.getProperty("/templateValues"));
			}
			var params = this.parametersModel.getProperty("/");
			for (var i = 0; i < params.length; i++) {
				this.parametersModel.setProperty("/" + i + "/value", this.valuesModel.getProperty("/" + params[i].key));				
			}
			this.setPreviewUrl();			
		},

		refreshFiles : function() {
			this.filesModel.loadData("/s/api/fileservice/files");			

			setTimeout(this.updateUI.bind(this), 1500);
			MessageToast.show("File upload finished.");
		},

		updateUI : function() {
			this.filterFiles(this.pageModel.getProperty("/pageType"));
			this.updatePageList();
		},

		updatePageList : function() {
			var pageArr = [];
			var file = this.findById(this.filesModel.getData(), this.pageModel.getProperty("/file/id"));
			if (file) {
				for (var i = 0; i < file.pageCount; i++) {
					pageArr[i] = {};
					pageArr[i].key = i + 1;
					pageArr[i].value = i + 1;
				}
			}
			this.pageListModel.setData(pageArr);							
		},

		onTypeChanged : function(oEvent) {
			var key = oEvent.getParameter("selectedItem").getKey();
			this.filterFiles(key);
			this.pageModel.setProperty("/file", {});
		},

		onFileChanged : function(oEvent) {
			this.updatePageList();
		},

		onShowPageDetails: function(oEvent) {
			this.getView().byId("extendedPageDetails").setVisible(true);		
			oEvent.getSource().setVisible(false);
		},
		
		filterFiles : function(key) {
			var aFilter = [];
			if (key === "PDF") {
				aFilter.push(new Filter("fileType", FilterOperator.EQ, "pdf"));
			} else if (key === "IMAGE") {
				aFilter.push(new Filter("fileType", FilterOperator.NE, "pdf"));
				aFilter.push(new Filter("fileType", FilterOperator.NE, "mp4"));
				aFilter.push(new Filter("fileType", FilterOperator.NE, "template"));
			} else if (key === "MOVIE") {
				aFilter.push(new Filter("fileType", FilterOperator.EQ, "mp4"));
			} else if (key === "TEMPLATE") {
				aFilter.push(new Filter("fileType", FilterOperator.EQ, "template"));
				// FIXME: to filter out broken templates, but only works for NE, aFilter.push(new Filter("errorState", FilterOperator.EQ, undefined));
			}

			var oList = this.byId("filesList");
			var oBinding = oList.getBinding("items");
			if (oBinding) {
				oBinding.filter(new Filter({
					filters: aFilter,
					and: true
				}));
			}
		},

		onTestPage : function() {
			window.open(this.getHostPart() + "/?page=" + this.getId());
		},

		setPreviewUrl : function() {
			this.byId("previewFrame").destroyContent();
			this.byId("previewFrame").addContent(new sap.ui.core.HTML({
				preferDOM: true,
				content: '<iframe src="' + this.getHostPart() + '/?pagePreview=' + this.getId() +  '" style="position:relative;width:44vw;height:calc(44vw*(9/16));min-height:200px;min-width:300px;"></iframe>'
			}));
		},

		updatePreview : function() {
			jQuery.ajax({
				url : "/s/api/pageservice/drafts/" + this.getId(),
				type : "PUT",
				data : this.pageModel.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});
		},

		startPreview : function() {
			if (this.previewHandler) {
				clearInterval(this.previewHandler);
			}
			this.previewHandler = setInterval(this.updatePreview.bind(this), 3000);			
		},

		stopPreview : function() {
			clearInterval(this.previewHandler);			

			jQuery.ajax({
				url : "/s/api/pageservice/drafts/" + this.getId(),
				type : "DELETE",
				contentType : "application/json; charset=UTF-8"
			});
		},

		handleSaveTemplateValues : function() {
			var values = new JSONModel();

			var params = this.parametersModel.getProperty("/");
			for (var i = 0; i < params.length; i++) {
				values.setProperty("/" + params[i].key, params[i].value);
			}
			this.pageModel.setProperty("/templateValues", values.getJSON());
			this.handleSavePress(null, true);
		},

		handleEditPress : function () {
			this.toggleButtonsAndView(true);
			this.updateUI();
			this.startPreview();
			
			// schedule again in case of late data arrival when entering edit mode directly through URL
			setTimeout(this.updateUI.bind(this), 1000);
		},

		handleCancelPress : function () {
			this.stopPreview();
			this.toggleButtonsAndView(false);
			this.loadData(this.getId());
		},

		handleSavePress : function (oEvent, isTemplate) {
			this.stopPreview();

			var updatePromise = jQuery.ajax({
				url : "/s/api/pageservice/pages/" + this.getId() + ((typeof isTemplate !== "undefined" && isTemplate) ? "/templatevalues" : ""),
				type : "PUT",
				data : this.pageModel.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.toggleButtonsAndView(false);
				this.loadData(this.getId());
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
			}, this));
		},

		handleRemoveScreenshot : function () {
			var deletePromise = jQuery.ajax({
				url : "/s/api/pageservice/pages/" + this.getId() + "/screenshot",
				type : "DELETE"
			});
			jQuery.when(deletePromise).then(jQuery.proxy(function() {
				this.loadData(this.getId());
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
				this.loadData(this.getId());
			}, this));
		},

		getFormFragment: function(name) {
			var fragment = this.formFragments[name];
			if (fragment) {
				return fragment;
			}

			fragment = sap.ui.xmlfragment(this.getView().getId(), "sap.primetime.ui.view.PageDetails" + name, this);

			this.formFragments[name] = fragment;
			return this.formFragments[name];
		},

		onUsageDetails : function() {
			this.usageDialog = sap.ui.xmlfragment("sap.primetime.ui.view.PageUsageDialog", this);
			
			this.getView().addDependent(this.usageDialog);
			this.usageDialog.open();
		},

		onCloseUsageDetails : function() {
			this.usageDialog.close();
		},

		handleUploadComplete : function() {
			this.loadData(this.getId());

			var token = "cb=" + new Date().getTime();
			var screenshot = this.byId("pageScreenshot");
			var url = screenshot.getSrc();
			if (url.indexOf("?") === -1) {
				url += "?";
			}
			if (jQuery.sap.getUriParameters().get("cb") !== null) {
				url = url.substr(0, url.lastIndexOf("cb")) + token;
			} else {
				url += "&" + token;
			}
			screenshot.setSrc(url);
		},

		cancelFileUploadPressed : function() {
			this.uploadDialog.destroy();
		},

		onRouteMatched: function(oEvent) {
			this.loadData(oEvent.getParameter("arguments").id);
			
			if (oEvent.getParameter("arguments")["?query"] && oEvent.getParameter("arguments")["?query"].editMode) {
				this.handleEditPress();
			} else {
				this.toggleButtonsAndView(false);
			}
		}
	}));
});