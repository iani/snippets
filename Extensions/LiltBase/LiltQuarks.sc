/*


Manage local quarks of lilt library:
- Create 3 menu items for Configuring core, project and draft quarks
- Modify window height to fit the list of all quarks in a directory better. 

Note: Since the UserExtensionsDirectory is now ignored by the git configuration of Lilt2, quark installation works in the standard way through symbolic links. There is no need to copy or remove the quarks files to the Extensions. 

*/
LiltQuarks : Quarks {
	
	*initClass {
		Class.initClassTree(MenuHandler);	
		this.addMenu;	
	}
	
	*menuItems {
		^[
			CocoaMenuItem.addToMenu("Code", "Configure core quarks", nil, {
				LiltQuarks(localPath: Platform.userAppSupportDir +/+ "quarks.local.core").gui;
			}),
			CocoaMenuItem.addToMenu("Code", "Configure project quarks", nil, {
				LiltQuarks(localPath: Platform.userAppSupportDir +/+ "quarks.local.projects").gui;
			}),
			CocoaMenuItem.addToMenu("Code", "Configure draft quarks", nil, {
				LiltQuarks(localPath: Platform.userAppSupportDir +/+ "quarks.local.drafts").gui;
			}),
		]
	}

	// a gui for Quarks. 2007 by LFSaw.de
	// Mod of window height to fit all quarks list by IZ 201108
	gui {
		var	window, caption, explanation, views, resetButton, saveButton, warning,
			scrollview, scrB, flowLayout, /* quarksflow, */ height, maxPerPage, nextButton, prevButton;
		var	quarks;
		var pageStart = 0, fillPage = { |start|
			scrollview.visible = false;
			views.notNil.if({
				views.do({ |view| view.remove });
			});
			scrollview.decorator.reset;
			views = quarks collect: { | quark |
				var qView = QuarkView.new(scrollview, 500@20, quark,
					this.installed.detect{|it| it == quark}.notNil);
				scrollview.decorator.nextLine;
				qView;
			};
			scrollview.visible = true;
			views
		};

		// note, this doesn't actually contact svn
		// it only reads the DIRECTORY entries you've already checked out
		quarks = this.repos.quarks.copy
			.sort({ |a, b| a.name < b.name });

		scrB = GUI.window.screenBounds;
//		height = min(quarks.size * 25 + 120, scrB.height - 60);
//		IZ mod: 
		height = min(quarks.size.postln * 25 + 170, scrB.height - 60);
		window = GUI.window.new(this.name, Rect.aboutPoint( scrB.center, 250, height.div( 2 )));
		flowLayout = FlowLayout( window.view.bounds );
		window.view.decorator = flowLayout;

		caption = GUI.staticText.new(window, Rect(20,15,400,30));
		caption.font_( GUI.font.new( GUI.font.defaultSansFace, 24 ));
		caption.string = this.name;
		window.view.decorator.nextLine;

		if ( quarks.size == 0 ){
			GUI.button.new(window, Rect(0, 0, 229, 20))
			.states_([["checkout Quarks DIRECTORY", Color.black, Color.gray(0.5)]])
			.action_({ this.checkoutDirectory; });
		}{
			GUI.button.new(window, Rect(0, 0, 229, 20))
			.states_([["update Quarks DIRECTORY", Color.black, Color.gray(0.5)]])
			.action_({ this.updateDirectory;});
		};

		GUI.button.new(window, Rect(0, 0, 200, 20))
		.states_([["refresh Quarks listing", Color.black, Color.gray(0.5)]])
		.action_({
			window.close;
			this.gui;
		});

		window.view.decorator.nextLine;

		GUI.button.new(window, Rect(0, 0, 150, 20))
			.states_([["browse all help", Color.black, Color.gray(0.5)]])
			.action_({ Help(this.local.path).gui });

		// add open directory button (open is only implemented in OS X)
		(thisProcess.platform.name == \osx).if{
			GUI.button.new(window, Rect(15,15,150,20)).states_([["open quark directory", Color.black, Color.gray(0.5)]]).action_{ arg butt;
				"open %".format(this.local.path.escapeChar($ )).unixCmd;
			};
		};

		resetButton = GUI.button.new(window, Rect(15,15,75,20));
		resetButton.states = [
			["reset", Color.black, Color.gray(0.5)]
		];
		resetButton.action = { arg butt;
			views.do(_.reset);
		};

		saveButton = GUI.button.new(window, Rect(15,15,75,20));
		saveButton.states = [
			["save", Color.black, Color.blue(1, 0.5)]
		];
		saveButton.action = { arg butt;
			Task{
				warning.string = "Applying changes, please wait";
				warning.background_(Color(1.0, 1.0, 0.9));
				0.1.wait;
				views.do{ | qView |
					qView.toBeInstalled.if({
						this.install(qView.quark.name);
						qView.flush
					});
					qView.toBeDeinstalled.if({
						this.uninstall(qView.quark.name);
						qView.flush;
					})
				};
				warning.string = "Done. You should now recompile sclang";
				warning.background_(Color(0.9, 1.0, 0.9));
			}.play(AppClock);
		};

		window.view.decorator.nextLine;
		explanation = GUI.staticText.new(window, Rect(20,15,500,20));
		explanation.string = "\"+\" -> installed, \"-\" -> not installed, \"*\" -> marked to install, \"x\" -> marked to uninstall";
		window.view.decorator.nextLine;

		warning = GUI.staticText.new(window, Rect(20,15,400,30));
		warning.font_( GUI.font.new( GUI.font.defaultSansFace, 18 ));

		window.view.decorator.nextLine;
		GUI.staticText.new( window, 492 @ 1 ).background_( Color.grey );		window.view.decorator.nextLine;

		flowLayout.margin_( 0 @0 ).gap_( 0@0 );
		scrollview = GUI.scrollView.new(window, 500 @ (height - 165))
			.resize_( 5 )
			.autohidesScrollers_(true);
		scrollview.decorator = FlowLayout( Rect( 0, 0, 500, quarks.size * 25 + 20 ));

		window.front;
		fillPage.(pageStart);
		^window;
	}

}