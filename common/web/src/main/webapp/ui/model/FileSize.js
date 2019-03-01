sap.ui.define([ "sap/ui/model/type/FileSize" ], function(UI5FileSize) {
	"use strict";

	return UI5FileSize.extend("sap.primetime.model.FileSize", {
		constructor : function() {
			return UI5FileSize.call(this, {
				maxFractionDigits : 2
			});
		}
	});
});