sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/core/HTML",
    "sap/ui/model/json/JSONModel",
    "sap/base/util/UriParameters",
    "sap/primetime/ui/util"
], function(Controller, HTML, JSONModel, UriParameters, util) {
    "use strict";

    return Controller.extend("sap.primetime.ui.controller.RotateContent", jQuery.extend({}, util, {
        updateCheckInterval: 10 * 1000, // 10s
        updateStatisticsInterval: 60 * 1000, // 60s
        fullData: {},
        config: {},
        pages: [],
        pageReloadIntervals: [],
        currentIndex: 0,
        currentIndexNorm: 0,
        maxFrames: 3,
        playlistId: undefined,
        checkInterval: undefined,
        contentTimeout: undefined,
        switchTime: undefined,
        playlistStartTime: undefined,
        useAnimations: true,
        screenshotMode: false,
        transitionMode: "SLIDE",
        mode: "",
        autoPlay: true,

        onInit: function() {
        	this.useAnimations = (new UriParameters(window.location.href).get("noAnimations") !== "true");
        	this.doOnboarding = (new UriParameters(window.location.href).get("onboard") === "true");

			this.dataUrl = this.determineBoardToLoad();

        	this.userModel = new JSONModel();
			this.userModel.loadData("/s/api/userservice/user", null, false);

            $.getJSON(this.dataUrl).done(this.loadContent.bind(this));

            this.statisticsInterval = setInterval(this.updateStatistics.bind(this), this.updateStatisticsInterval, false);
            this.timerInterval = setInterval(this.updateTimer.bind(this), 1000);
        },

        determineBoardToLoad: function() {
        	var url = "";

        	var pagePreview = new UriParameters(window.location.href).get("pagePreview");
        	var page = new UriParameters(window.location.href).get("page");
        	var board = new UriParameters(window.location.href).get("playlist");
            var screen = new UriParameters(window.location.href).get("screen");
            var screenKey = new UriParameters(window.location.href).get("screenKey");

            if (pagePreview && pagePreview !== "null") {
            	url = "/s/api/pageservice/drafts/" + pagePreview;
            	this.updateCheckInterval = 2 * 1000;
            	this.mode = "page";
            } else if (page && page !== "null") {
            	url = "/s/api/pageservice/pages/" + page;
            	this.mode = "page";
            } else if (board && board !== "null") {
            	url = "/s/api/playlistservice/playlists/" + board + "?expand=true&includeMetrics=false";
            	this.mode = "playlist";
            } else if (screen && screen !== "null") {
               	url = "/s/api/screenservice/screens/" + screen + "?includeMetrics=false&live=true";
            	this.mode = "screen";
            } else if (screenKey && screenKey !== "null") {
               	url = "/s/api/screenservice/screens/bykey/" + screenKey + "?includeMetrics=false&live=true" + (this.doOnboarding ? "&autoOnboard=true" : "");
            	this.mode = "screen";
            } else {
    			this.getOwnerComponent().getRouter().navTo("AdminHome");
            }
            return url;
        },

        checkConfigForUpdate: function() {
            $.getJSON(this.dataUrl).done(this.reloadContentIfChanged.bind(this));
        },

        reloadContentIfChanged: function(data) {
            if (this.contentHasChanged(data)) {
                this.loadContent(data);
            }
        },

        contentHasChanged: function(data) {
        	this.checkRemoteControl(data);
        	
        	var hasChanged = (JSON.stringify(data) !== JSON.stringify(this.fullData));
            return hasChanged;
        },

        checkRemoteControl: function(data) {
        	var active = false;
        	
        	if (this.mode === "screen") {
	        	// react to control events
	        	if (data.pageToShow > 0) {
	        		for (var i = 0; i < this.fullData.playlist.pageReferences.length; i++) {
	        			if (this.fullData.playlist.pageReferences[i].id === data.pageToShow) {
	        				clearTimeout(this.contentTimeout);
	        				clearTimeout(this.checkInterval);
	        	            
	        				// install high frequency poll
	        				this.autoPlay = false;
	        				this.checkInterval = setInterval(this.checkConfigForUpdate.bind(this), 2000);
	        				this.currentIndex = i - 1;
	        				this.currentIndexNorm = i - 1;
	        				this.switchContent();
	        				active = true;
	        				break;
	        			}
	        		}
	        	} else if (!this.autoPlay) {
	        		// reset back to normal mode
	        		// this.loadContent(data); // FIXME: throws ID error, once this is resolved full reload is probably not necessary anymore
	        		location.reload();
	        	}
	        	
	        	// reset control data
	        	data.pageToShow = -1;
        	}

        	return active;
        },

        loadContent: function(data) {
        	this.clearContentAndEvents();
        	this.playlistId = 0;
        	this.fullData = data;
        	this.playlistStartTime = Date.now();

        	var playlist = (this.mode === "screen") ? data.playlist : data; 
        	if (playlist) {
	        	var content = playlist.pageReferences;
	            if (!content) {
	                content = [{
	                	page: data
	                }];
	            } else {
		            this.playlistId = playlist.id;
		            this.config.pageDisplayDuration = playlist.pageDisplayDuration;
	            }
	            this.addPages(content);
	            this.maxFrames = (this.mode === "screen" && this.fullData.lowMemoryMode) ? 3 : this.pages.length;

	            this.createIFrames();
        	}
        	if (this.mode === "screen") {
        		this.transitionMode = this.fullData.transitionMode;
        		this.screenshotMode = this.fullData.screenshotMode;
        	}
        	
            this.adaptHeaderFooter(0);

            if (!this.checkRemoteControl(this.fullData) && this.pages.length > 1) {
                var delay = ((this.pages[0].pageDisplayDurationOverride > 0) ? this.pages[0].pageDisplayDurationOverride : this.config.pageDisplayDuration);
                this.byId("remainingPageTime").setText(this.autoPlay ? delay : "paused");
                this.contentTimeout = setTimeout(this.switchContent.bind(this), delay * 1000);
                this.switchTime = Date.now() + delay * 1000;
            }

            this.checkInterval = setInterval(this.checkConfigForUpdate.bind(this), this.updateCheckInterval);
            this.statisticsInterval = setInterval(this.updateStatistics.bind(this), this.screenshotMode ? 10 * 1000 : this.updateStatisticsInterval, false);
            this.updateStatistics(true);

            return true;
        },

        clearContentAndEvents: function() {
            clearTimeout(this.contentTimeout);
            clearInterval(this.checkInterval);
            clearInterval(this.statisticsInterval);
            
    		for (var i = 0; i < this.pageReloadIntervals.length; i++) {
    			clearInterval(this.pageReloadIntervals[i]);
    		}
    		this.pageReloadIntervals = [];
            
            this.getView().byId("iFramePage").destroyContent();
            this.pages = [];
            this.currentIndex = 0;
            this.currentIndexNorm = 0;
        },

        addPages: function(content) {
            for (var i = 0; i < content.length; i++) {
                var page = content[i];
                this.pages.push(page);
            }
        },

        createIFrames: function() {
            for (var i = 0; i < this.maxFrames; i++) {
                var iFrame = this.createIFrame(this.pages[i], i);
                this.getView().byId("iFramePage").addContent(iFrame);
            }
        },

        createIFrame: function(page, number) {
            var iFrame = new sap.ui.core.HTML("iFrame" + number);
            var url = this.getUrl(page.page);
            var content;

            if (number === 0) {
                content = this.createIFrameContent(url, "current");
            } else if (number === 1) {
                content = this.createIFrameContent(url, "next");
            } else {
                content = this.createIFrameContent(url, "hiding");
            }
            iFrame.setContent(content);
            
            if (page.reloadInterval > 0) {
            	this.pageReloadIntervals.push(setInterval(this.refreshFrame.bind(this), page.reloadInterval * 1000, number));
            }

            return iFrame;
        },

        getUrl : function(page) {
            var url = (typeof page.url !== "undefined") ? page.url : "";
            if (page.pageType === "PDF") {
            	if (page.file && page.file.id > 0) {
            		url = this.getHostPart() + "/s/api/fileservice/files/" + page.file.id + "/content"; // TODO: switch to unprotected
            	}
            	url = this.getHostPart() + "/showPDF?content=" + this.encodeURIComponentFully(url) + "&page=" + page.page;
            } else if (page.pageType === "TEXT") {
            	url = this.getHostPart() + "/showText.html?text=" + this.encodeURIComponentFully(page.text);
            } else if (page.pageType === "HTML") {
            	url = this.getHostPart() + "/showHTML.html?text=" + this.encodeURIComponentFully(page.text);
            } else if (page.pageType === "IMAGE") {
            	if (page.file && page.file.id > 0) {
            		url = this.getHostPart() + "/showImage.html?file=" + this.encodeURIComponentFully(page.file.id);
            	}
            } else if (page.pageType === "MOVIE") {
            	if (page.file && page.file.id !== 0) {
            		url = this.getHostPart() + "/showMovie.html?file=" + this.encodeURIComponentFully(page.file.id);
            	}
            } else if (page.pageType === "TEMPLATE") {
            	if (page.file && page.file.id > 0) {
            		url = this.getHostPart() + "/s/api/fileservice/files/" + page.file.id + "/content/index.html?page=" + this.encodeURIComponentFully(page.id);
            	}
            } else if (page.pageType === "URLGRID") {
            	url = this.getHostPart() + "/showURLGrid.html?urls=" + this.encodeURIComponentFully(page.url) + "&gridX=" + page.gridX + "&gridY=" + page.gridY + "&ratio=" + page.gridRatio;
            } else if (page.pageType === "YOUTUBE") {
            	url = this.getHostPart() + "/showYouTube.html?id=" + this.encodeURIComponentFully(page.text);
            } else if (page.pageType === "MEDIASHARE") {
            	url = this.getHostPart() + "/showMediaShare.html?id=" + this.encodeURIComponentFully(page.text) + "&privateVideo=" + this.encodeURIComponentFully(page.mediasharePrivate);
            }
            
            return url;
        },

        refreshFrame: function(id) {
        	$("#iFrame" + id)[0].src += "";
        },

        createIFrameContent: function(url, style) {
            return "<iframe scrolling='no' data-no-lazy='true' class='" + style + "' src='" + url + "' allowfullscreen webkitallowfullscreen mozAllowFullScreen allow='autoplay; fullscreen; encrypted-media'/>";
        },

        changeIFrameStyle: function(id, style) {
            $("#iFrame" + id).attr("class", style + ((!this.useAnimations || this.transitionMode === "INSTANT") ? " notransition" : ""));
        },

        adaptHeaderFooter: function(pageIdx) {
    		var ownerVisible = (this.mode !== "screen" || (this.mode === "screen" && this.fullData.showOwners)) && this.pages.length > 0 && typeof this.pages[pageIdx].page.ownersDisplayText !== "undefined";
    		var titleText = this.pages[pageIdx] ? ((this.pages[pageIdx].page.title) ? this.pages[pageIdx].page.title : this.pages[pageIdx].page.name) : null;
    		var titleVisible = this.fullData.showHeader && (typeof titleText === "string" && titleText.length > 0);
    		var footerVisible = this.mode !== "screen" || this.fullData.showFooter === true; 
    		var QRVisible = this.mode === "screen";
    		var countdownVisible = (this.mode === "screen" || this.mode === "playlist") && this.pages.length > 1;

    		var page = this.byId("iFramePage");
    		page.setTitle(titleText);
    		page.setShowHeader(titleVisible);
    		page.setShowFooter(footerVisible);

    		// Create avatars
    		if (this.pages[pageIdx]) {
	    		var owners = this.pages[pageIdx].page.owners;
	    		var avatars = this.byId("ownerAvatars");
	    		var avatarCount = 0;
	    		avatars.destroyContent();
	    		if (ownerVisible) {
		    		for (var i = 0; i < owners.length; i++) {
		    			if (owners[i].contact) {
			    			avatarCount += 1;
		    				avatars.addContent(new sap.f.Avatar({
			    				src: owners[i].user.imageLink,
			    				tooltip: owners[i].user.displayName,
			    				displaySize: "XS"
			    			}).addStyleClass("sapUiTinyMarginTopBottom"));
		    			}
		    		}
	    		}
	    		
	    		if (avatarCount === 0) {
	    			ownerVisible = false;
	    		}
	    		this.byId("pageOwners").setText(this.pages[pageIdx].page.ownersDisplayText);
    		}
    		this.byId("ownerNames").setVisible(ownerVisible);
    		this.byId("pageCountdown").setVisible(countdownVisible);
    		this.byId("pageQRCode").setVisible(QRVisible);
    		this.byId("pageQRCode").setCode(this.getHostPart() + "/?" + this.mode + "=" + this.fullData.id + "#/companion");
        },

        loadFrame : function(page) {
   			setTimeout(function() {
   				$("#iFrame" + (page % this.maxFrames))[0].src = this.getUrl(this.pages[page % this.pages.length].page);
   			}.bind(this), 3000);        	
        },

        switchContent: function() {
            var pageToShow = this.currentIndex + 1;
            var pageToQueue = this.currentIndex + 2;
            
            for (var i = 0; i < this.maxFrames; i++) {
           		this.changeIFrameStyle(i, "hidden");
           		
           		var pageIdx = pageToShow + i;
           		
           		// load new data if in memory save mode and not current or page to hide
           		if (this.maxFrames < this.pages.length && i === this.maxFrames - 1) {
           			this.loadFrame(pageIdx);
           		}
            }

            this.changeIFrameStyle(this.currentIndex % this.maxFrames, "hiding");
            this.changeIFrameStyle(pageToQueue % this.maxFrames, "next");
            this.changeIFrameStyle(pageToShow % this.maxFrames, "current");
            
            this.currentIndex = pageToShow;
            this.currentIndexNorm = pageToShow % this.pages.length;

            var delay = ((this.pages[this.currentIndexNorm].pageDisplayDurationOverride > 0) ? this.pages[this.currentIndexNorm].pageDisplayDurationOverride : this.config.pageDisplayDuration);
            this.byId("remainingPageTime").setText(this.autoPlay ? delay : "paused");
            this.contentTimeout = setTimeout(this.switchContent.bind(this), delay * 1000);
            this.switchTime = Date.now() + delay * 1000;

            this.adaptHeaderFooter(this.currentIndexNorm);
            this.updateStatistics(true);

            // inform iframe
            $("#iFrame" + (pageToShow % this.maxFrames))[0].contentWindow.postMessage("become_active", "*");
        },

        updateTimer: function() {
        	if (this.autoPlay) {
	        	if (this.pages[this.currentIndexNorm] && this.pages.length > 1 && typeof this.switchTime !== "undefined") {
	        		this.byId("remainingPageTime").setText(Math.ceil((this.switchTime - Date.now()) / 1000));
	        	} else {
	        		this.byId("remainingPageTime").setText("");
	        	}
        	} else {
	        	this.byId("remainingPageTime").setText("paused");        		
        	}
        },

        updateStatistics: function(isNewContent) {
            if (this.mode === "screen" && this.fullData.id > 0) {
            	
            	// update statistics
	            var stats = new JSONModel();
            	stats.setProperty("/metric_browser", navigator.userAgent);
            	stats.setProperty("/metric_os", navigator.platform);
            	stats.setProperty("/metric_resX", $(window).width());
            	stats.setProperty("/metric_resY", $(window).height());
            	stats.setProperty("/metric_user", this.userModel.getProperty("/userId"));
            	stats.setProperty("/metric_playlistTime", Math.ceil((Date.now() - this.playlistStartTime) / 1000));
            	stats.setProperty("/screenshotMode", this.screenshotMode);
            	if (this.pages[this.currentIndexNorm]) {
                	stats.setProperty("/metric_currentPageId", this.pages[this.currentIndexNorm].id);
                	
                	if (isNewContent) {
                    	var delay = ((this.pages[this.currentIndexNorm].pageDisplayDurationOverride > 0) ? this.pages[this.currentIndexNorm].pageDisplayDurationOverride : this.config.pageDisplayDuration);
	                	stats.setProperty("/playlist", {});
	                	stats.setProperty("/playlist/pageDisplayDuration", delay);
                	}
            	} else {
            		stats.setProperty("/metric_currentPageId", -1);
            	}
            	
            	jQuery.ajax({
					url : "/s/api/screenservice/screens/" + this.fullData.id + "/statistics",
					type : "PUT",
					data : stats.getJSON(),
					contentType : "application/json; charset=UTF-8"
				}).fail(function(jqXHR, textStatus, errorThrown) {
					if (jqXHR.status === 401 || jqXHR.status === 405) {
						// assume session gone, reload window to re-login
						location.reload();
					}
				});            
            }        	
        }
    }));
});
