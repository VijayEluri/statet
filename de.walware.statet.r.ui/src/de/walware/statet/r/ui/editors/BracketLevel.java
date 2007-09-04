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

package de.walware.statet.r.ui.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.swt.events.VerifyEvent;

import de.walware.statet.r.core.rsource.IRDocumentPartitions;


/**
 * Control LinkedModel for auto inserted pairs
 */
class BracketLevel implements IExitPolicy {

	static interface IBracketLevelType {
		boolean processReturn();
		boolean matchesEnd(IDocument doc, int startOffset, char c, int charOffset) throws BadLocationException;
	}
	
	private static class BracketLevelType implements IBracketLevelType {

		private final char fCloseChar;
		
		public BracketLevelType(char closeChar) {
			fCloseChar = closeChar;
		}
		
		public boolean processReturn() {
			return true;
		}
		public boolean matchesEnd(final IDocument doc, final int startOffset, final char c, int charOffset) throws BadLocationException {
			return (c == fCloseChar && TextUtilities.getPartition(doc, IRDocumentPartitions.R_DOCUMENT_PARTITIONING, charOffset, true)
					.getType() == IRDocumentPartitions.R_DEFAULT);
		}
		
	}

	private static class StringLevelType implements IBracketLevelType {

		private final char fSeparatorChar;

		public StringLevelType(char sepChar) {
			fSeparatorChar = sepChar;
		}
		
		public boolean processReturn() {
			return false;
		}
		public boolean matchesEnd(final IDocument doc, final int startOffset, final char c, int charOffset) throws BadLocationException {
			if (c == fSeparatorChar && TextUtilities.getPartition(doc, IRDocumentPartitions.R_DOCUMENT_PARTITIONING, charOffset, true)
					.getType() == IRDocumentPartitions.R_STRING) {
				int count = -1;
				do {
					count++;
					charOffset--;
				} while (doc.getChar(charOffset) == '\\');
				return ((count % 2) == 0);
			}
			return false;
		}
		
	}
	
	private static class InfixLevelType implements IBracketLevelType {

		private final char fCloseChar;
		
		public InfixLevelType(char closeChar) {
			fCloseChar = closeChar;
		}
		
		public boolean processReturn() {
			return false;
		}
		public boolean matchesEnd(final IDocument doc, final int startOffset, final char c, int charOffset) throws BadLocationException {
			return (c == fCloseChar);
		}
		
	}

	private static final IBracketLevelType LEVEL_STRING_S = new StringLevelType('\'');
	private static final IBracketLevelType LEVEL_STRING_D = new StringLevelType('\"');
	private static final IBracketLevelType LEVEL_CURLY_BRACKET = new BracketLevelType('}');
	private static final IBracketLevelType LEVEL_ROUND_BRACKET = new BracketLevelType(')');
	private static final IBracketLevelType LEVEL_SQUARE_BRACKET = new BracketLevelType(']');
	private static final IBracketLevelType LEVEL_INFIX = new InfixLevelType('%');
	
	static final IBracketLevelType getType(char c) {
		switch (c) {
		case '\'':
			return LEVEL_STRING_S;
		case '\"':
			return LEVEL_STRING_D;
		case '{':
			return LEVEL_CURLY_BRACKET;
		case '(':
			return LEVEL_ROUND_BRACKET;
		case '[':
			return LEVEL_SQUARE_BRACKET;
		case '%':
			return LEVEL_INFIX;
		}
		throw new IllegalArgumentException();
	}

	
	
	private IBracketLevelType fConfig;
	private LinkedPosition fPosition;
	private IDocument fDocument;
	private boolean fConsoleMode;
	
	public BracketLevel(IDocument doc, LinkedPosition position, IBracketLevelType config, boolean consoleMode) throws BadLocationException, BadPositionCategoryException {
		fConfig = config;
		fPosition = position;
		fDocument = doc;
		fConsoleMode = consoleMode;
	}

	public ExitFlags doExit(final LinkedModeModel model, final VerifyEvent event,
			final int offset, final int length) {
		try {
			// submit cr
			switch (event.character) {
			case 0x0A:
			case 0x0D:
				if (fConsoleMode || fConfig.processReturn()) {
					return new ExitFlags(ILinkedModeListener.NONE, true);
				}
				return null;
			case 0x08:
				if (offset == fPosition.offset) {
					fDocument.replace(offset-1, 2, ""); //$NON-NLS-1$
					return new ExitFlags(ILinkedModeListener.NONE, false);
				}
				return null;
			}
			// don't enter the character if if its the closing peer
			if (offset == fPosition.offset+fPosition.length && length == 0
					&& fConfig.matchesEnd(fDocument, fPosition.offset, event.character, offset)) {
				return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
			}
		} catch (BadLocationException e) {
		}
		return null;
	}

}