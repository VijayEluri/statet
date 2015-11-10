/*=============================================================================#
 # Copyright (c) 2010-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.statet.r.ui.editors;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;

import de.walware.statet.r.core.IRCoreAccess;
import de.walware.statet.r.core.model.IRSourceUnit;


public interface IRSourceEditor extends ISourceEditor {
	
	
	IRCoreAccess getRCoreAccess();
	
	@Override
	IRSourceUnit getSourceUnit();
	
}