sap.ui.define([ "jquery.sap.global", "sap/ui/model/json/JSONModel" ], function(
		jQuery, JSONModel) {
	"use strict";

	return {
		pageTypes : [ "URL", "URLGRID", "PDF", "TEXT", "IMAGE", "HTML",
				"MOVIE", "YOUTUBE", "MEDIASHARE", "TEMPLATE" ],
		transitionModes : [ "INSTANT", "SLIDE" ],
		editMode : false,
		isOwner : false,
		isContentOwner : false,

		navHome : function() {
			this.getOwnerComponent().getRouter().navTo("AdminHome");
		},

		navRoot : function() {
			this.getOwnerComponent().getRouter().navTo("RotateContent");
		},

		isAdmin : function() {
			return this.getView().getModel("system").getProperty("/adminMode");
		},

		isDBAdmin : function() {
			return this.getView().getModel("system")
					.getProperty("/dbAdminMode");
		},

		checkOwnerStatus : function(contextModel) {
			this.isOwner = false;
			this.isContentOwner = false;

			var owners = contextModel.getProperty("/owners");
			if (typeof owners !== "undefined") {
				for (var i = 0; i < owners.length; i++) {
					if (owners[i].user.userId === this.getView().getModel(
							"system").getProperty("/currentUser")) {
						if (owners[i].role === "ADMINISTRATOR") {
							this.isOwner = true;
						} else {
							this.isContentOwner = true;
						}
						break;
					}
				}
			}
			contextModel.setProperty("/isContentOwner", this.isContentOwner);
			contextModel.setProperty("/isOwner", this.isOwner
					|| this.getView().getModel("system").getProperty(
							"/adminMode"));
		},

		getPageIcon : function(type) {
			switch (type) {
			case "URL":
				return "sap-icon://internet-browser";
			case "URLGRID":
				return "sap-icon://grid";
			case "PDF":
				return "sap-icon://pdf-attachment";
			case "TEXT":
				return "sap-icon://text";
			case "IMAGE":
				return "sap-icon://background";
			case "MOVIE":
				return "sap-icon://attachment-video";
			case "HTML":
				return "sap-icon://attachment-html";
			case "TEMPLATE":
				return "sap-icon://business-by-design";
			case "YOUTUBE":
				return "sap-icon://video";
			case "MEDIASHARE":
				return "sap-icon://video";
			default:
				return "sap-icon://grid";
			}
		},

		onShowUserDetails : function(oEvent) {
			this.userPopover = sap.ui.xmlfragment(
					"sap.primetime.ui.view.UserDetails", this);
			this.getView().addDependent(this.userPopover);
			this.userPopover.openBy(oEvent.getSource());
		},

		onLogout : function(oEvent) {
			var promise = jQuery.ajax({
				url : "/s/api/userservice/logout",
				type : "POST"
			});
			jQuery.when(promise).then(
					jQuery.proxy(function(data) {
						window.location.href = this.getView()
								.getModel("system").getProperty("/logoutUrl");
					}, this),
					jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
						alert(textStatus);
					}, this));
		},

		getPagePreviewIcon : function(page) {
			return page.screenshotUrl.startsWith("/ui") ? this
					.getPageIcon(page.pageType) : page.screenshotUrl;
		},

		formatRelativeDate : function(date) {
			if (typeof date === "undefined")
				return "";

			var oDate = new sap.ui.model.type.DateTime({
				formatOptions : {
					relative : true,
					relativeScale : "auto",
					source : {
						pattern : "yyyy-MM-ddTHH:mm:ss Z"
					}
				}
			});
			return oDate.formatValue(date);
		},

		formatSeconds : function(value) {
			var unit = "sec";
			var result = value;

			if (result > 120) {
				result = result / 60;
				unit = "min";

				if (result > 120) {
					result = result / 60;
					unit = "hours";

					if (result > 48) {
						result = result / 60;
						unit = "days";
					}
				}
			}
			return Math.round(result) + " " + unit;
		},

		getId : function() {
			return this.getView().getModel(this.context).getProperty("/id");
		},

		isValidJson : function(text) {
			try {
				if (!text.startsWith("{")) {
					return false;
				}
				JSON.parse(text);
			} catch (e) {
				return false;
			}
			return true;
		},

		isEditMode : function() {
			return this.editMode;
		},

		toggleButtonsAndView : function(bEdit) {
			this.getOwnerComponent().getModel("editState").setProperty(
					"/" + this.context + "EditMode", bEdit);
			this.editMode = bEdit;

			// Set the right form type
			this.showFormFragment(bEdit ? "Edit" : "Display");
		},

		showFormFragment : function(name) {
			var object = this.byId(this.context + "Details");

			object.removeAllContent();
			object.insertContent(this.getFormFragment(name));
		},

		handleEditPress : function() {
			this.toggleButtonsAndView(true);
		},

		handleCancelPress : function() {
			this.toggleButtonsAndView(false);
			this.loadData(this.getId());
		},

		handleCopyPress : function() {
			var name = prompt("Name for new " + this.context, "Copy of "
					+ this.getView().getModel(this.context)
							.getProperty("/name"));

			if (name !== null && name !== "") {
				var tmpModel = new JSONModel();
				tmpModel.setProperty("/name", name);
				var createPromise = jQuery.ajax({
					url : "/s/api/" + this.context + "service/" + this.context
							+ "s/" + this.getId() + "/copy",
					type : "POST",
					data : tmpModel.getJSON(),
					contentType : "application/json; charset=UTF-8"
				});
				jQuery.when(createPromise).then(
						jQuery.proxy(function(data) {
							this.getOwnerComponent().getRouter().navTo(
									this.upperCaseFirst(this.context)
											+ "Details", {
										"id" : data.id
									});
						}, this),
						jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
							alert(textStatus);
						}, this));
			}

		},

		upperCaseFirst : function(text) {
			var lower = text.toLowerCase();
			return lower.charAt(0).toUpperCase() + lower.substr(1);
		},

		getHostPart : function() {
			return window.location.protocol + "//" + window.location.hostname
					+ ":" + window.location.port;
		},

		onManageOwners : function() {
			this.ownersDialog = sap.ui.xmlfragment(
					"sap.primetime.ui.view.OwnersDialog", this);

			this.ownerModel = new sap.ui.model.json.JSONModel();
			this.ownerModel.setProperty("/user", {});
			this.ownerModel.setProperty("/user/userId", "");
			this.ownerModel.setProperty("/role", "ADMINISTRATOR");
			this.ownerModel.setProperty("/context", this.context);
			this.ownersDialog.setModel(this.ownerModel);
			this.ownersDialog.setModel(this.getView().getModel(this.context),
					"source");
			this.getView().addDependent(this.ownersDialog);
			this.ownersDialog.open();
		},

		getOwnerArray: function(data) {
			var id = data.getProperty("/user/userId");
			var ids = id.split(/[\s,;]+/);
			var result = [];
			
			for (var i = 0; i<ids.length; i++) {
				var temp = JSON.parse(data.getJSON());
				temp.user.userId = ids[i];
				
				result.push(temp);
			}
			
			return result;
		},

		onOwnerAddPressed : function() {
			var objectId = this.getView().getModel(this.context).getProperty(
					"/id");
			if (this.ownerModel.getProperty("/user/userId") === "")
				return;
			var ownerArr = this.getOwnerArray(this.ownerModel);
			
			var createPromise = jQuery.ajax({
				url : "/s/api/" + this.context + "service/" + this.context
						+ "s/" + objectId + "/owners",
				type : "POST",
				data : JSON.stringify(ownerArr),
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(createPromise).then(jQuery.proxy(function() {
				this.ownerModel.setProperty("/user/userId", "");
				this.loadData(objectId);
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.loadData(objectId);
				alert(textStatus);
			}, this));
		},

		onDeleteOwner : function(oEvent) {
			var id = oEvent.getParameter("listItem")
					.getBindingContext("source").getProperty("id");
			var objectId = this.getView().getModel(this.context).getProperty(
					"/id");

			var deletePromise = jQuery.ajax({
				url : "/s/api/" + this.context + "service/" + this.context
						+ "s/" + objectId + "/owners/" + id,
				type : "DELETE"
			});
			jQuery.when(deletePromise).then(jQuery.proxy(function() {
				this.loadData(objectId);
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
				this.loadData(objectId);
			}, this));
		},

		onChangeOwner : function(oEvent) {
			var id = oEvent.getSource().getBindingContext("source")
					.getProperty("id");
			var objectId = this.getView().getModel(this.context).getProperty(
					"/id");

			var ownerModel = new sap.ui.model.json.JSONModel();
			ownerModel.setProperty("/contact", oEvent.getSource()
					.getBindingContext("source").getProperty("contact"));

			var updatePromise = jQuery.ajax({
				url : "/s/api/" + this.context + "service/" + this.context
						+ "s/" + objectId + "/owners/" + id,
				data : ownerModel.getJSON(),
				type : "PUT",
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(updatePromise).then(jQuery.proxy(function() {
				this.loadData(objectId);
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				alert(textStatus);
				this.loadData(objectId);
			}, this));
		},

		onOwnersClosePressed : function() {
			this.ownersDialog.destroy();
		},

		onDeleteObject : function(oEvent) {
			var name = this.getView().getModel(this.context).getProperty(
					"/name");
			var id = this.getView().getModel(this.context).getProperty("/id");

			sap.m.MessageBox.confirm("Do you really want to delete '" + name
					+ '"', {
				onClose : jQuery.proxy(function(action) {
					if (action === "OK") {
						this.deleteObject(id);
					}
				}, this)
			});
		},

		deleteObject : function(id) {
			var deletePromise = jQuery.ajax({
				url : "/s/api/" + this.context + "service/" + this.context
						+ "s/" + id,
				type : "DELETE"
			});
			jQuery.when(deletePromise).then(jQuery.proxy(function() {
				this.navHome();
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				// TODO: only here since for some reason a parse error is shown
				// after successful delete, find out why
				// alert(textStatus);
				this.navHome();
			}, this));
		},

		onCreatePage : function() {
			var model = new sap.ui.model.json.JSONModel();
			model.setProperty("/name", "My Page");
			model.setProperty("/text", "Example Text");
			model.setProperty("/pageType", "TEXT");

			this.openCreatePageDialog(model);
		},

		openCreatePageDialog : function(model) {
			this.newPageModel = model;
			this.pageDialog = sap.ui.xmlfragment(
					"sap.primetime.ui.view.PageDialog", this);
			this.pageDialog.setModel(this.newPageModel);
			this.getView().addDependent(this.pageDialog);
			this.pageDialog.open();
		},

		savePagePressed : function() {
			this.pageDialog.setBusy(true);

			var createPromise = jQuery.ajax({
				url : "/s/api/pageservice/pages",
				type : "POST",
				data : this.newPageModel.getJSON(),
				contentType : "application/json; charset=UTF-8"
			});
			jQuery.when(createPromise).then(jQuery.proxy(function(data) {
				this.pageDialog.destroy();
				this.getOwnerComponent().getRouter().navTo("PageDetails", {
					id : data.id,
					query : {
						editMode : true
					}
				});
			}, this), jQuery.proxy(function(jqXHR, textStatus, errorThrown) {
				this.pageDialog.setBusy(false);
				alert(textStatus);
			}, this));
		},

		cancelPagePressed : function() {
			this.pageDialog.destroy();
		},

		findById : function(data, id) {
			for (var i = 0; i < data.length; i++) {
				if (data[i].id === id) {
					return data[i];
				}
			}

			return null;
		},

		encodeURIComponentFully : function(str) {
			return encodeURIComponent(str).replace(/[!'()*]/g, function(c) {
				return "%" + c.charCodeAt(0).toString(16);
			});
		}
	};
});

String.prototype.replaceAll = function(search, replacement) {
	var target = this;
	return target.replace(new RegExp(search, "g"), replacement);
};