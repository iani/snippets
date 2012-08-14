/* IZ Tue 14 August 2012  1:44 AM EEST
Shortcuts for creating commonly used GUI items connected to Proxies and acting on them. 

See Widget, methods watchProxySpace etc

Example: 
(
Document.current.envir = ProxySpace.push;
w = Window.new;
w.layout = VLayout(
	PxMenu(w, \nodes),
	PxButton(w, \button, \nodes).states_([["start"], ["stop"]]),
	PxControlsMenu(w, \specs, \nodes),
	PxKnob(w, \knob, \specs, \numbox),
	PxNumberBox(w, \numbox, \knob)
);
w.windowHandler(w).front;
)
//:sample

~out = { | freq = 400 | SinOsc.ar(freq, 0, 0.1) };
~out.play; // after that, check the first menu of the window above, and select 'out'

// Commented version of the above: 

(
Document.current.envir = ProxySpace.push;
w = Window.new;
w.layout = VLayout(
        /*  Create a self-updating menu for selecting a node from 
            this Document's ProxySpace, an name it \nodes */
	PxMenu(w, \nodes),
        /* Create a button for starting and stopping the selected proxy,
           and make its proxy settable by menu element named \nodes */
	PxButton(w, \button, \nodes).states_([["start"], ["stop"]]),
        /* Create a menu for selecting control parameters from the selected
           node, name it \specs, make its node settable by menu \nodes */
	PxControlsMenu(w, \specs, \nodes),
        /* Create a knob named \knob, make its spec settable by menu named 
           \specs, and make it set its mapped value to \numbox */
	PxKnob(w, \knob, \specs, \numbox),
        /* Create a NumberBox named \numbox, make it set its unmapped value
           to element named \knob */
	PxNumberBox(w, \numbox, \knob)
);
w.windowHandler(w).front;
)

*/

PxWidget {
	classvar widget;
	*widget { | ... args |
		this.new(*args);
		^widget;
	}	
}

PxMenu : PxWidget { 
	*new { | model, name, proxySpace |
		^(widget = PopUpMenu()
			.addModel(model, name)
			.watchProxySpace(proxySpace)).view;
	}
}

PxControlsMenu : PxWidget {
	*new { | model, name, setterWidgetNameOrProxy |
		^(widget = PopUpMenu()
			.addModel(model, name)
			.proxyControlsMenu(setterWidgetNameOrProxy)).view;
	}
}

PxButton : PxWidget {
	*new { | model, name, setterWidgetNameOrProxy |
		^(widget = Button()
			.addModel(model, name)
			.proxyOnOffButton(setterWidgetNameOrProxy)).view;
	}
}


PxSlider : PxWidget {
	*new { | model, name, setterWidgetName, receiverWidgetName |
		^(widget = Slider()
			.addModel(model, name, receiverWidgetName)
			.getSpecsFrom(setterWidgetName)).view;	
	}
}


PxKnob : PxWidget {
	*new { | model, name, setterWidgetName, receiverWidgetName |
		^(widget = Knob()
			.addModel(model, name, receiverWidgetName)
			.getSpecsFrom(setterWidgetName)).view;	
	}
}

PxNumberBox : PxWidget {
	*new { | model, name, receiverWidgetName |
		^(widget = NumberBox().addModel(model, name, receiverWidgetName)).view
	}
}
