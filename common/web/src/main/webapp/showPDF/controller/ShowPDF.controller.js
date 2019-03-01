sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/base/util/UriParameters",
    "de/nysoft/control/PDF"
], function(Controller, UriParameters, PDF) {
    "use strict";

    return Controller.extend("sap.hdb.showPDF.controller.ShowPDF", {
        onInit: function() {
            var content = this.determinePDFToShow();

            var oPDF = new PDF("pdf", {
                src: content
            });
            oPDF.attachPagesLoaded(this.showCorrectPage.bind(this));

            this.getView().byId("pdfPage").addContent(oPDF);
        },

        determinePDFToShow: function() {
            var content = "";
            var sContent = new UriParameters(window.location.href).get("content");
            if (sContent) {
                content = sContent;
            }

            return content;
        },

        determinePageToShow: function() {
            var pageNumber = 1;

            var pageParam = new UriParameters(window.location.href).get("page");
            if (pageParam) {
                pageNumber = parseInt(pageParam, 10);
            }

            return pageNumber;
        },

        showPageIfPossible: function(oPDF, pageNumber) {
            if (pageNumber > 0 && pageNumber <= oPDF.getNumPages()) {
                oPDF.setPage(pageNumber);
            }
        },

        showCorrectPage: function() {
            var pageNumber = this.determinePageToShow();
            var oPDF = sap.ui.getCore().byId("pdf");
            this.showPageIfPossible(oPDF, pageNumber);
        }
    });
});

