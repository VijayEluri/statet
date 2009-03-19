/*******************************************************************************
 * Copyright (c) 2007-2009 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ui.preferences;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;

import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.SettingsChangeNotifier.ChangeListener;
import de.walware.ecommons.ui.util.ISettingsChangedHandler;
import de.walware.ecommons.ui.util.UIAccess;


/**
 * Util for settings changes in UI components.
 * Propagates changes to handler in UI thread and disposes automatically.
 */
public class SettingsUpdater implements ChangeListener {
	
	
	private ISettingsChangedHandler fHandler;
	private Control fControl;
	private DisposeListener fDisposeListener;
	private String[] fInterestingGroupIds;
	
	
	public SettingsUpdater(final ISettingsChangedHandler handler, final Control control) {
		this(handler, control, null);
	}
	
	public SettingsUpdater(final ISettingsChangedHandler handler, final Control control, final String[] groupIds) {
		fHandler = handler;
		fControl = control;
		setInterestingGroups(groupIds);
		PreferencesUtil.getSettingsChangeNotifier().addChangeListener(this);
		fDisposeListener = new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				dispose();
			}
		};
		fControl.addDisposeListener(fDisposeListener);
	}
	
	
	public void setInterestingGroups(final String[] groupIds) {
		fInterestingGroupIds = groupIds;
	}
	
	public void settingsChanged(final Set<String> groupIds) {
		if (fInterestingGroupIds == null) {
			runUpdate(groupIds);
			return;
		}
		for (final String id : fInterestingGroupIds) {
			if (groupIds.contains(id)) {
				runUpdate(groupIds);
				return;
			}
		}
	}
	
	private void runUpdate(final Set<String> groupIds) {
		final HashMap<String, Object> options = new HashMap<String, Object>();
		UIAccess.getDisplay().syncExec(new Runnable() {
			public void run() {
				if (UIAccess.isOkToUse(fControl)) {
					fHandler.handleSettingsChanged(groupIds, options);
				}
			}
		});
	}
	
	public void dispose() {
		fControl.removeDisposeListener(fDisposeListener);
		PreferencesUtil.getSettingsChangeNotifier().removeChangeListener(this);
	}
	
}