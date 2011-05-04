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
	var <bounds, <server, <rate, <bufsize, <>stopOnClose = true;
	var <userview, <image, <imgWidth, <imgHeight;
	var <scrollWidth, scrollImage, clearImage;
	var <index;	// running count of the currently polled fft frame. 
				// received from FFTsynthPoller. Cached for asynchronous use by penObjects
	var <windowIndex;	// index of x pixel on image where current frame is being drawn

	var <imageObjects; // array of objecs that add display graphics to pixels on the image
	var <penObjects; // array of objecs that add display graphics using Pen
	var <drawSpectrogram, <drawCrosshair;	// the two built-in drawing objects
	
	var <persistentWindowIndex;	// needed for frame drawing sync????
	
	*start { | name, bounds, server, rate = 0.025, bufsize = 1024 |
		^this.new	(name, bounds, server ? Server.default, rate, bufsize = 1024).start;
	}
	
	*new { | name, bounds, server, rate = 0.025, bufsize = 1024 |
		name = format("%:%", name = name ? "Spectrograph", server = server ? Server.default).asSymbol;
		^current = super.new(name, bounds, server, rate, bufsize);
	}

	name { ^key[1] }
	
	init { | argBounds, argServer, argRate, argBufsize |
		var window; // just for naming convenience
		bounds = argBounds ?? { Window.centeredWindowBounds(1000) };
		server = argServer;
		rate = argRate;
		bufsize = argBufsize;
		window = object = Window(this.name, bounds);
		this.initViews;
		drawSpectrogram = DrawSpectrogram(bufsize, 64, 0.5, 1, Color.white, Color.black);
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
		// Scroll image is an array used when scrolling to copy the already drawn pixels 
		// from the right part of the image to the left part
		scrollImage = Int32Array.fill(imgHeight * (imgWidth - scrollWidth), 0);
		this.background = Color.black; // method can be called at any time to change color
		this onClose: { image.free; };
	}

	background_ { | color |
		// This method can be called at any time to change the background color
		if (image.notNil) { image.free };
		image = Image.color(imgWidth@imgHeight, color);
		// clearImage is an image used when scrolling to erase the right part of the screen 
		clearImage = Int32Array.fill(imgHeight * scrollWidth, Integer.fromColor(color));
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
		if ((poller = FFTsynthPoller.at(this.name.postln, server)).postln.notNil) { poller.stop; };
	}

	update { | argIndex, magnitudes, fftData |
		windowIndex = argIndex;
		if (windowIndex >= imgWidth) {
			windowIndex = windowIndex % scrollWidth; 
			if (windowIndex == 0) {	// the frame has reached the rightmost end of the drawing window ...
			// ... so scroll the rest of the image to the left
				image.loadPixels(scrollImage, Rect(scrollWidth, 0, imgWidth - scrollWidth, imgHeight), 0);
				image.setPixels(scrollImage, Rect(0, 0, imgWidth - scrollWidth, imgHeight), 0); 
				image.setPixels(clearImage, Rect(imgWidth - scrollWidth, 0, scrollWidth, imgHeight), 0);
			};
			windowIndex = windowIndex + (imgWidth - scrollWidth);
		};

		persistentWindowIndex = windowIndex;
/*		currentFFTframeMagnitudes = Complex(
				Signal.newFrom(magarray[0]), Signal.newFrom(magarray[1])
			).magnitude;
		currentFFTframeMagnitudesReversed = currentFFTframeMagnitudes.reverse;
		complexarray = log10(1 + currentFFTframeMagnitudesReversed).clip(0, 1) * intensity;
		complexarray.do({ | val, i |
			fftDataArray[i] = colints.clipAt((val * colorSize).round);
		});
*/
		{	// correct: in sync with data, and index protected
			drawSpectrogram.update(image, persistentWindowIndex, magnitudes);
//			image.setPixels(fftDataArray, Rect(persistentWindowIndex, 0, 1, fftDataArray.size));
			userview.refresh;	
		}.defer;		
	}

}

/*

	update { | argIndex, magnitudes, fftData |
		// FFT data received from FFTsynthPoller. Draw graphics and refresh user view.
		// Received from the FFTsynthPoller each time an fft frame is polled.
		index = argIndex;
//		postf("% updating: %, %, %\n", this.name, index, fftData.size, magnitudes.size);
		{ 
			this.scroll(index);
			imageObjects do: _.update(image, windowIndex, magnitudes);
			if (userview.notClosed) { userview.refresh };
		}.defer;
	}

	scroll { | index |
		windowIndex = index;
		if (windowIndex >= imgWidth) {
			windowIndex = windowIndex % scrollWidth; 
			if (windowIndex == 0) {	// the frame has reached the rightmost end of the drawing window ...
			// ... so scroll the rest of the image to the left
//				"SCROLLING".postln;
				image.loadPixels(scrollImage, Rect(scrollWidth, 0, imgWidth - scrollWidth, imgHeight), 0);
				image.setPixels(scrollImage, Rect(0, 0, imgWidth - scrollWidth, imgHeight), 0); 
				image.setPixels(clearImage, Rect(imgWidth - scrollWidth, 0, scrollWidth, imgHeight), 0);
			};
			windowIndex = windowIndex + (imgWidth - scrollWidth);
		};
	}		

*/