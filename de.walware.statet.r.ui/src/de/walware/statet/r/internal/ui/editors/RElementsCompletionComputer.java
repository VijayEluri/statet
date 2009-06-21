/*******************************************************************************
 * Copyright (c) 2008-2009 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.icu.text.Collator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IElementName;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ast.AstSelection;
import de.walware.ecommons.ltk.ast.IAstNode;
import de.walware.ecommons.ltk.ui.IElementLabelProvider;
import de.walware.ecommons.text.IPartitionConstraint;
import de.walware.ecommons.ui.text.sourceediting.AssistInvocationContext;
import de.walware.ecommons.ui.text.sourceediting.IAssistCompletionProposal;
import de.walware.ecommons.ui.text.sourceediting.IAssistInformationProposal;
import de.walware.ecommons.ui.text.sourceediting.IContentAssistComputer;
import de.walware.ecommons.ui.text.sourceediting.ISourceEditor;

import de.walware.statet.nico.core.ITool;
import de.walware.statet.nico.core.runtime.ToolProcess;
import de.walware.statet.nico.ui.NicoUITools;
import de.walware.statet.nico.ui.console.ConsolePageEditor;
import de.walware.statet.nico.ui.console.InputDocument;

import de.walware.statet.r.core.RCore;
import de.walware.statet.r.core.model.ArgsDefinition;
import de.walware.statet.r.core.model.IPackageReferences;
import de.walware.statet.r.core.model.IRFrame;
import de.walware.statet.r.core.model.IRFrameInSource;
import de.walware.statet.r.core.model.IRLangElement;
import de.walware.statet.r.core.model.IRMethod;
import de.walware.statet.r.core.model.IRModelInfo;
import de.walware.statet.r.core.model.IRModelManager;
import de.walware.statet.r.core.model.RElementAccess;
import de.walware.statet.r.core.model.RElementName;
import de.walware.statet.r.core.model.RModel;
import de.walware.statet.r.core.rlang.RTokens;
import de.walware.statet.r.core.rsource.IRDocumentPartitions;
import de.walware.statet.r.core.rsource.RHeuristicTokenScanner;
import de.walware.statet.r.core.rsource.ast.FCall;
import de.walware.statet.r.core.rsource.ast.NodeType;
import de.walware.statet.r.core.rsource.ast.RAstNode;
import de.walware.statet.r.core.rsource.ast.FCall.Args;
import de.walware.statet.r.nico.RWorkspace;
import de.walware.statet.r.nico.RWorkspace.ICombinedEnvironment;
import de.walware.statet.r.ui.RLabelProvider;


public class RElementsCompletionComputer implements IContentAssistComputer {
	
	public static final class PrefixPattern {
		
		private final char[] fPrefix;
		
		
		public PrefixPattern(final String namePrefix) {
			fPrefix = namePrefix.toLowerCase().toCharArray();
		}
		
		
		/**
		 * Tolerant string comparison
		 * 
		 * @param candidate string to test against prefix
		 * @param prefix char array of lowercase prefix
		 * @return if candidate starts with prefix
		 */
		public boolean matches(final String candidate) {
			if (fPrefix.length == 0) {
				return true;
			}
			if (candidate == null || candidate.length() == 0) {
				return false;
			}
			int pC = fPrefix[0];
			int cC = Character.toLowerCase(candidate.charAt(0));
			if (cC != pC) {
				return false;
			}
			int pIdx = 0;
			int cIdx = 0;
			while (true) {
				if (pC == cC) {
					if (++pIdx >= fPrefix.length) {
						return true;
					}
					if (++cIdx >= candidate.length()) {
						return false;
					}
					pC = fPrefix[pIdx];
					cC = Character.toLowerCase(candidate.charAt(cIdx));
					continue;
				}
				if (pC == '.' || pC == '_') {
					if (++pIdx >= fPrefix.length) {
						return true;
					}
					pC = fPrefix[pIdx];
					continue;
				}
				if (cC == '.' || cC == '_') {
					if (++cIdx >= candidate.length()) {
						return false;
					}
					cC = Character.toLowerCase(candidate.charAt(cIdx));
					continue;
				}
				return false;
			}
		}
	}
	
	private static final class ExactFCallPattern {
		
		private final IElementName fCodeName;
		private final String fAssignName;
		private final int fAssignLength;
		
		public ExactFCallPattern(final IElementName name) {
			fCodeName = name;
			if (fCodeName.getNextSegment() == null) {
				fAssignName = fCodeName.getSegmentName();
				fAssignLength = fAssignName.length();
			}
			else {
				fAssignName = null;
				fAssignLength = 0;
			}
		}
		
		public boolean matches(final IElementName candidateName) {
			String candidate0;
			return (fCodeName.equals(candidateName)
					|| (fAssignName != null && candidateName.getNextSegment() == null
							&& fAssignLength == (candidate0 = candidateName.getSegmentName()).length()-2
							&& fCodeName.getType() == candidateName.getType()
							&& candidate0.charAt(fAssignLength) == '<' && candidate0.charAt(fAssignLength+1) == '-'
							&& candidate0.regionMatches(false, 0, fAssignName, 0, fAssignLength) ));
		}
		
	}
	
	
	private static final char[] F_BRACKETS = new char[] { '(', ')' };
	
	private static final IPartitionConstraint SKIP_COMMENT_PARTITIONS = new IPartitionConstraint() {
		public boolean matches(final String partitionType) {
			return (partitionType != IRDocumentPartitions.R_COMMENT);
		};
	};
	
	private static class FCallInfo {
		
		final FCall node;
		final RElementAccess access;
		
		public FCallInfo(final FCall node, final RElementAccess access) {
			this.node = node;
			this.access = access;
		}
		
	}
	
	private static final int LOCAL_ENVIR = 0;
	private static final int WS_ENVIR = 1;
	private static final int RUNTIME_ENVIR = 2;
	
	
	private static final List<String> fgKeywords;
	static {
		final ArrayList<String> list = new ArrayList<String>();
		Collections.addAll(list, RTokens.CONSTANT_WORDS);
		Collections.addAll(list, RTokens.FLOWCONTROL_WORDS);
		Collections.sort(list, Collator.getInstance());
		list.trimToSize();
		fgKeywords = Collections.unmodifiableList(list);
	}
	
	
	public static class CompleteRuntime extends RElementsCompletionComputer {
		
		public CompleteRuntime() {
			fCompleteRuntimeMode = true;
		}
		
	}
	
	
	private class EnvirIter implements Iterator<IRFrame> {
		
		private int fEnvirListIter0;
		private int fEnvirListIter1 = -1;
		private IRFrame fNext;
		
		public boolean hasNext() {
			if (fNext != null) {
				return true;
			}
			ITER_0 : while (fEnvirListIter0 < fEnvirList.length) {
				if (++fEnvirListIter1 < fEnvirList[fEnvirListIter0].size()) {
					fNext = fEnvirList[fEnvirListIter0].get(fEnvirListIter1);
					return true;
				}
				else {
					fEnvirListIter0++;
					fEnvirListIter1 = -1;
					continue ITER_0;
				}
			}
			return false;
		}
		
		public int getEnvirGroup() {
			return fEnvirListIter0;
		}
		
		public IRFrame next() {
			if (hasNext()) {
				final IRFrame frame = fNext;
				fNext = null;
				return frame;
			}
			return null;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	
	private final IElementLabelProvider fLabelProvider = new RLabelProvider(RLabelProvider.NAMESPACE);
	private ISourceEditor fEditor;
	private RHeuristicTokenScanner fScanner;
	private ToolProcess<RWorkspace> fProcess;
	
	private final List<IRFrame>[] fEnvirList = new List[3];
	private Set<String> fEnvirListPackages;
	
	protected boolean fCompleteRuntimeMode;
	
	
	public RElementsCompletionComputer() {
		for (int i = 1; i < fEnvirList.length; i++) {
			fEnvirList[i] = new ArrayList<IRFrame>();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void sessionStarted(final ISourceEditor editor) {
		if (fEditor != editor) {
			fEditor = editor;
			fScanner = null;
			fProcess = null;
		}
		
		final ToolProcess tool;
		if (fEditor instanceof ConsolePageEditor) {
			tool = (ToolProcess) fEditor.getAdapter(ITool.class);
		}
		else {
			tool = NicoUITools.getTool(fEditor.getWorkbenchPart());
		}
		if (tool != null
				&& tool.getMainType() == "R") {
			final ToolProcess<RWorkspace> rProcess = tool;
			final RWorkspace workspace = rProcess.getWorkspaceData();
			if (workspace.hasRObjectDB()) {
				fProcess = rProcess;
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void sessionEnded() {
		fEnvirList[0] = null;
		for (int i = 1; i < fEnvirList.length; i++) {
			fEnvirList[i].clear();
		}
		fEnvirListPackages = null;
		fProcess = null;
	}
	
	private RHeuristicTokenScanner getScanner() {
		if (fScanner == null && fEditor != null) {
			fScanner = (RHeuristicTokenScanner) fEditor.getAdapter(RHeuristicTokenScanner.class);
		}
		return fScanner;
	}
	
	
	private boolean isCompletable(IElementName elementName) {
		if (elementName == null) {
			return false;
		}
		do {
			switch (elementName.getType()) {
			case RElementName.SUB_INDEXED_S:
			case RElementName.SUB_INDEXED_D:
				return false;
			}
			if (elementName.getSegmentName() == null) {
				return false;
			}
			elementName = elementName.getNextSegment();
		}
		while (elementName != null);
		return true;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public IStatus computeCompletionProposals(final AssistInvocationContext context,
			final int mode, final List<IAssistCompletionProposal> tenders, final IProgressMonitor monitor) {
		if (mode == IContentAssistComputer.INFORMATION_MODE) {
			return computeContextInformation2(context, tenders, false, monitor);
		}
		
		if (context.getModelInfo() == null) {
			return null;
		}
		
		// Get node
		final AstSelection astSelection = context.getAstSelection();
		IAstNode node = astSelection.getCovering();
		if (node == null) {
			node = context.getAstInfo().root;
		}
		if (!(node instanceof RAstNode)) {
			return null;
		}
		
		// Get envir
		if (!initEnvirList(context, (RAstNode) node)) {
			return null;
		}
		
		// Get prefix
		final String prefix = context.getIdentifierPrefix();
		final RElementName prefixSegments = RElementName.parseDefault(prefix);
		if (prefixSegments == null) {
			return null;
		}
		
		// Collect proposals
		if (prefixSegments.getNextSegment() == null) {
			doComputeArgumentProposals(context, prefix, prefixSegments, tenders, monitor);
			doComputeMainProposals(context, prefix, prefixSegments, tenders, monitor);
			doComputeKeywordProposals(context, prefix, prefixSegments.getSegmentName(), tenders, monitor);
		}
		else {
			final String lastPrefix = computeSingleIdentifierPrefix(context);
			doComputeSubProposals(context, lastPrefix, prefixSegments, tenders, monitor);
		}
		return null;
	}
	
	protected void doComputeArgumentProposals(final AssistInvocationContext context, final String orgPrefix, final IElementName prefixName,
			final List<IAssistCompletionProposal> tenders, final IProgressMonitor monitor) {
		final String namePrefix = prefixName.getSegmentName();
		int offset = context.getInvocationOffset()-context.getIdentifierPrefix().length();
		IDocument document = context.getSourceViewer().getDocument();
		final RHeuristicTokenScanner scanner = getScanner();
		int indexShift = 0;
		if (document instanceof InputDocument) {
			final InputDocument inputDoc = (InputDocument) document;
			document = inputDoc.getMasterDocument();
			indexShift = inputDoc.getOffsetInMasterDocument();
			offset += indexShift;
		}
		if (scanner == null || offset < 2) {
			return;
		}
		scanner.configureDefaultParitions(document);
		if (scanner.getPartitioningConfig().getDefaultPartitionConstraint().matches(scanner.getPartition(offset-1).getType())) {
			final int index = scanner.findOpeningPeer(offset-1, F_BRACKETS);
			if (index >= 0) {
				final FCallInfo fcallInfo = searchFCallInfo(context, index-indexShift);
				if (fcallInfo != null) {
					final Args child = fcallInfo.node.getArgsChild();
					int sep = fcallInfo.node.getArgsOpenOffset()+indexShift;
					for (int argIdx = 0; argIdx < child.getChildCount()-1; argIdx++) {
						final int next = child.getSeparatorOffset(argIdx);
						if (next+indexShift < offset) {
							sep = next+indexShift;
						}
						else {
							break;
						}
					}
					fScanner.configure(document, SKIP_COMMENT_PARTITIONS);
					if (sep+1 == offset
							|| fScanner.findNonBlankForward(sep+1, offset, true) < 0) {
						doComputeFCallArgumentProposals(context, offset-indexShift, fcallInfo, namePrefix, tenders);
					}
				}
			}
		}
	}
	
	protected void doComputeMainProposals(final AssistInvocationContext context, final String orgPrefix, final IElementName prefixName,
			final List<IAssistCompletionProposal> tenders, final IProgressMonitor monitor) {
		String namePrefix = prefixName.getSegmentName();
		if (namePrefix == null) {
			namePrefix = ""; //$NON-NLS-1$
		}
		final PrefixPattern pattern = new PrefixPattern(namePrefix); 
		final int offset = context.getInvocationOffset()-orgPrefix.length();
		final Set<String> mainNames = new HashSet<String>();
		final List<String> methodNames = new ArrayList<String>();
		
		int sourceLevel = 5;
		for (final EnvirIter iter = new EnvirIter(); iter.hasNext();) {
			final IRFrame envir = iter.next();
			int relevance;
			switch (envir.getFrameType()) {
			case IRFrame.CLASS:
			case IRFrame.FUNCTION:
			case IRFrame.EXPLICIT:
				relevance = Math.max(sourceLevel--, 1);
				break;
			case IRFrame.PROJECT:
				relevance = 1;
				break;
			case IRFrame.PACKAGE:
				relevance = -5;
				if (iter.getEnvirGroup() > 0 && namePrefix.length() == 0) {
					continue;
				}
				break;
			default:
				relevance = -10;
				break;
			}
			final List<? extends IRLangElement> elements = envir.getModelChildren(null);
			for (final IModelElement element : elements) {
				final IElementName elementName = element.getElementName();
				final int c1type = (element.getElementType() & IModelElement.MASK_C1);
				final boolean isRich = (c1type == IModelElement.C1_METHOD);
				if ((isRich || c1type == IModelElement.C1_VARIABLE)
						&& isCompletable(elementName)
						&& pattern.matches(elementName.getSegmentName())) {
					if ((relevance < 0) && !isRich
							&& mainNames.contains(elementName.getSegmentName()) ) {
						continue;
					}
					final IAssistCompletionProposal proposal = createProposal(context, offset, elementName, element, relevance);
					if (proposal != null) {
						if (elementName.getNextSegment() == null) {
							if (isRich) {
								methodNames.add(elementName.getSegmentName());
							}
							else {
								mainNames.add(elementName.getSegmentName());
							}
						}
						tenders.add(proposal);
					}
				}
			}
		}
		
		mainNames.addAll(methodNames);
		for (final EnvirIter iter = new EnvirIter(); iter.hasNext();) {
			final IRFrame envir = iter.next();
			if (envir instanceof IRFrameInSource) {
				final IRFrameInSource sframe = (IRFrameInSource) envir;
				final Set<String> elementNames = sframe.getAllAccessNames();
				for (final String candidate : elementNames) {
					if (candidate != null
							&& pattern.matches(candidate) 
							&& !mainNames.contains(candidate)
							&& !(candidate.equals(namePrefix) && (sframe.getAllAccessOfElement(candidate).size() <= 1)) ) {
						final IAssistCompletionProposal proposal = createProposal(context, orgPrefix, candidate);
						if (proposal != null) {
							mainNames.add(candidate);
							tenders.add(proposal);
						}
					}
				}
			}
		}
	}
	
	private void doComputeKeywordProposals(final AssistInvocationContext context, final String orgPrefix, final String prefix,
			final List<IAssistCompletionProposal> tenders, final IProgressMonitor monitor) {
		if (prefix.length() > 0 && orgPrefix.charAt(0) != '`') {
			final int offset = context.getInvocationOffset()-orgPrefix.length();
			final List<String> keywords = fgKeywords;
			for (final String keyword : keywords) {
				if (keyword.regionMatches(true, 0, prefix, 0, prefix.length())) {
					tenders.add(new RKeywordCompletionProposal(context, keyword, offset));
				}
			}
		}
	}
	
	protected String computeSingleIdentifierPrefix(final AssistInvocationContext context) {
		// like RAssistInvocationContext#computeIdentifierPrefix but only one single identifier
		final AbstractDocument document = (AbstractDocument) context.getSourceViewer().getDocument();
		int offset = context.getInvocationOffset();
		if (offset <= 0 || offset > document.getLength()) {
			return ""; 
		}
		try {
			ITypedRegion partition = document.getPartition(context.getEditor().getPartitioning().getPartitioning(), offset, true);
			if (partition.getType() == IRDocumentPartitions.R_QUOTED_SYMBOL) {
				offset = partition.getOffset();
			}
			int goodStart = offset;
			SEARCH_START: while (offset > 0) {
				final char c = document.getChar(offset - 1);
				if (RTokens.isRobustSeparator(c, false)) {
					switch (c) {
					case ':':
					case '$':
					case '@':
						break SEARCH_START;
					case ' ':
					case '\t':
						if (offset >= 2) {
							final char c2 = document.getChar(offset - 2);
							if ((offset == context.getInvocationOffset())
									&& !RTokens.isRobustSeparator(c, false)) {
								offset -= 2;
								continue SEARCH_START;
							}
						}
						break SEARCH_START;
					case '`':
						partition = document.getPartition(context.getEditor().getPartitioning().getPartitioning(), offset, false);
						if (partition.getType() == IRDocumentPartitions.R_QUOTED_SYMBOL) {
							offset = goodStart = partition.getOffset();
							break SEARCH_START;
						}
						else {
							break SEARCH_START;
						}
					
					default:
						break SEARCH_START;
					}
				}
				else {
					offset --;
					goodStart = offset;
					continue SEARCH_START;
				}
			}
			
			return document.get(offset, context.getInvocationOffset() - goodStart);
		}
		catch (final BadLocationException e) {
		}
		catch (final BadPartitioningException e) {
		}
		return ""; 
	}
	
	protected void doComputeSubProposals(final AssistInvocationContext context, final String orgPrefix, final RElementName prefixSegments,
			final List<IAssistCompletionProposal> tenders, final IProgressMonitor monitor) {
		int count = 0;
		IElementName prefixSegment = prefixSegments;
		while (true) {
			count++;
			if (prefixSegment.getNextSegment() != null) {
				prefixSegment = prefixSegment.getNextSegment();
				continue;
			}
			else {
				break;
			}
		}
		String namePrefix = prefixSegment.getSegmentName();
		if (namePrefix == null) {
			namePrefix = ""; //$NON-NLS-1$
		}
		final PrefixPattern pattern = new PrefixPattern(namePrefix);
		final int offset = context.getInvocationOffset()-orgPrefix.length();
		
		final Set<String> mainNames = new HashSet<String>();
		final List<String> methodNames = new ArrayList<String>();
		
		int sourceLevel = 5;
		for (final EnvirIter iter = new EnvirIter(); iter.hasNext();) {
			final IRFrame envir = iter.next();
			int relevance;
			switch (envir.getFrameType()) {
			case IRFrame.CLASS:
			case IRFrame.FUNCTION:
			case IRFrame.EXPLICIT:
				relevance = Math.max(sourceLevel--, 1);
				break;
			case IRFrame.PROJECT:
				relevance = 1;
				break;
			case IRFrame.PACKAGE:
				relevance = -5;
				break;
			default:
				relevance = -10;
				break;
			}
			final List<? extends IRLangElement> elements = envir.getModelChildren(null);
			ITER_ELEMENTS: for (final IModelElement rootElement : elements) {
				final IElementName elementName = rootElement.getElementName();
				final int c1type = (rootElement.getElementType() & IModelElement.MASK_C1);
				final boolean isRich = (c1type == IModelElement.C1_METHOD);
				if (isRich || c1type == IModelElement.C1_VARIABLE) {
					IModelElement element = rootElement;
					prefixSegment = prefixSegments;
					IElementName elementSegment = elementName;
					ITER_SEGMENTS: for (int i = 0; i < count-1; i++) {
						if (elementSegment == null) {
							final List<? extends IModelElement> children = element.getModelChildren(null);
							for (final IModelElement child : children) {
								elementSegment = child.getElementName();
								if (isCompletable(elementSegment)
										&& elementSegment.getSegmentName().equals(prefixSegment.getSegmentName())) {
									element = child;
									prefixSegment = prefixSegment.getNextSegment();
									elementSegment = elementSegment.getNextSegment();
									continue ITER_SEGMENTS;
								}
							}
							continue ITER_ELEMENTS;
						}
						else {
							if (isCompletable(elementSegment)
									&& elementSegment.getSegmentName().equals(prefixSegment.getSegmentName())) {
								prefixSegment = prefixSegment.getNextSegment();
								elementSegment = elementSegment.getNextSegment();
								continue ITER_SEGMENTS;
							}
							continue ITER_ELEMENTS;
						}
					}
					
					final boolean childMode;
					final List<? extends IModelElement> children;
					if (elementSegment == null) {
						childMode = true;
						children = element.getModelChildren(null);
					}
					else {
						childMode = false;
						children = Collections.singletonList(element);
					}
					for (final IModelElement child : children) {
						if (childMode) {
							elementSegment = child.getElementName();
						}
						final String candidate = elementSegment.getSegmentName();
						if (isCompletable(elementSegment)
								&& pattern.matches(candidate) ) {
							if ((relevance > 0) && !isRich
									&& mainNames.contains(candidate) ) {
								continue ITER_ELEMENTS;
							}
							final IAssistCompletionProposal proposal = createProposal(context, offset, elementSegment, child, relevance);
							if (proposal != null) {
								if (elementSegment.getNextSegment() == null) {
									if (isRich) {
										methodNames.add(candidate);
									}
									else {
										mainNames.add(candidate);
									}
								}
								tenders.add(proposal);
							}
						}
					}
				}
			}
		}
		
		mainNames.addAll(methodNames);
		for (final EnvirIter iter = new EnvirIter(); iter.hasNext();) {
			final IRFrame envir = iter.next();
			if (envir instanceof IRFrameInSource) {
				final IRFrameInSource sframe = (IRFrameInSource) envir;
				final List<? extends RElementAccess> allAccess = sframe.getAllAccessOfElement(prefixSegments.getSegmentName());
				if (allAccess != null) {
					ITER_ELEMENTS: for (final RElementAccess elementAccess : allAccess) {
						IElementName elementSegment = elementAccess;
						ITER_SEGMENTS: for (int i = 0; i < count-1; i++) {
							if (isCompletable(elementSegment)
									&& elementSegment.getSegmentName().equals(prefixSegment.getSegmentName())) {
								prefixSegment = prefixSegment.getNextSegment();
								elementSegment = elementSegment.getNextSegment();
								continue ITER_SEGMENTS;
							}
							continue ITER_ELEMENTS;
						}
						
						if (elementSegment == null) {
							continue ITER_ELEMENTS;
						}
						final String candidate = elementSegment.getSegmentName();
						if (candidate != null && isCompletable(elementSegment)
								&& pattern.matches(candidate)
								&& !mainNames.contains(candidate)
								&& !candidate.equals(namePrefix) ) {
							final IAssistCompletionProposal proposal = createProposal(context, orgPrefix, candidate);
							if (proposal != null) {
								mainNames.add(candidate);
								tenders.add(proposal);
							}
						}
					}
				}
			}
		}
	}
	
	protected IAssistCompletionProposal createProposal(final AssistInvocationContext context, final String prefix, final String name) {
		final int offset = context.getInvocationOffset()-prefix.length();
		return new RSimpleCompletionComputer(context, name, offset);
	}
	
	protected IAssistCompletionProposal createProposal(final AssistInvocationContext context, final int offset, final IElementName elementName, final IModelElement element, final int relevance) {
		return new RElementCompletionProposal(context, elementName, offset, element, relevance, fLabelProvider);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IStatus computeContextInformation(final AssistInvocationContext context,
			final List<IAssistInformationProposal> tenders, final IProgressMonitor monitor) {
		return computeContextInformation2(context, tenders, true, monitor);
	}
	
	public IStatus computeContextInformation2(final AssistInvocationContext context,
			final List tenders, final boolean createContextInfoOnly, final IProgressMonitor monitor) {
		if (context.getModelInfo() == null) {
			return null;
		}
		
		int offset = context.getInvocationOffset();
		IDocument document = context.getSourceViewer().getDocument();
		final RHeuristicTokenScanner scanner = getScanner();
		int indexShift = 0;
		if (document instanceof InputDocument) {
			final InputDocument inputDoc = (InputDocument) document;
			document = inputDoc.getMasterDocument();
			indexShift = inputDoc.getOffsetInMasterDocument();
			offset += indexShift;
		}
		if (scanner == null || offset < 2) {
			return null;
		}
		scanner.configureDefaultParitions(document);
		if (scanner.getPartitioningConfig().getDefaultPartitionConstraint().matches(scanner.getPartition(offset-1).getType())) {
			final int index = scanner.findOpeningPeer(offset-1, F_BRACKETS);
			if (index >= 0) {
				final FCallInfo fcallInfo = searchFCallInfo(context, index-indexShift);
				if (fcallInfo != null) {
					doComputeFCallContextInformation(context, fcallInfo, tenders, createContextInfoOnly);
				}
			}
		}
		return Status.OK_STATUS;
	}
	
	private FCallInfo searchFCallInfo(final AssistInvocationContext context, final int fcallOpen) {
		final AstInfo astInfo = context.getAstInfo();
		if (astInfo == null || astInfo.root == null) {
			return null;
		}
		final AstSelection selection = AstSelection.search(astInfo.root, fcallOpen, fcallOpen+1, AstSelection.MODE_COVERING_SAME_LAST);
		IAstNode node = selection.getCovering();
		
		while (node != null && node instanceof RAstNode) {
			final RAstNode rnode = (RAstNode) node;
			FCall fcallNode = null;
			if (rnode.getNodeType() == NodeType.F_CALL
					&& (fcallOpen == (fcallNode = ((FCall) rnode)).getArgsOpenOffset())) {
				final Object[] attachments = fcallNode.getRefChild().getAttachments();
				for (int i = 0; i < attachments.length; i++) {
					if (attachments[i] instanceof RElementAccess) {
						final RElementAccess fcallAccess = (RElementAccess) attachments[i];
						if (fcallAccess.isMethodAccess() && !fcallAccess.isWriteAccess()) {
							final FCallInfo info = new FCallInfo(fcallNode, fcallAccess);
							if (initEnvirList(context, fcallNode)) {
								return info;
							}
						}
					}
				}
			}
			node = rnode.getParent();
		}
		return null;
	}
	
	private void doComputeFCallContextInformation(final AssistInvocationContext context, final FCallInfo fcallInfo, final List tenders, final boolean createContextInfoOnly) {
		int distance = 0;
		final ExactFCallPattern pattern = new ExactFCallPattern(fcallInfo.access);
		final int infoOffset = fcallInfo.node.getArgsOpenOffset()+1;
		for (final Iterator<IRFrame> iter = new EnvirIter(); iter.hasNext();) {
			final IRFrame envir = iter.next();
			final List<? extends IModelElement> elements = envir.getModelChildren(null);
			for (final IModelElement element : elements) {
				final IElementName elementName = element.getElementName();
				final int c1type = (element.getElementType() & IModelElement.MASK_C1);
				if ((c1type == IModelElement.C1_METHOD)
						&& isCompletable(elementName)
						&& pattern.matches(elementName)) {
					final IRMethod method = (IRMethod) element;
					tenders.add(createContextInfoOnly ?
							new RArgumentListContextInformation(infoOffset, method) :
							new RElementCompletionProposal.ContextInformationProposal(context, method.getElementName(), infoOffset,
								method, -distance, fLabelProvider) );
				}
			}
			distance++;
		}
	}
	
	private void doComputeFCallArgumentProposals(final AssistInvocationContext context, final int offset, final FCallInfo fcallInfo,
			final String prefix, final List<IAssistCompletionProposal> tenders) {
		int distance = 0;
		final HashSet<String> names = new HashSet<String>();
		for (final Iterator<IRFrame> iter = new EnvirIter(); iter.hasNext();) {
			final IRFrame envir = iter.next();
			final List<? extends IModelElement> elements = envir.getModelChildren(null);
			for (final IModelElement element : elements) {
				final IElementName elementName = element.getElementName();
				final int c1type = (element.getElementType() & IModelElement.MASK_C1);
				if ((c1type == IModelElement.C1_METHOD)
						&& isCompletable(elementName)
						&& (fcallInfo.access.equals(elementName)) ) {
					final IRMethod method = (IRMethod) element;
					final ArgsDefinition argsDef = method.getArgsDefinition();
					if (argsDef != null) {
						for (int i = 0; i < argsDef.size(); i++) {
							final ArgsDefinition.Arg arg = argsDef.get(i);
							if (arg.name != null && arg.name.length() > 0 && !arg.name.equals("...")) {
								if ((prefix == null || arg.name.startsWith(prefix))
										&& names.add(arg.name)) {
									final RElementName name = RElementName.create(RElementName.MAIN_DEFAULT, arg.name);
									tenders.add(new RElementCompletionProposal.ArgumentProposal(context, name, offset, distance, fLabelProvider));
								}
							}
						}
					}
				}
			}
			distance++;
		}
	}
	
	
	private boolean initEnvirList(final AssistInvocationContext context, final RAstNode node) {
		if (fEnvirList[LOCAL_ENVIR] != null) {
			return true;
		}
		final IRFrameInSource envir = RModel.searchEnvir(node);
		if (envir != null && !fCompleteRuntimeMode) {
			fEnvirList[LOCAL_ENVIR] = RModel.createEnvirList(envir);
		}
		else {
			fEnvirList[LOCAL_ENVIR] = new ArrayList<IRFrame>();
		}
		
		fEnvirListPackages = new HashSet<String>();
		if (!fCompleteRuntimeMode) {
			addProjectEnvirList();
		}
		addRuntimeEnvirList(context);
		return true;
	}
	
	private void addProjectEnvirList() {
		final ISourceUnit su = fEditor.getSourceUnit();
		if (su == null) {
			return;
		}
		final IResource resource = su.getResource();
		if (resource == null) {
			return;
		}
		final IProject suProject = resource.getProject();
		if (suProject == null) {
			return;
		}
		final IRModelManager manager = RCore.getRModelManager();
		IRFrame frame;
		
		frame = manager.getProjectFrame(suProject);
		if (frame != null) {
			if (frame.getFrameType() == IRFrame.PACKAGE) {
				fEnvirListPackages.add(frame.getElementName().getSegmentName());
			}
			fEnvirList[LOCAL_ENVIR].add(new FilteredFrame(frame, su));
		}
		
		final List<IProject> projects = new ArrayList<IProject>();
		try {
			final IProject[] referencedProjects = suProject.getReferencedProjects();
			for (final IProject referencedProject : referencedProjects) {
				projects.add(referencedProject);
			}
		} catch (final CoreException e) {}
		for (int i = 0; i < projects.size(); i++) {
			final IProject project = projects.get(i);
			frame = manager.getProjectFrame(project);
			if (frame != null) {
				if (frame.getFrameType() == IRFrame.PACKAGE) {
					fEnvirListPackages.add(frame.getElementName().getSegmentName());
				}
				fEnvirList[WS_ENVIR].add(frame);
			}
			try {
				final IProject[] referencedProjects = project.getReferencedProjects();
				for (final IProject referencedProject : referencedProjects) {
					if (!projects.contains(referencedProject)) {
						projects.add(referencedProject);
					}
				}
			} catch (final CoreException e) {}
		}
	}
	
	private void addRuntimeEnvirList(final AssistInvocationContext context) {
		if (fProcess != null) {
			if (fEditor instanceof ConsolePageEditor || fCompleteRuntimeMode) {
				final RWorkspace data = fProcess.getWorkspaceData();
				final List<? extends ICombinedEnvironment> runtimeList = data.getRSearchEnvironments();
				if (runtimeList != null && !runtimeList.isEmpty()) {
					for (final ICombinedEnvironment envir : runtimeList) {
						final IRFrame frame = (IRFrame) envir;
						if (frame.getFrameType() == IRFrame.PROJECT) {
							fEnvirList[LOCAL_ENVIR].add(frame);
						}
						else {
							fEnvirList[WS_ENVIR].add(frame);
						}
					}
				}
			}
			else {
				final Set<String> requiredPackages = new HashSet<String>();
				final IRModelInfo info = (IRModelInfo) context.getModelInfo();
				final IPackageReferences packages = info.getReferencedPackages();
				for (final String name : packages.getAllPackageNames()) {
					if (packages.isImported(name)) {
						requiredPackages.add(name);
					}
				}
				requiredPackages.add("base");
				
				final RWorkspace data = fProcess.getWorkspaceData();
				final List<? extends ICombinedEnvironment> runtimeList = data.getRSearchEnvironments();
				if (runtimeList != null && !runtimeList.isEmpty()) {
					for (final ICombinedEnvironment envir : runtimeList) {
						final IRFrame frame = (IRFrame) envir;
						if (frame.getFrameType() == IRFrame.PACKAGE
								&& requiredPackages.contains(frame.getElementName().getSegmentName())
								&& !fEnvirListPackages.contains(frame.getElementName().getSegmentName())) {
							fEnvirList[RUNTIME_ENVIR].add(frame);
						}
					}
				}
			}
		}
	}
	
}
