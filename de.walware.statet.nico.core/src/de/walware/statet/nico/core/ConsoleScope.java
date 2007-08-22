/*******************************************************************************
 * Copyright (c) 2007 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.nico.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;


/**
 * Dummy scope to overlay instance preferences with special values for consoles.
 */
public class ConsoleScope implements IScopeContext {
	
	
	public static final String SCOPE = "nico"; //$NON-NLS-1$
	public static final String QUALIFIER = "de.walware.statet.nico.core.Scope"; //$NON-NLS-1$

	
	private InstanceScope fBaseScope;
	
	
	public ConsoleScope() {
		fBaseScope = new InstanceScope();
	}
	
	public IPath getLocation() {
		return fBaseScope.getLocation();
	}
	
	public String getName() {
		return SCOPE;
	}
	
	public IEclipsePreferences getNode(String qualifier) {
		return (IEclipsePreferences) fBaseScope.getNode(QUALIFIER).node(qualifier);
	}
	
}
