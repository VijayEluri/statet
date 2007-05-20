/*******************************************************************************
 * Copyright (c) 2005-2006 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.ui.text.r;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.WhitespaceRule;

import de.walware.eclipsecommons.ui.util.ColorManager;

import de.walware.statet.ext.ui.text.DefaultWhitespaceDetector;
import de.walware.statet.ext.ui.text.OperatorRule;
import de.walware.statet.ext.ui.text.StatextTextScanner;
import de.walware.statet.r.core.rlang.RTokens;


/**
 * Scanner for usual R code (except: strings, infix-operator).
 *  
 * @author Stephan Wahlbrink
 */
public class RCodeScanner extends StatextTextScanner {
	
	public RCodeScanner(ColorManager colorManager, IPreferenceStore preferenceStore) {
		super(colorManager, preferenceStore);
		initialize();
	}
	
	protected List<IRule> createRules() {
		
		IToken tInvalid = getToken(IRTextTokens.UNDEFINED);
		
		IToken tIdentifier = getToken(IRTextTokens.DEFAULT);
		IToken tSpecial = getToken(IRTextTokens.SPECIAL_CONSTANTS);
		IToken tLogical = getToken(IRTextTokens.LOGICAL_CONSTANTS);
		IToken tFlowControl = getToken(IRTextTokens.FLOWCONTROL);
		
		IToken tNumbers = getToken(IRTextTokens.NUMBERS);
		
		IToken tSeparators = getToken(IRTextTokens.SEPARATORS);
		IToken tAssignmentOperators = getToken(IRTextTokens.ASSIGNMENT);
		IToken tOtherOperators = getToken(IRTextTokens.OTHER_OPERATORS);
		IToken tGrouping = getToken(IRTextTokens.GROUPING);
		IToken tSubelement = getToken(IRTextTokens.INDEXING);
		
		setDefaultReturnToken(tInvalid);

		List<IRule> rules = new ArrayList<IRule>();
				
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new DefaultWhitespaceDetector()));
		
		// Add rule for R-Operators
		OperatorRule opRule = new OperatorRule(RTokens.SEPARATOR_CHARS);
		opRule.addOps(RTokens.SEPARATORS, tSeparators);
		opRule.addOps(RTokens.ASSIGNMENT_OPERATORS, tAssignmentOperators);
		opRule.addOps(RTokens.DEFAULT_OPERATORS, tOtherOperators);
		opRule.addOps(RTokens.GROUPING, tGrouping);
		opRule.addOps(RTokens.SUBELEMENT_ACCESS, tSubelement);
		opRule.addOps(RTokens.NAMESPACE_ACCESS, tSubelement);
		rules.add(opRule);
		
		// Add rule for number-constants
		rules.add(new RNumberRule(tNumbers, tInvalid));
		
		// Add rule for words (reserved words, identifiers)
		RWordRule wordRule = new RWordRule(tIdentifier, tInvalid);
		wordRule.addSpecialWords(RTokens.SPECIAL_CONSTANTS, tSpecial);
		wordRule.addSpecialWords(RTokens.LOGICAL_CONSTANTS, tLogical);
		wordRule.addSpecialWords(RTokens.FLOWCONTROL_RESERVED_WORDS, tFlowControl);
		rules.add(wordRule);
		
		return rules;
	}

}
