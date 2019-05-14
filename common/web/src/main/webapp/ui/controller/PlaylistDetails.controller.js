sap.ui.define([
	"jquery.sap.global",
	"sap/m/MessageBox",
	"sap/ui/core/mvc/Controller",
	"sap/ui/model/json/JSONModel",
	"sap/ui/model/Filter",
	"sap/ui/model/FilterOperator",
	"sap/m/MessageToast",
	"sap/primetime/ui/util"
], function (jQuery, MessageBox, Controller, JSONModel, Filter, FilterOperator, MessageToast, util) {
	"use strict";

	return Controller.extend("sap.primetime.ui.controller.PlaylistDetails", jQuery.extend({}, util, {
		context: "playlist",
		formFragments: {},
		usageDialog: null,
		
		onInit: function () {
			this.playlistModel = new JSONModel();
			this.playlistExpandedModel = new JSONModel();
			this.pageCatalogModel = new JSONModel();
			this.fileCatalogModel = new JSONModel();
			this.playlistCatalogModel = new JSONModel();
			this.refModel = new JSONModel();
	
			this.playlistModel.setSizeLimit(Number.MAX_VALUE);
			this.playlistExpandedModel.setSizeLimit(Number.MAX_VALUE);
			this.pageCatalogModel.setSizeLimit(Number.MAX_VALUE);
			this.fileCatalogModel.setSizeLimit(Number.MAX_VALUE);
			this.playlistCatalogModel.setSizeLimit(Number.MAX_VALUE);
			this.refModel.setSizeLimit(Number.MAX_VALUE);

			this.getView().setModel(this.playlistModel, this.context);
			this.getView().setModel(this.playlistExpandedModel, "playlistExpanded");
			this.getView().setModel(this.pageCatalogModel, "pageCatalog");
			this.getView().setModel(this.playlistCatalogModel, "playlistCatalog");
			this.getView().setModel(this.fileCatalogModel, "fileCatalog");
			this.getView().setModel(this.refModel, "ref");
			this.getOwnerComponent().getRouter().getRoute("PlaylistDetails").attachMatched(this.onRouteMatched.bind(this));
		},

		loadData : function(id) {
			this.playlistExpandedModel.loadData("/s/api/playlistservice/playlists/" + id + "?expand=true");
			this.refModel.loadData("/s/api/playlistservice/playlists/" + id + "/references");
			this.playlistModel.loadData("/s/api/playlistservice/playlists/" + id, null, false);
			
			this.checkOwnerStatus(this.playlistModel);
			
			if (this.isOwner) {			
				this.pageCatalogModel.loadData("/s/api/pageservice/catalog");
				this.playlistCatalogModel.loadData("/s/api/playlistservice/catalog?exclude=" + id);
				this.fileCatalogModel.loadData("/s/api/fileservice/files");
			}
		},

		onAddFile: function () {
			var aFilter = [];
			aFilter.push(new Filter("fileType", FilterOperator.NE, "template"));

			this.pageDialog = sap.ui.xmlfragment("sap.primetime.ui.view.FileCatalog", this);
			this.pageDialog.setMultiSelect(true);
			this.pageDialog.setModel(this.getView().getModel("fileCatalog"), "fileCatalog");
			this.pageDialog.getBinding("items").filter(aFilter);
			this.pageDialog.open();
		},

		onAddPage: function () {
			this.pageDialog = sap.ui.xmlfragment("sap.primetime.ui.view.PageCatalog", this);
			this.pageDialog.setMultiSelect(true);
			this.pageDialog.setModel(this.getView().getModel("pageCatalog"), "pageCatalog");
			this.pageDialog.getBinding("items").filter([]);
			this.pageDialog.open();
		},

		onAddPlaylist: function () {
			this.playlistDialog = sap.ui.xmlfragment("sap.primetime.ui.view.PlaylistCatalog", this);
			this.playlistDialog.setMultiSelect(true);
			this.playlistDialog.setModel(this.getView().getModel("playlistCatalog"), "playlistCatalog");
			this.playlistDialog.getBinding("items").filter([]);
			this.playlistDialog.open();
		},

		onCatalogSearch: function(oEvent) {
			var sValue = oEvent.getParameter("value");
			var nameFilter = new Filter("name", sap.ui.model.FilterOperator.Contains, sValue);
			var ownerFilter = new Filter("owner", sap.ui.model.FilterOperator.Contains, sValue);
			var oBinding = oEvent.getSource().getBinding("items");
			oBinding.filter([nameFilter]); // TODO: adding ownerFilter will not show any results anymore, how do we get an OR filtering?
		},

		onPageCatalogClose: function(oEvent) {
			var aContexts = oEvent.getParameter("selectedContexts");
			if (aContexts && aContexts.length) {
				for (var i = 0; i < aContexts.length; i++) {
					var pageId = aContexts[i].getProperty("id");
					var pageModel = new sap.ui.model.json.JSONModel();
					pageModel.setProperty("/id", pageId);

					var createPromise = jQuery.ajax({
						url : "/s/api/playlistservice/playlists/" + this.getId() + "/pages",
						type : "POST",
						async : false,
						data : pageModel.getJSON(),
						contentType : "application/json; charset=UTF-8"
					});
					jQuery.when(createPromise).then(jQuery.proxy(function() {
						this.loadData(this.getId());
						this.pageDialog.destroy();
					}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
						alert(textStatus);
					}, this));				
				}
			}
		},

		onPlaylistCatalogClose: function(oEvent) {
			var aContexts = oEvent.getParameter("selectedContexts");
			if (aContexts && aContexts.length) {
				for (var i = 0; i < aContexts.length; i++) {
					var playlistId = aContexts[i].getProperty("id");
					var playlistModel = new sap.ui.model.json.JSONModel();
					playlistModel.setProperty("/id", playlistId);

					var createPromise = jQuery.ajax({
						url : "/s/api/playlistservice/playlists/" + this.getId() + "/playlists",
						type : "POST",
						async : false,
						data : playlistModel.getJSON(),
						contentType : "application/json; charset=UTF-8"
					});
					jQuery.when(createPromise).then(jQuery.proxy(function() {
						this.loadData(this.getId());
						this.playlistDialog.destroy();
					}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
						alert(textStatus);
					}, this));				
				}
			}
		},

		onFileCatalogClose: function(oEvent) {
			var aContexts = oEvent.getParameter("selectedContexts");
			if (aContexts && aContexts.length) {
				for (var i = 0; i < aContexts.length; i++) {
					var fileId = aContexts[i].getProperty("id");
					var fileModel = new sap.ui.model.json.JSONModel();
					fileModel.setProperty("/id", fileId);

					var createPromise = jQuery.ajax({
						url : "/s/api/playlistservice/playlists/" + this.getId() + "/files",
						type : "POST",
						async : false,
						data : fileModel.getJSON(),
						contentType : "application/json; charset=UTF-8"
					});
					jQuery.when(createPromise).then(jQuery.proxy(function() {
						this.loadData(this.getId());
						this.pageDialog.destroy();
					}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
						alert(textStatus);
					}, this));				
				}
			}
		},

		onUsageDetails : function() {
			this.usageDialog = sap.ui.xmlfragment("sap.primetime.ui.view.PlaylistUsageDialog", this);
			
			this.getView().addDependent(this.usageDialog);
			this.usageDialog.open();
		},

		onCloseUsageDetails : function() {
			this.usageDialog.close();
		},

		onTestPlaylist : function() {
			window.open(this.getHostPart() + "/?playlist=" + this.getId());
		},

		handleCustomDurationChange : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("playlist").getProperty("id");
			var customDuration = oEvent.getSource().getBindingContext("playlist").getProperty("customDuration");
			var duration = oEvent.getSource().getBindingContext("playlist").getProperty("pageDisplayDurationOverride");

			var model = new JSONModel();
			if (customDuration) {
				model.setProperty("/pageDisplayDurationOverride", (duration !== 0) ? duration : oEvent.getSource().getBindingContext("playlist").getProperty("/pageDisplayDuration"));
			} else {
				model.setProperty("/pageDisplayDurationOverride", 0);				
			}
			model.setProperty("/repeatEveryIteration", oEvent.getSource().getBindingContext("playlist").getProperty("repeatEveryIteration"));
			model.setProperty("/repeatEveryPage", oEvent.getSource().getBindingContext("playlist").getProperty("repeatEveryPage"));
			
			var updatePromise = jQuery.ajax({
				url : "/s/api/playlistservice/playlists/" + this.getId() + "/pagereferences/" + id,
				type : "PUT",
				data : model.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.loadData(this.getId());
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
			}, this));			
		},

		onMovePage : function(oEvent) {
			var dragControl = oEvent.getParameter("draggedControl");
			var id = dragControl.getBindingContext("playlist").getProperty("id");
            var dragIndex = dragControl.getParent().indexOfItem(dragControl);
            
            var dropPosition = oEvent.getParameter("dropPosition");
            var dropControl = oEvent.getParameter("droppedControl");
            var dropIndex = dropControl.getParent().indexOfItem(dropControl) + (dropPosition == "On" ? 0 : (dropPosition == "Before" ? -1 : 0));
            
            if (dragIndex > dropIndex) {
                dropIndex++;
            }
            this.movePage(id, dropIndex);
		},

		movePage : function(id, newIdx) {
			var movePromise = jQuery.ajax({
				url : "/s/api/playlistservice/playlists/" + this.getId() + "/pagereferences/" + id + "/moveto/" + newIdx,
				type : "POST"
			});
			jQuery.when(movePromise).then(jQuery.proxy(function() {
				this.loadData(this.getId());
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.loadData(this.getId());
				// alert(textStatus);
			}, this));
		},

		onSelectPage : function(oEvent) {
			this.pagePopover = sap.ui.xmlfragment("sap.primetime.ui.view.PageActions", this);
			this.pagePopover.bindElement("playlist>" + oEvent.getSource().getBindingContext("playlist").getPath());
			
			this.getView().addDependent(this.pagePopover);
			this.pagePopover.openBy(oEvent.getSource());
		},

		onDeletePage : function(oEvent) {
			var refType = oEvent.getSource().getBindingContext("playlist").getProperty("refType");
			var name = refType === "PAGE" ? oEvent.getSource().getBindingContext("playlist").getProperty("page/name") : refType === "FILE" ? oEvent.getSource().getBindingContext("playlist").getProperty("file/name") : oEvent.getSource().getBindingContext("playlist").getProperty("playlist/name");
			var id = oEvent.getSource().getBindingContext("playlist").getProperty("id");

			sap.m.MessageBox.confirm("Do you really want to remove '" + name + '"', {
				onClose : jQuery.proxy(function(action) {
					if (action === "OK") {
						this.deletePage(id);
					}
				}, this)
			});
		},

		deletePage : function(id) {
			var deletePromise = jQuery.ajax({
				url : "/s/api/playlistservice/playlists/" + this.getId() + "/pagereferences/" + id,
				type : "DELETE"
			});
			jQuery.when(deletePromise).then(jQuery.proxy(function() {
				this.loadData(this.getId());
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				// alert(textStatus);
				// TODO: only here since for some reason a parseerror is shown after successful delete, find out why
				this.loadData(this.getId());
			}, this));
		},

		handleSavePress : function () {
			var updatePromise = jQuery.ajax({
				url : "/s/api/playlistservice/playlists/" + this.getId(),
				type : "PUT",
				data : this.playlistModel.getJSON(),
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

			fragment = sap.ui.xmlfragment(this.getView().getId(), "sap.primetime.ui.view.PlaylistDetails" + name, this);

			this.formFragments[name] = fragment;
			return this.formFragments[name];
		},

		onRouteMatched: function(oEvent) {
			if (this.usageDialog !== null) {
				this.usageDialog.close();
			}
			
			this.loadData(oEvent.getParameter("arguments").id);
			
			if (oEvent.getParameter("arguments")["?query"] && oEvent.getParameter("arguments")["?query"].editMode) {
				this.toggleButtonsAndView(true);
			} else {
				this.toggleButtonsAndView(false);
			}
		}
	}));
});