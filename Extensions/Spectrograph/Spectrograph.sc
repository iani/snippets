/* 

!!!!!!!!!!!!!!! UNDER DEVELOPMENT !!!!!!!!!!!!!!!!

Inspired by Spectrogram Quark of Thor Magnusson and Dan Stowell. 

This here is a redo using UniqueObject subclasses to simplify the 
synchronization between window, synth and routine. 

fft sizes > 1024 are not supported, because that is the largest size of a buffer that 
can be obtained with buf.getn at the moment.

Getting of buffers with larger sizes is documented in SC Help, but for spectrograph display purposes this would be overkill at the moment. 

*/

Spectrograph : UniqueWindow {
	classvar <current;
	classvar <minWidth = 400, <>backgroundColor, <>binColor;
	var <bounds, <server, <rate, <bufsize, <>stopOnClose = true;
	var <userview, <image, <imgWidth, <imgHeight;
	var <scrollWidth; // , scrollImage, clearImage;
	var <index;	// running count of the currently polled fft frame. 
				// received from FFTsynthPoller. Cached for asynchronous use by penObjects
	var <windowIndex;	// index of x pixel on image where current frame is being drawn

	var <imageObjects; 	// array of objecs that add display graphics to pixels on the image
	var <penObjects; 		// array of objecs that add display graphics using Pen
	var <drawSpectrogram, <drawCrosshair;	// the two built-in drawing objects
	var <scroll;			// object that scrolls the image when drawind has reached the end
	
	*initClass {
		Class.initClassTree(Color);
		backgroundColor = Color.black;
		binColor = Color.white;
	}
	
	*start { | name, bounds, server, rate = 0.04, bufsize = 1024 |
		^this.new	(name, bounds, server ? Server.default, rate, bufsize = 1024).start;
	}
	
	*new { | name, bounds, server, rate = 0.04, bufsize = 1024 |
		name = format("%:%", name = name ? "Spectrograph", server = server ? Server.default).asSymbol;
		^current = super.new(name, bounds, server, rate, bufsize);
	}

	name { ^key[1] }
	
	init { | argBounds, argServer, argRate, argBufsize |
		var window; // just for naming convenience
		bounds = argBounds ?? { Window.centeredWindowBounds(1000) };
		bounds.width = bounds.width max: minWidth;
		server = argServer;
		rate = argRate;
		bufsize = argBufsize;
		window = object = Window(this.name, bounds);
		this.initViews;
		drawSpectrogram = DrawSpectrogram(bufsize, 64, 0.5, 1, binColor, backgroundColor);
		scroll = Scroll(image, (bounds.width / 4).round(1).asInteger, backgroundColor);
		this.addImageObject(drawSpectrogram);
		this.addWindowOnCloseAction;
		this.front;
	}

	initViews {
		userview = UserView(object, object.view.bounds)
			.resize_(5)
			.drawFunc = { | view |
			var b = view.bounds;
			Pen.use {
				Pen.scale( b.width / imgWidth, b.height / imgHeight );
				Pen image: image;
			};
			penObjects do: _.update(this); 	// let objects draw with Pen here
		};
		imgWidth = userview.bounds.width;
		imgHeight = bufsize / 2;
		scrollWidth = (imgWidth / 4).round(1).asInteger;
		scrollWidth = (imgWidth * 0.25).round(1).asInteger;
		// Setting the background also creates the image
		this.background = backgroundColor; // method can be called at any time to change color
		this onClose: { image.free; image = nil; };
	}

	background_ { | color |
		// This method can be called at any time to change the background color
		if (image.notNil) { image.free };
		image = Image.color(imgWidth@imgHeight, color);
		// clearImage is an image used when scrolling to erase the right part of the screen 
	}
	
	addImageObject { | object | imageObjects = imageObjects add: object }
		
	start {
		var poller;
		poller = FFTsynthPoller(this.name, server).rate_(rate).bufSize_(bufsize);
		this.name.postln;
		poller addListener: this;
		this onClose: { poller removeListener: this }; 
		poller.addNotifier(this, this.removedMessage, { 
			if (stopOnClose) { poller.stop; } });
		poller.start;
	}
	
	// Added for symmetry, but should only be used for debugging.
	// (Normally you just close the Spectrograph window.)
	*stop { if (current.notNil) { current.stop } }
	stop { // only for debugging purposes. Normally just close the Spectrograph window.
		var poller;
		if ((poller = FFTsynthPoller.at(this.name.postln, server)).notNil) { poller.stop; };
	}

	update { | argIndex, magnitudes, fftData |
		{	// correct: in sync with data, and index protected
			if (this.isOpen) {
				argIndex = scroll.update(argIndex, image);
				drawSpectrogram.update(argIndex, image, magnitudes);
				userview .refresh;
			};
		}.defer;		
	}
}
