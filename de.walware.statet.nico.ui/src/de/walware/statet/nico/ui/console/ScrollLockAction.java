/*******************************************************************************
 * Copyright (c) 2005 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.nico.ui.console;

import org.eclipse.jface.action.Action;

import de.walware.eclipsecommons.ui.SharedMessages;

import de.walware.statet.base.ui.StatetImages;


public class ScrollLockAction extends Action {
	
	
	public interface Receiver {
		
		public void setAutoScroll(boolean enabled);
		
	}
	
	
	private Receiver fView;
	
	
	public ScrollLockAction(final Receiver view, final boolean initialChecked) {
		setText(SharedMessages.ToggleScrollLockAction_name);
		setToolTipText(SharedMessages.ToggleScrollLockAction_tooltip);
		
		setImageDescriptor(StatetImages.getDescriptor(StatetImages.LOCTOOL_SCROLLLOCK));
		
		fView = view;
		setChecked(initialChecked);
	}
	
	
	@Override
	public void run() {
		fView.setAutoScroll(!isChecked());
	}
	
}
