/*******************************************************************************
 * Copyright (c) 2005-2007 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.nico.ui.console;

import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

import de.walware.eclipsecommons.ui.util.PixelConverter;
import de.walware.eclipsecommons.ui.util.UIAccess;

import de.walware.statet.base.ui.IStatetUICommandIds;
import de.walware.statet.base.ui.StatetUIServices;
import de.walware.statet.base.ui.util.ISettingsChangedHandler;
import de.walware.statet.ext.ui.editors.DeleteLineAction;
import de.walware.statet.ext.ui.editors.GotoMatchingBracketAction;
import de.walware.statet.ext.ui.editors.IEditorAdapter;
import de.walware.statet.ext.ui.editors.IEditorInstallable;
import de.walware.statet.ext.ui.editors.SourceViewerConfigurator;
import de.walware.statet.ext.ui.editors.SourceViewerUpdater;
import de.walware.statet.ext.ui.editors.TextViewerAction;
import de.walware.statet.ext.ui.text.PairMatcher;
import de.walware.statet.nico.core.runtime.History;
import de.walware.statet.nico.core.runtime.IHistoryListener;
import de.walware.statet.nico.core.runtime.Prompt;
import de.walware.statet.nico.core.runtime.SubmitType;
import de.walware.statet.nico.core.runtime.ToolController;
import de.walware.statet.nico.core.runtime.ToolProcess;
import de.walware.statet.nico.core.runtime.History.Entry;
import de.walware.statet.nico.internal.ui.Messages;
import de.walware.statet.nico.internal.ui.NicoUIPlugin;


public class InputGroup implements ISettingsChangedHandler {

	
	final static int KEY_HIST_UP = SWT.ARROW_UP;
	final static int KEY_HIST_DOWN = SWT.ARROW_DOWN;
	final static int KEY_HIST_SPREFIX_UP = SWT.ALT | SWT.ARROW_UP;
	final static int KEY_HIST_SPREFIX_DOWN = SWT.ALT | SWT.ARROW_DOWN;
	final static int KEY_SUBMIT_DEFAULT = SWT.CR;
	final static int KEY_SUBMIT_KEYPAD = SWT.KEYPAD_CR;
	
	final static int KEY_OUTPUT_LINEUP = SWT.SHIFT | SWT.ARROW_UP;
	final static int KEY_OUTPUT_LINEDOWN = SWT.SHIFT | SWT.ARROW_DOWN;
	final static int KEY_OUTPUT_PAGEUP = SWT.SHIFT | SWT.PAGE_UP;
	final static int KEY_OUTPUT_PAGEDOWN = SWT.SHIFT | SWT.PAGE_DOWN;
	final static int KEY_OUTPUT_START = SWT.MOD1 | SWT.SHIFT | SWT.HOME;
	final static int KEY_OUTPUT_END = SWT.MOD1 | SWT.SHIFT | SWT.END;
	
	
	
	private final class ScrollControl implements Listener {
		
		private static final int MAX = 150;
		private static final int SPECIAL = 80;
		private final Slider fSlider;
		private int fLastPos = 0;
		
		private ScrollControl(Slider slider) {
			fSlider = slider;
			fSlider.setMaximum(MAX);
			fSlider.addListener(SWT.Selection, this);
		}
		
		public void handleEvent(Event event) {
			final int selection = fSlider.getSelection();
			int newIndex;
			StyledText output = fConsolePage.getOutputViewer().getTextWidget();
			StyledText input = fSourceViewer.getTextWidget();
			int current = Math.max(output.getHorizontalIndex(), input.getHorizontalIndex());
			if (event.detail == SWT.DRAG) {
				if (fLastPos < 0) {
					fLastPos = current;
				}
				if (current > SPECIAL || fLastPos > SPECIAL) {
					current = fLastPos;
					double diff = selection-SPECIAL;
					newIndex = current + (int) (
							Math.signum(diff) * Math.max(Math.exp(Math.abs(diff)/7.5)-1.0, 0.0) * 2.0 );
					if (newIndex < selection) {
						newIndex = selection;
					}
				}
				else {
					newIndex = selection;
				}
			}
			else {
				if (current > SPECIAL) {
					newIndex = current + selection-SPECIAL;
				}
				else {
					newIndex = selection;
				}
			}
			output.setHorizontalIndex(newIndex);
			input.setHorizontalIndex(newIndex);
			// System.out.println(newIndex);
			current = Math.max(output.getHorizontalIndex(), input.getHorizontalIndex());
			if (event.detail != SWT.DRAG) {
				fSlider.setSelection(current > 80 ? 80 : current);
				fLastPos = -1;
			}
		}
		
		public void reset() {
			fSlider.setSelection(0);
			StyledText output = fConsolePage.getOutputViewer().getTextWidget();
			output.setHorizontalIndex(0);
			fLastPos = -1;
		}
	}

	private class EditorAdapter implements IEditorAdapter {

		private boolean fMessageSetted;
		
		public SourceViewer getSourceViewer() {
			return fSourceViewer;
		}
		
		public void setStatusLineErrorMessage(String message) {
			IStatusLineManager manager = fConsolePage.getSite().getActionBars().getStatusLineManager();
			if (manager != null) {
				fMessageSetted = true;
				manager.setErrorMessage(message);
			}
		}
		
		public boolean isEditable(boolean validate) {
			return true;
		}
		
		protected void cleanStatusLine() {
			if (fMessageSetted) {
				IStatusLineManager manager = fConsolePage.getSite().getActionBars().getStatusLineManager();
				if (manager != null) {
					manager.setErrorMessage(null);
					fMessageSetted = false;
				}
				
			}
		}
	}
	
	private class ThisKeyListener implements VerifyKeyListener {

		public void verifyKey(VerifyEvent e) {
			int key = (e.stateMask | e.keyCode);
			switch (key) {
			case KEY_HIST_UP:
				doHistoryOlder(null);
				break;
			case KEY_HIST_DOWN:
				doHistoryNewer(null);
				break;
			case KEY_SUBMIT_DEFAULT:
			case KEY_SUBMIT_KEYPAD:
				doSubmit();
				break;

			case KEY_OUTPUT_LINEUP:
				doOutputLineUp();
				break;
			case KEY_OUTPUT_LINEDOWN:
				doOutputLineDown();
				break;
			case KEY_OUTPUT_PAGEUP:
				doOutputPageUp();
				break;
			case KEY_OUTPUT_PAGEDOWN:
				doOutputPageDown();
				break;
			default:
				// non-constant values
				if (key == KEY_OUTPUT_START) {
					doOutputFirstLine();
					break;
				}
				if (key == KEY_OUTPUT_END) {
					doOutputLastLine();
					break;
				}
				// no special key
				return;
			}
			e.doit = false;
		}

		public void keyReleased(KeyEvent e) {
		}
		
		private void doOutputLineUp() {
			StyledText output = (StyledText) fConsolePage.getOutputViewer().getControl();
			int next = output.getTopIndex() - 1;
			if (next < 0) {
				return;
			}
			output.setTopIndex(next);
		}

		private void doOutputLineDown() {
			StyledText output = (StyledText) fConsolePage.getOutputViewer().getControl();
			int next = output.getTopIndex() + 1;
			if (next >= output.getLineCount()) {
				return;
			}
			output.setTopIndex(next);
		}
		
		private void doOutputPageUp() {
			StyledText output = (StyledText) fConsolePage.getOutputViewer().getControl();
			int current = output.getTopIndex();
			int move = Math.max(1, (output.getClientArea().height / output.getLineHeight()) - 1);
			int next = Math.max(0, current - move);
			if (next == current) {
				return;
			}
			output.setTopIndex(next);
		}

		private void doOutputPageDown() {
			StyledText output = (StyledText) fConsolePage.getOutputViewer().getControl();
			int current = output.getTopIndex();
			int move = Math.max(1, (output.getClientArea().height / output.getLineHeight()) - 1);
			int next = Math.min(current + move, output.getLineCount() - 1);
			if (next == current) {
				return;
			}
			output.setTopIndex(next);
		}

		private void doOutputFirstLine() {
			StyledText output = (StyledText) fConsolePage.getOutputViewer().getControl();
			int next = 0;
			output.setTopIndex(next);
		}

		private void doOutputLastLine() {
			StyledText output = (StyledText) fConsolePage.getOutputViewer().getControl();
			int next = output.getLineCount()-1;
			if (next < 0) {
				return;
			}
			output.setTopIndex(next);
		}
		
	}

	private NIConsolePage fConsolePage;
	private ToolProcess fProcess;
	private History.Entry fCurrentHistoryEntry;
	private IHistoryListener fHistoryListener;
	
	private Composite fComposite;
	private Label fPrefix;
	private InputSourceViewer fSourceViewer;
	protected InputDocument fDocument;
	private Button fSubmitButton;
	private ScrollControl fScroller;
	
	EditorAdapter fEditorAdapter = new EditorAdapter();
	IEditorInstallable[] fInstalledModules;
	private SourceViewerDecorationSupport fSourceViewerDecorationSupport;
	private SourceViewerConfigurator fConfigurator;

	/**
	 * Saves the selection before starting a history navigation session.
	 * non-null value indicates in history session */
	private Point fHistoryCompoundChange;
	/** There are no caret listener */
	private Point fHistoryCaretWorkaround;
	/** Indicates that the document is change by a history action */
	private boolean fInHistoryChange = false;
	

	public InputGroup(NIConsolePage page) {
		fConsolePage = page;
		fProcess = page.getConsole().getProcess();
		
		fDocument = new InputDocument();
	}

	public Composite createControl(Composite parent, SourceViewerConfigurator editorConfig) {
		fComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 3;
		fComposite.setLayout(layout);
		
		fPrefix = new Label(fComposite, SWT.LEFT);
		GridData gd = new GridData(SWT.LEFT, SWT.FILL, false, true);
		gd.verticalIndent = 1;
		fPrefix.setLayoutData(gd);
		float[] hsb = fPrefix.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB().getHSB();
		fPrefix.setBackground(StatetUIServices.getSharedColorManager().getColor(new RGB(hsb[0], hsb[1], 0.925f)));
		fPrefix.setText("> "); //$NON-NLS-1$
		
		createSourceViewer(editorConfig);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.verticalIndent = 1;
		fSourceViewer.getControl().setLayoutData(gd);
		fSourceViewer.appendVerifyKeyListener(new ThisKeyListener());
		
		fSubmitButton = new Button(fComposite, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd.horizontalIndent = layout.verticalSpacing;
		gd.heightHint = new PixelConverter(fSubmitButton).convertHeightInCharsToPixels(1)+4;
		gd.verticalSpan = 2;
		fSubmitButton.setLayoutData(gd);
		fSubmitButton.setText(Messages.Console_SubmitButton_label);
		fSubmitButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				doSubmit();
				fSourceViewer.getControl().setFocus();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Slider slider = new Slider(fComposite, SWT.HORIZONTAL);
		slider.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		fScroller = new ScrollControl(slider);
		
		setFont(fConsolePage.getConsole().getFont());
		
		fHistoryListener = new IHistoryListener() {
			public void entryAdded(History source, Entry e) {
			}
			public void entryRemoved(History source, Entry e) {
			}
			public void completeChange(History source, Entry[] es) {
				fCurrentHistoryEntry = null;
			}
		};
		fProcess.getHistory().addListener(fHistoryListener);
		
		fDocument.addPrenotifiedDocumentListener(new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {
				if (fHistoryCompoundChange != null && !fInHistoryChange) {
					fHistoryCompoundChange = null;
					fSourceViewer.getUndoManager().endCompoundChange();
				}
			}
			public void documentChanged(DocumentEvent event) {
			}
		});
		return fComposite;
	}
	
	protected void createSourceViewer(SourceViewerConfigurator editorConfigurator) {
		fConfigurator = editorConfigurator;
		fSourceViewer = new InputSourceViewer(fComposite);
		fConfigurator.setTarget(fSourceViewer, true);
		
		fSourceViewerDecorationSupport = new SourceViewerDecorationSupport(
				fSourceViewer, null, null, EditorsUI.getSharedTextColors());
		fConfigurator.configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
		fSourceViewerDecorationSupport.install(fConfigurator.getPreferenceStore());
		
		IDocumentSetupParticipant docuSetup = fConfigurator.getDocumentSetupParticipant();
		if (docuSetup != null) {
			docuSetup.setup(fDocument.getMasterDocument());
		}
		
		new SourceViewerUpdater(fSourceViewer, fConfigurator.getSourceViewerConfiguration());

		fSourceViewer.setDocument(fDocument);
		fSourceViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fEditorAdapter.cleanStatusLine();
			}
		});
		fSourceViewer.getTextWidget().addListener(SWT.FocusOut, new Listener() {
			public void handleEvent(Event event) {
				fEditorAdapter.cleanStatusLine();
			}
		});
		fSourceViewer.removeSpecialBinding(KEY_HIST_UP);
		fSourceViewer.removeSpecialBinding(KEY_HIST_DOWN);
		fSourceViewer.removeSpecialBinding(KEY_SUBMIT_DEFAULT);
		fSourceViewer.removeSpecialBinding(KEY_SUBMIT_KEYPAD);
		fSourceViewer.removeSpecialBinding(KEY_OUTPUT_LINEUP);
		fSourceViewer.removeSpecialBinding(KEY_OUTPUT_LINEDOWN);
		fSourceViewer.removeSpecialBinding(KEY_OUTPUT_PAGEUP);
		fSourceViewer.removeSpecialBinding(KEY_OUTPUT_PAGEDOWN);
		fSourceViewer.removeSpecialBinding(KEY_OUTPUT_START);
		fSourceViewer.removeSpecialBinding(KEY_OUTPUT_END);
		
		fInstalledModules = fConfigurator.getSourceViewerConfiguration().getInstallables();
		for (int i = 0; i < fInstalledModules.length; i++) {
			fInstalledModules[i].install(fEditorAdapter);
		}
	}
	
	public boolean handleSettingsChanged(Set<String> contexts, Object options) {
		fConfigurator.handleSettingsChanged(contexts, options);
		return false;
	}
	
	public void configureServices(IHandlerService commands, IContextService keys) {
		keys.activateContext("de.walware.statet.base.contexts.ConsoleEditor"); //$NON-NLS-1$
		
		IAction action;
		PairMatcher matcher = fConfigurator.getPairMatcher();
		if (matcher != null) {
			action = new GotoMatchingBracketAction(matcher, fEditorAdapter);
			commands.activateHandler(IStatetUICommandIds.GOTO_MATCHING_BRACKET, new ActionHandler(action));
		}
		
		action = new DeleteLineAction(fEditorAdapter, DeleteLineAction.WHOLE, false);
		commands.activateHandler(action.getActionDefinitionId(), new ActionHandler(action));
		action = new DeleteLineAction(fEditorAdapter, DeleteLineAction.TO_BEGINNING, false);
		commands.activateHandler(action.getActionDefinitionId(), new ActionHandler(action));
		action = new DeleteLineAction(fEditorAdapter, DeleteLineAction.TO_END, false);
		commands.activateHandler(action.getActionDefinitionId(), new ActionHandler(action));
		action = new DeleteLineAction(fEditorAdapter, DeleteLineAction.WHOLE, true);
		commands.activateHandler(action.getActionDefinitionId(), new ActionHandler(action));
		action = new DeleteLineAction(fEditorAdapter, DeleteLineAction.TO_BEGINNING, true);
		commands.activateHandler(action.getActionDefinitionId(), new ActionHandler(action));
		action = new DeleteLineAction(fEditorAdapter, DeleteLineAction.TO_END, true);
		commands.activateHandler(action.getActionDefinitionId(), new ActionHandler(action));

		action = new TextViewerAction(fEditorAdapter.getSourceViewer(), ISourceViewer.CONTENTASSIST_PROPOSALS);
		commands.activateHandler(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new ActionHandler(action));

		commands.activateHandler("de.walware.statet.nico.commands.SearchHistoryOlder", new AbstractHandler() { //$NON-NLS-1$
			@Override
			public Object execute(ExecutionEvent arg0)
					throws ExecutionException {
				doHistoryOlder(getLineStart());
				return null;
			}
		});
		commands.activateHandler("de.walware.statet.nico.commands.SearchHistoryNewer", new AbstractHandler() { //$NON-NLS-1$
			@Override
			public Object execute(ExecutionEvent arg0)
					throws ExecutionException {
				doHistoryNewer(getLineStart());
				return null;
			}
		});
		commands.activateHandler("de.walware.statet.nico.commands.GotoHistoryNewest", new AbstractHandler() { //$NON-NLS-1$
			@Override
			public Object execute(ExecutionEvent arg0)
					throws ExecutionException {
				doHistoryNewest();
				return null;
			}
		});
	}
	
	
	void setFont(Font font) {
		fPrefix.setFont(font);
		fSourceViewer.getControl().setFont(font);
	}
	
	/**
	 * @param prompt new prompt or null.
	 */
	public void updatePrompt(final Prompt prompt) {
		UIAccess.getDisplay().syncExec(new Runnable() {
			public void run() {
				if (UIAccess.isOkToUse(fPrefix)) {
					Prompt p = (prompt != null) ? prompt : fProcess.getWorkspaceData().getPrompt();
					fPrefix.setText(p.text);
					getComposite().layout(new Control[] { fPrefix });
					onPromptUpdate(p);
				}
			}
		});
	}
	
	protected void onPromptUpdate(Prompt prompt) {
	}
	
	private String getLineStart() {
		try {
			return fDocument.get(0, fSourceViewer.getSelectedRange().x);
		} catch (BadLocationException e) {
			NicoUIPlugin.logError(-1, "Error while extracting prefix for history search", e); //$NON-NLS-1$
			return "";
		}
	}
	
	public void doHistoryNewer(String prefix) {
		if (fCurrentHistoryEntry == null) {
			return;
		}
		
		History.Entry next = fCurrentHistoryEntry.getNewer();
		while (next != null
				&& (next.isEmpty() || (prefix != null && !next.getCommand().startsWith(prefix)) )) {
			next = next.getNewer();
		}
		
		if (next == null && prefix != null) {
			Display.getCurrent().beep();
			return;
		}

		fCurrentHistoryEntry = next;
		setHistoryContent((fCurrentHistoryEntry != null) ?
				fCurrentHistoryEntry.getCommand() : "");
	}
	
	public void doHistoryOlder(String prefix) {
		History.Entry next;
		if (fCurrentHistoryEntry != null) {
			next = fCurrentHistoryEntry.getOlder();
		}
		else {
			next = fProcess.getHistory().getNewest();
		}
		while (next != null
				&& (next.isEmpty() || (prefix != null && !next.getCommand().startsWith(prefix)) )) {
			next = next.getOlder();
		}
		
		if (next == null) {
			Display.getCurrent().beep();
			return;
		}
		
		fCurrentHistoryEntry = next;
		setHistoryContent(fCurrentHistoryEntry.getCommand());
	}
	
	public void doHistoryNewest() {
		History.Entry next = fProcess.getHistory().getNewest();
		if (next == null) {
			Display.getCurrent().beep();
			return;
		}
		fCurrentHistoryEntry = next;
		setHistoryContent(fCurrentHistoryEntry.getCommand());
	}

	protected void setHistoryContent(String text) {
		if (fHistoryCompoundChange == null) {
			fHistoryCompoundChange = fSourceViewer.getSelectedRange();
			fSourceViewer.getUndoManager().beginCompoundChange();
		}
		else {
			Point current = fSourceViewer.getSelectedRange();
			if (!current.equals(fHistoryCaretWorkaround)) {
				fHistoryCompoundChange = current;
			}
		}
		fInHistoryChange = true;

		StyledText widget = fSourceViewer.getTextWidget();
		if (UIAccess.isOkToUse(widget)) {
			widget.setRedraw(false);
			fDocument.set(text);
			fSourceViewer.setSelectedRange(fHistoryCompoundChange.x, fHistoryCompoundChange.y);
			widget.setRedraw(true);
		}
		else {
			fDocument.set(text);
		}
		
		fHistoryCaretWorkaround = fSourceViewer.getSelectedRange();
		fInHistoryChange = false;
	}
	
	public void doSubmit() {
		String content = fDocument.get();
		ToolController controller = fProcess.getController();
		
		if (controller != null && controller.submit(content, SubmitType.CONSOLE)) {
			clear();
		}
	}
	
	public void clear() {
		fDocument.set(""); //$NON-NLS-1$
		fCurrentHistoryEntry = null;
		fHistoryCompoundChange = null;
		fSourceViewer.getUndoManager().reset();
		fScroller.reset();
	}

	public Composite getComposite() {
		return fComposite;
	}
	
	public InputSourceViewer getSourceViewer() {
		return fSourceViewer;
	}
	
	public Button getSubmitButton() {
		return fSubmitButton;
	}
	
	protected NIConsolePage getConsolePage() {
		return fConsolePage;
	}

	public void dispose() {
		fProcess.getHistory().removeListener(fHistoryListener);
		fHistoryListener = null;
		fCurrentHistoryEntry = null;
		
		if (fInstalledModules != null) {
			for (int i = 0; i < fInstalledModules.length; i++) {
				fInstalledModules[i].uninstall();
			}
			fInstalledModules = null;
		}

		if (fSourceViewerDecorationSupport != null) {
			fSourceViewerDecorationSupport.dispose();
			fSourceViewerDecorationSupport = null;
		}
		fProcess = null;
		fConsolePage = null;
	}
	
}
