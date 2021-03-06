/* IZ Sat 17 November 2012  8:01 PM EET

This is a redo of SoundFileGui which saves the sound file lists as Files of a ScriptLib.
It operates directly on a ScriptLib.

Manage lists of sound file paths, saving these in user app support dir.
Selectively load files into buffers.
Display sound file contents in SoundFileView.

*/

// UNDER DEVELOPMENT!!!

ScriptLibSoundFileGui : AppModel {

	classvar <>font;
	classvar <>soundFileFolder = 'SoundFiles';

	var <>scriptLib;
	var <files; // Value holding current files list;

	*initClass {
		StartUp add: {
			{	// compatibility with 3.5
				GUI.qt;
				QtGUI.style = \CDE;
				font = Font.default.size_(10);
			}.defer(0.5);
		};
	}

	*new { | scriptLib |
		^super.new(scriptLib ?? { ScriptLib.current; }).init;
	}

	init {
		var bufferLists;
		bufferLists = this.getValue(\bufferLists, ListAdapter());
		bufferLists.items_(nil, this.getFileLists);
		files = this.getValue(\files, ListAdapter());
		files.items_(this.getFiles);
		files.sublistOf(bufferLists);
	}

	getFileLists { ^scriptLib.getFiles(soundFileFolder) }

	makeList { | argName |
		^NamedList().name_(argName ?? { Date.getDate.format("Buffer List %c") });
	}

	readDefaults { | bufferList |
		(Platform.resourceDir +/+ "sounds/*").pathMatch do: { | path |
			this.addBuffer(bufferList, BufferItem(path));
		};
		bufferList.container.updateListeners;
	}

	addBuffer { | bufferList, buffer | if (bufferList.includes(buffer).not) { bufferList add: buffer } }

	gui { this.makeWindow }

	makeWindow {
		this.stickyWindow(this.class, \bufferListGui, { | w, app |
			w.name = "Sound Files";
			w.bounds = Rect(400, 400, 1040, 650);
			w.layout = VLayout(
				HLayout(
					[VLayout(this.listButtonRow, this.selectedListDisplay, this.listListDisplay), s: 1],
					[VLayout(this.fileButtonRow, this.fileListDisplay), s: 4],
					[this.bufferDisplay, s: 2],
				),
				this.soundFileItemsRow1,
				this.soundFileDisplay,
//				GridLayout.rows(
//					[Knob(), this.soundFileDisplay],
//				).setMinColumnWidth(0, 200),
				// Here will follow the functionality items
//				[nil, s: 5],

			);
			// load current file when re-opening
			files.item !? { files.changed(\index, files); };
			this.windowClosed(w, { this.saveLists });
			ShutDown add: { this.saveLists };
		});
		Library.changed(\selectedLib); // update buffer list from ScriptLib
	}

	listButtonRow {
		^HLayout(
			StaticText().string_("Lists:").font_(font),
			this.button(\bufferLists).changedAction(\append).view.states_([["add"]]).font_(font),
			this.button(\bufferLists).changedAction(\insert).view.states_([["insert"]]).font_(font),
			this.button(\bufferLists).changedAction(\rename).view.states_([["rename"]]).font_(font),
		)
	}

	selectedListDisplay {
		^this.listItem(\bufferLists, TextField(), { | me |
			me.value.adapter.item !? { me.value.adapter.item.name; }
		})
		.updateAction(\rename, { | sender, me |
			me.value.adapter.item.name = me.view.string;
			me.value.updateListeners;
		})
		.action_({ | me |
			me.value.adapter.item.name = me.view.string;
			me.value.updateListeners;
		})
		.appendOn({ this.makeList })
		.insertOn({ this.makeList }).view.font_(font)
	}

	listListDisplay {
		^this.listView(\bufferLists, { | me |
			me.value.adapter.items collect: _.name
		})
		.view.font_(font)
	}

	fileButtonRow {
		^HLayout(
			this.button(\bufferLists).action_({ | me | me.value.adapter.delete })
				.view.states_([["delete"]]).font_(font),
			Button().action_({
				this.init(/* archivePath */);
				this.updateListeners;
			}).states_([["revert"]]).font_(font),
			Button().action_({ this.saveLists }).states_([["save"]]).font_(font),
			StaticText().string_("Sound Files:").font_(font),
			this.button(\files).changedAction(\readNew).view.states_([["read new"]]).font_(font),
			this.button(\files).changedAction(\readDefaults)
			.view.states_([["read defaults"]]).font_(font),
			this.button(\files).changedAction(\loadSelected)
			.view.states_([["load selected"]]).font_(font),
			this.button(\files).changedAction(\loadAll).view.states_([["load all"]]).font_(font),
			this.button(\files).changedAction(\delete).view.states_([["delete"]]).font_(font),
		)
	}

	fileListDisplay {
		^this.listView(\files, { | me |
			me.value.adapter.items collect: _.name
		})
			.updateAction(\readNew, { | me |
				Dialog.openPanel({ | paths |
					paths do: { | p | this.addBuffer(me.value.adapter, BufferItem(p)) };
					me.value.updateListeners;
				}, multipleSelection: true);
			})
			.updateAction(\readDefaults, { | me | this.readDefaults(me.value.adapter) })
			.updateAction(\loadAll, { | me |
				me.value.adapter.items do: _.loadIfNeeded;
			})
			.updateAction(\loadSelected, { | me |
				me.value.adapter.item !? { me.value.adapter.item.load }
			})
			.updateAction(\delete, { | me | me.value.adapter.delete(me); })
			.updateAction(\free, { | me | me.value.adapter.item.free })
			.view.font_(font)
	}

	bufferDisplay {
		^GridLayout.rows(
			[
				Button().action_({ ScriptLib.current.gui; })
				.font_(font).states_([["current lib buffer config:"]]),
				this.bufferListHeader
			],
			[this.scriptLibList, this.loadedBuffersList],
		)
	}

	bufferListHeader {
		^HLayout(
			StaticText().string_("Loaded buffers:").font_(font),
			this.button(\loadedBuffers).changedAction(\free).view.states_([["free"]]).font_(font),
		)
	}

	loadedBuffersList {
		^this.listView(\loadedBuffers).updater(BufferItem, \bufferList, { | me, names |
			me.value.adapter.items_(nil, names)
		})
		.items_(Library.at['Buffers'].keys.asArray.sort)
		.updateAction(\free, { | me |
			Library.at('Buffers', me.value.adapter.item).free;
		})
		.addAction({ | me |
				this.showLoadedBuffer(BufferItem.getBuffer(this.getValue(\loadedBuffers).item))
		})
		.view.font_(font)
	}

	scriptLibList {
		^VLayout(
			HLayout(
				this.button(\files).action_({ | me |
					ScriptLib.current.addBuffer(me.item);
					this.getValue(\scriptLibBuffers).item_(nil, me.item.nameSymbol);
				})
				.view.font_(font).states_([["+"]]),
				this.button(\files).action_({ | me |
					me.items do: { | item |
					ScriptLib.current.addBuffer(item);
					};
//					this.getValue(\scriptLibBuffers).item_(nil, me.item.nameSymbol);
				})
				.view.font_(font).states_([["+*"]]),
				this.button(\scriptLibBuffers).action_({ | me |
					ScriptLib.current.removeSoundFile(me.item);
				})
				.view.font_(font).states_([["-"]]),
				this.button(\scriptLibBuffers).action_({ | me |
					me.items do: { | item | ScriptLib.current.removeSoundFile(item); }
				})
				.view.font_(font).states_([["-*"]]),
			),
			this.listView(\scriptLibBuffers)
			.updater(Library, \selectedLib, { | me |
				me.items = ScriptLib.current.buffers.keys.asArray.sort;
			})
			.addAction({ | me |
				this.showLoadedBuffer(BufferItem.getBuffer(this.getValue(\scriptLibBuffers).item))
			})
			.view.font_(font)
		);
	}

	showLoadedBuffer { | bufferItem |
		this.getValue(\soundFileView).adapter.soundFile = bufferItem.name.asString
	}

	soundFileDisplay {
		^this.soundFileView(\soundFileView)
		.viewGetter(\sfView)	// provide view to other widgets for extra actions
		.updater(files, \index, { | me, list |
			list.item !? { me.value.adapter.soundFile_(list.item.name); }
		})
		.view.timeCursorOn_(true);
	}

	soundFileItemsRow1 {
		^HLayout(
			[StaticText().string_("num frames:").font_(font), s: 2],
			this.numberBox(\soundFileView)
			.updateAction(\read, { | sf, me |
				sf.soundFile !? { me.view.value = sf.soundFile.numFrames }
			})
			.view.font_(font),
			[StaticText().string_("sample rate:").font_(font), s: 2],
			[this.numberBox(\soundFileView)
			.updateAction(\read, { | sf, me |
				sf.soundFile !? { me.view.value = sf.soundFile.sampleRate }
			})
			.view.font_(font), s: 1],
			StaticText().string_("duration:").font_(font),
			[this.numberBox(\soundFileView)
			.updateAction(\read, { | sf, me |
				sf.soundFile !? { me.view.value = sf.soundFile.duration }
			})
			.view.font_(font), s: 1],
			StaticText().string_("cursor:").font_(font),
			this.numberBox(\soundFileView)
			.updateAction(\sfViewAction, { | sfv, me |
				sfv.view.soundfile !? { me.view.value = sfv.view.timeCursorPosition; }
			})
			.view.font_(font),
			StaticText().string_("time:").font_(font),
			[this.numberBox(\soundFileView)
			.updateAction(\sfViewAction, { | sfv, me |
				sfv.view.soundfile !? {
					me.view.value = sfv.view.timeCursorPosition / sfv.view.soundfile.sampleRate;
				}
			})
			.view.font_(font), s: 1],
			[StaticText().string_("selected frames:").font_(font), s: 2],
			this.numberBox(\soundFileView)
			.updateAction(\sfViewAction, { | sfv, me |
				sfv.view.soundfile !? {
					me.view.value = sfv.view.selectionSize(sfv.view.currentSelection);
				}
			})
			.view.font_(font),
			[StaticText().string_("selected dur.:").font_(font), s: 2],
			[this.numberBox(\soundFileView)
			.updateAction(\sfViewAction, { | sfv, me |
				sfv.view.soundfile !? {
					me.view.value =
						sfv.view.selectionSize(sfv.view.currentSelection) /
						sfv.view.soundfile.sampleRate;
				}
			})
			.view.font_(font), s: 1],
			this.button(\soundFileView).action_({ | me |
				me.getView(\sfView).soundfile.cue((), playNow: true, closeWhenDone: true)
			})
			.view.states_([["play"]]).font_(font),
			this.button(\soundFileView)
			.action_({ | me |
				var sfv, selection, firstFrame, lastFrame;
				sfv = me.getView(\sfView);
				selection = sfv.currentSelection;
				firstFrame = sfv.selectionStart(selection);
				lastFrame = sfv.selectionStart(selection) + sfv.selectionSize(selection);
				if (lastFrame <= firstFrame) { lastFrame = sfv.soundfile.numFrames };
				sfv.soundfile.cue(
				(
					firstFrame: firstFrame,
					lastFrame: lastFrame,
				), playNow: true, closeWhenDone: true)
			})
			.view.states_([["play sel"]]).font_(font),
		)
	}

	saveLists {
//		this.getValue(\bufferLists).adapter.items.writeArchive(archivePath);
//		postf("Buffer lists saved to: \n%\n", archivePath);
	}

	bufferPlayCode {
		^
"var buffer;
buffer = '%'.b;
Ndef('%', {
	PlayBuf.ar(buffer.numChannels, buffer)
}).play;"
	}
}
