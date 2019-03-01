sap.ui.define([ "sap/ui/model/type/DateTime" ], function(UI5DateTime) {
	"use strict";

	return UI5DateTime.extend("sap.primetime.model.DateTime", {
		constructor : function() {
			return UI5DateTime.call(this, {
				relative : true,
				relativeScale : 'auto',
				source : {
					pattern : 'yyyy-MM-ddTHH:mm:ss Z'
				}
			});
		}
	});
});