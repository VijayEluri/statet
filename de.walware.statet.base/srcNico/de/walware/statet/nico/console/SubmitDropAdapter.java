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

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;

import de.walware.statet.nico.runtime.ToolController;


public class SubmitDropAdapter implements DropTargetListener {

	
	private NIConsolePage fPage;
	
	
	public SubmitDropAdapter(NIConsolePage page) {
		
		fPage = page;
	}
	
	
	public void dragEnter(DropTargetEvent event) {
		
		validate(event);
	}

	public void dragLeave(DropTargetEvent event) {
		
	}

	public void dragOperationChanged(DropTargetEvent event) {
		
		validate(event);
	}

	public void dragOver(DropTargetEvent event) {
		
	}

	public void dropAccept(DropTargetEvent event) {
		
		validate(event);
	}

	public void drop(DropTargetEvent event) {
		
		String text = (String) event.data;
		ToolController controller = fPage.getConsole().getController();
		
		if (text == null || controller == null)
			return;
		
		IRunnableWithProgress runnable = SubmitPasteAction.createRunnable(controller, text);
		controller.runSubmitInBackground(runnable, fPage.getSite().getShell());
	}
	
	
	private void validate(DropTargetEvent event) {
		
		if (( (event.operations & DND.DROP_COPY) == DND.DROP_COPY) 
				&& fPage.getConsole().getController().isStarted() ) {
			event.detail = DND.DROP_COPY;
		}
		else {
			event.detail = DND.DROP_NONE;
		}
	}

}
