/*!
 * Library for SAPUI5 and OpenUI5
 * (c) Copyright 2015-2015 Manuel Richarz.
 * Licensed under the Apache License, Version 2.0 - see LICENSE.txt.
 */

/**
 * Initialization Code and shared classes of library de.nysoft.
 */
sap.ui.define(['jquery.sap.global', 'sap/ui/core/library', 'sap/m/library'], // library dependency
	function(jQuery) {
		"use strict";


		/**
		 * Library for SAPUI5 and OpenUI5
		 *
		 * @namespace
		 * @name de.nysoft
		 * @author Manuel Richarz
		 * @version 1.0.0
		 * @public
		 */

		// delegate further initialization of this library to the Core
		sap.ui.getCore().initLibrary({
			name : "de.nysoft",
			version: "1.0.0",
			dependencies : ["sap.ui.core", "sap.m"],
			types: [

			],
			interfaces: [
			
			],
			controls: [
				"de.nysoft.control.PDF"
			],
			elements: [

			]
		});

		return de.nysoft;

	}, /* bExport= */ false);
