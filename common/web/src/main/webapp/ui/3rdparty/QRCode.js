sap.ui.define([
	"sap/ui/core/Control",
	"sap/primetime/ui/3rdparty/qr/qrcode"
], function (Control) {
	"use strict";

	/**
	 * Constructor for a new QR Code.
	 *
	 * @param {string} [sId] id for the new control, generated automatically if no id is given
	 * @param {object} [mSettings] initial settings for the new control
	 *
	 * @class
	 * <p>
	 *     Shows a QR Code
	 * </p>
	 *
	 *
	 * @extends sap.ui.core.Control
	 * @version ${version}
	 *
	 * @constructor
	 * @public
	 * @alias com.penninkhof.qrcode.control.QRCode
	 * @ui5-metamodel This control/element also will be described in the UI5 (legacy) designtime metamodel
	 */
	var QRCodeControl = Control.extend("sap.primetime.ui.3rdparty.QRCode", {

		metadata : {
			properties : {

				/**
				 * Width of the QR Code
				 */
				width : { type : "sap.ui.core.CSSSize", group : "Appearance", defaultValue : "200px" },

				/**
				 * Height of the QR Code
				 */
				height : { type : "sap.ui.core.CSSSize", group : "Appearance", defaultValue : "200px" },

				/**
				 * Text encoded in the QR code
				 */
				code: { type: "string", group : "Appearance", defaultValue : null },

				/**
				 * Dark color of the QR Code
				 */
				colorDark: { type : "sap.ui.core.CSSColor", group : "Appearance", defaultValue : "#000000" },

				/**
				 * Light color of the QR Code
				 */
				colorLight: { type : "sap.ui.core.CSSColor", group : "Appearance", defaultValue : "#ffffff" }
			}
		},

		/**
		 * Control lifecycle method that is fired after the control has been rendered (added to the DOM)
		 */
		onAfterRendering: function() {
			this.qrCode = new QRCode(this.getDomRef(), {
				text: this.getCode(),
				width: this._CSSSizeToPixel(this.getWidth()),
				height: this._CSSSizeToPixel(this.getHeight()),
				colorDark : this.getColorDark(),
				colorLight : this.getColorLight()
			});
        },

		/**
		 * Control lifecycle method that is fired when the control needs to be rendered
		 * @param rm
		 * @param oControl
		 */
		renderer: function(rm, oControl) {
			rm.write("<div");
			rm.writeControlData(oControl);
			rm.addClass("qrcode");
			rm.writeClasses();
			rm.write(">");
			rm.write("</div>");
		},

		/**
		 * Setter for the code property
		 * Prevents rerendering of the full control and just modifies the code embedded in the QR code
		 * @param code
		 */
        setCode: function(code) {
        	this.setProperty("code", code, true);
			this._update();
        },

		/**
		 * Setter for the colorDark property
		 * Prevents rerendering of the full control and just modifies the code embedded in the QR code
		 * @param code
		 */
        setColorDark: function(color) {
        	this.setProperty("colorDark", color, true);
			this._update();
        },

		/**
		 * Setter for the colorLight property
		 * Prevents rerendering of the full control and just modifies the code embedded in the QR code
		 * @param code
		 */
        setColorLight: function(color) {
        	this.setProperty("colorLight", color, true);
			this._update();
        },

		/**
		 * Updates the QR code without completely rerendering it
		 */
		_update: function() {
			if (this.qrCode) {
				this.qrCode._htOption.colorDark = this.getColorDark();
				this.qrCode._htOption.colorLight = this.getColorLight();
				if (this.getCode()) {
					this.qrCode.makeCode(this.getCode());
				} else {
					this.qrCode.clear();
				}
			}
		}

	});

	/**
	 * Calculates the pixel value from a given CSS size and returns it with or without unit.
	 * @param sCSSSize
	 * @param bReturnWithUnit
	 * @returns {string|number} Converted CSS value in pixel
	 * @private
	 */
	QRCodeControl.prototype._CSSSizeToPixel = function(sCSSSize, bReturnWithUnit) {
		var sPixelValue = 0;
		if (sCSSSize) {
			if (sCSSSize.endsWith("px")) {
				sPixelValue = parseInt(sCSSSize, 10);
			} else if (sCSSSize.endsWith("em") || sCSSSize.endsWith("rem")) {
				sPixelValue = Math.ceil(parseFloat(sCSSSize) * this._getBaseFontSize());
			}
		}
		if (bReturnWithUnit) {
			return sPixelValue + "px";
		} else {
			return parseInt(sPixelValue, 10);
		}
	};

	return QRCodeControl;

});
