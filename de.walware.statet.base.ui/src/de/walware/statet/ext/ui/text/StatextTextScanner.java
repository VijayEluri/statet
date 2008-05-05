/*******************************************************************************
 * Copyright (c) 2005-2007 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.ext.ui.text;

import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;

import de.walware.eclipsecommons.ui.util.ColorManager;

import de.walware.statet.base.ui.util.ISettingsChangedHandler;


/**
 * BufferedRuleBasedScanner with managed text styles/tokens.
 */
public abstract class StatextTextScanner extends BufferedRuleBasedScanner implements ISettingsChangedHandler {
	
	
	private TextStyleManager fTextStyles;
	
	
	public StatextTextScanner(final ColorManager colorManager, final IPreferenceStore preferenceStore,
			final String stylesGroupId) {
		super();
		fTextStyles = new TextStyleManager(colorManager, preferenceStore, stylesGroupId);
	}
	
	
	/**
	 * Must be called after the constructor has been called.
	 */
	protected void initialize() {
		final List<IRule> rules = createRules();
		if (rules != null)
			setRules(rules.toArray(new IRule[rules.size()]));
	}
	
	/**
	 * Creates the list of rules controlling this scanner.
	 */
	abstract protected List<IRule> createRules();
	
	
	protected IToken getToken(final String key) {
		return fTextStyles.getToken(key);
	}
	
	public boolean handleSettingsChanged(final Set<String> groupIds, final Object options) {
		return fTextStyles.handleSettingsChanged(groupIds, options);
	}
	
}
