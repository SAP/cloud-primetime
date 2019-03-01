/*!
 * Library for SAPUI5 and OpenUI5
 * (c) Copyright 2015-2015 Manuel Richarz.
 * Licensed under the Apache License, Version 2.0 - see http://www.apache.org/licenses/LICENSE-2.0.
 */

sap.ui.define([
	"jquery.sap.global", 
	"sap/ui/core/Control", 
	"./../thirdparty/pdf", 
	"./../library",
	"sap/base/Log"
], function(jQuery, Control, PDF, library, Log) {
		"use strict";

		window.PDFJS.workerSrc = jQuery.sap.getModulePath('de.nysoft.thirdparty') + '/pdf.worker.js';

		/**
		 * PDF
		 * provides functionality to display a PDF-Document using JavaScript-Lib PDF.js
		 *
		 * @class
		 * @extends sap.ui.core.Control
		 * @alias de.nysoft.control.PDF
		 *
		 * @author Manuel Richarz
		 * @version 1.0.0
		 *
		 * @constructor
		 * @public
		 */
		var PDF = Control.extend("de.nysoft.control.PDF", {

			metadata : {
				properties : {

					"src": {
						type: "string",
						group: "Data"
					},

					"page": {
						type: "int",
						group: "Appearance",
						defaultValue: 1
					},

					"scaling" : {
						type: "any",
						group: "Appearance",
						defaultValue: 'fit'
					}

				},
				events: {
					documentLoaded: {},
					pagesLoaded: {},
					renderFinish: {}
				}
			},

			init: function() {
				this._iPage = null;
				this._fScaling = null;
				this._aPages = [];
				this._mPageRefs = [];

			},

			renderer: function(oRm, oControl) {
				oRm.write('<div');
				oRm.writeControlData(oControl);
				oRm.addClass("deNySoftPDF");
				oRm.writeClasses();
				oRm.write('><canvas></canvas></div>');

				oControl.invalidateCanvas();
			},

			renderCanvasContent: function() {
				//something is already in progress. we'll wait until it is ready
				if (this.getBusy()) {
					this.invalidateCanvas();
					return;
				}

				//only render if page or scaling has changed
				if (this.getPage() !== this._iPage || this.getScaling() !== this._fScaling) {
					this._iPage = this.getPage();
					this._fScaling = this.getScaling();
					var oPage = this._aPages[this._iPage];
					if (oPage) {
						Log.debug('Rendering CanvasContent');
						this.setBusy(true);
						var oPageRenderPromise = this.renderPage(oPage, this.getCanvas().get(0));
						oPageRenderPromise.then(jQuery.proxy(function() {
							this.fireRenderFinish();
							this.setBusy(false);
						}, this));
						return oPageRenderPromise;
					}
				}
			},

			/**
			 * invalidate canvas for page or scaling update
			 */
			invalidateCanvas: function() {
				if (!this._invalidation) {
					this._invalidation = setTimeout(jQuery.proxy(function () {
						this._invalidation = null;
						this.renderCanvasContent();
					}, this), 0);
				}
			},

			/**
			 * initialize PDF-Document
			 * @param sSrc
			 * @private
			 */
			_initDocument: function(sSrc) {
				this.getBusy(true);
				window.PDFJS.getDocument(sSrc).then(jQuery.proxy(function(oPdf) {
					Log.debug('PDF-Document loaded');

					this._oPdf = oPdf;
					this.fireDocumentLoaded(oPdf);

					var iNumPages = this.getNumPages(),
						aPagePromises = [];

					//load all pages
					for (var i = 1; i <= iNumPages; i++) {
						var oPagePromise = oPdf.getPage(i);
						oPagePromise.then(jQuery.proxy(this._onPageLoaded, this));
						aPagePromises.push(oPagePromise);
					}

					Promise.all(aPagePromises).then(jQuery.proxy(function() {
						Log.debug('All PDF-Pages are loaded');
						//reset temp data
						this._iPage = null;
						this._fScaling = null;
						//remove busy indicator
						this.setBusy(false);
						this.firePagesLoaded(this._aPages);
						//trigger rendering of canvas
						this.invalidateCanvas();
					}, this));
				}, this));
			},

			/**
			 * reference page after load
			 * @param oPage
			 * @private
			 */
			_onPageLoaded: function(oPage) {
				Log.debug('PDF-Page ' + oPage.pageNumber + ' loaded');
				this._aPages[oPage.pageNumber] = oPage;
				this._mPageRefs[oPage.ref.num + ' ' + oPage.ref.gen + 'R'] = oPage.pageNumber;
			},

			/**
			 * Render page of PDF-Document into canvas
			 * @param oPage
			 * @param domCanvas
			 * @returns {RenderTask|null}
			 * @public
			 */
			renderPage: function(oPage, domCanvas) {
				if (oPage && domCanvas) {
					//get scaling and viewport
					var fScaling = this._determineScalingFactor(oPage),
						oViewport = oPage.getViewport(fScaling);

					//apply sizes to canvas
					domCanvas.width = oViewport.width;
					domCanvas.height = oViewport.height;


					return oPage.render({
						canvasContext: domCanvas.getContext('2d'),
						viewport: oViewport
					});
				}
			},

			/**
			 * method to calculate the scalingFactor for a page
			 * @param oPage - page of PDF-Document
			 * @returns {float} - scalingFactor of page
			 * @private
			 */
			_determineScalingFactor: function(oPage) {
				var fScale = this.getScaling();

				//we don't need to determine scaling if scaling is hard defined
				if (typeof fScale === 'number') {
					return parseFloat(fScale, 10);
				}

				var jqPdf = this.$(),
					jqParent = jqPdf.parent();

				if (jqParent.length) {
					var oOrigViewport = oPage.getViewport(1.0),
					// remove 17px for browser scrollbars
						iWidth = jqParent.width(),// - 17,
						iScaleWidth = iWidth / oOrigViewport.width,
						iHeight = jqParent.height(), // - 17,
						iScaleHeight = iHeight / oOrigViewport.height;

					switch (fScale) {
						case 'fitWidth':
							return iScaleWidth;
						case 'fitHeight':
							return iScaleHeight;
						case 'fit':
							var iHeightDiff = (iHeight - oOrigViewport.height) * -1,
								iWidthDiff = (iWidth - oOrigViewport.width) * -1;
							return (iWidthDiff > iHeightDiff) ? iScaleWidth : iScaleHeight;
					}
				}

				return 1.0;
			},

			/**
			 * Navigate to first page of PDF-Document
			 * @public
			 */
			firstPage: function() {
				var iPage = this.getPage();
				if (iPage > 1) {
					this.setPage(1);
				}
			},

			/**
			 * Navigate to next page of PDF-Document
			 * @public
			 */
			nextPage: function() {
				var iPage = this.getPage(),
					iMaxPages = this.getNumPages();
				if (iPage < iMaxPages) {
					this.setPage(iPage + 1);
				} else {
					this.firstPage();
				}
			},

			/**
			 * Navigate to prev page of PDF-Document
			 * @public
			 */
			prevPage: function() {
				var iPage = this.getPage();
				if (iPage > 1) {
					this.setPage(iPage - 1);
				}
			},

			/**
			 * get canvas
			 * @returns {jQuery}
			 */
			getCanvas: function() {
				return this.$().children('canvas');
			},

			/**
			 * Get number of pages in this PDF-Document
			 * @returns {integer}
			 */
			getNumPages: function() {
				if (this._oPdf) {
					return this._oPdf.numPages;
				}
				return 0;
			},

			/**
			 * get PDF-Document
			 * @returns {PDFDocument}
			 * @public
			 */
			getPdf: function() {
				return this._oPdf;
			},

			/**
			 * get pageIndex by reference num and gen
			 * @param oRef
			 * @returns {integer}
			 */
			getPageIndex: function(oRef) {
				if (typeof oRef === 'object') {
					return this._mPageRefs[oRef.num + ' ' + oRef.gen + 'R'];
				}
			},

			/**
			 * Setter for current page of PDF-Document
			 * @param iPage - {integer} - pageNumber of page from PDF-Document
			 * @public
			 */
			setPage: function(iPage) {
				if (typeof iPage === 'number') {
					this.setProperty('page', iPage, true);
					this.invalidateCanvas();
				}
			},

			/**
			 * Setter for scaling of page
			 * @param fScaling - {float|string} - ScalingFactor as float or 'fit', 'fitHeight' and 'fitWidth'
			 * @public
			 */
			setScaling: function(fScaling) {
				if (typeof fScaling === 'number' || typeof fScaling  === 'string') {
					this.setProperty('scaling', fScaling, true);
					this.invalidateCanvas();
				}
			},

			/**
			 * Setter for PDF-Document source
			 * @param sSrc - string
			 * @public
			 */
			setSrc: function(sSrc) {
				if (typeof sSrc === 'string') {
					this.setProperty('src', sSrc, true);
					this._initDocument(sSrc);
				}
			}
		});

		return PDF;

	}, /* bExport= */ true);
