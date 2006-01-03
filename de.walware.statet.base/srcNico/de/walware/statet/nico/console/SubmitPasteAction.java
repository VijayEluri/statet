/*******************************************************************************
 * Copyright (c) 2005 StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.nico.console;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

import de.walware.statet.nico.Messages;
import de.walware.statet.nico.runtime.SubmitType;
import de.walware.statet.nico.runtime.ToolController;


class SubmitPasteAction extends Action {

	
	private static Pattern fLineSplitPattern = Pattern.compile("\\r(\\n)?|\\n");
	
	
	private NIConsolePage fView;
	
	public SubmitPasteAction(NIConsolePage consolePage) {
		
		super(Messages.PasteSubmitAction_name);
		
		setId(ActionFactory.PASTE.getId());
		setActionDefinitionId(IWorkbenchActionDefinitionIds.PASTE);
		
		fView = consolePage;
	}
	
	@Override
	public void run() {
		
		Transfer transfer = TextTransfer.getInstance();
		String text = (String) fView.getClipboard().getContents(transfer);
		ToolController controller = fView.getConsole().getController();
		
		if (text == null || controller == null)
			return;
		
		controller.runSubmitInBackground(
				createRunnable(controller, text), 
				fView.getSite().getShell());
	}
	
	
	static IRunnableWithProgress createRunnable(final ToolController controller, final String text) {
		
		return new IRunnableWithProgress () {
			public void run(IProgressMonitor monitor) throws InterruptedException {

				monitor.beginTask(NLS.bind(Messages.SubmitTask_name, controller.getName()), 3);
				
				String[] lines = splitString(text);
				monitor.worked(1);
				
				controller.submit(lines, SubmitType.CONSOLE, monitor);
				monitor.done();
			}
		};
	}
	
	static String[] splitString(String text) {
		
		String[] lines = fLineSplitPattern.split(text);
		return lines;
	}
}
