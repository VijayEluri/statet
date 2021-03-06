/*=============================================================================#
 # Copyright (c) 2010-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.statet.r.internal.ui.rhelp;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;

import de.walware.ecommons.ltk.ast.AstSelection;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.ui.sourceediting.assist.AssistInvocationContext;
import de.walware.ecommons.ltk.ui.sourceediting.assist.IInfoHover;

import de.walware.statet.r.core.IRCoreAccess;
import de.walware.statet.r.core.RCore;
import de.walware.statet.r.core.model.IRFrame;
import de.walware.statet.r.core.model.IRFrameInSource;
import de.walware.statet.r.core.model.IRSourceUnit;
import de.walware.statet.r.core.model.RElementAccess;
import de.walware.statet.r.core.model.RElementName;
import de.walware.statet.r.core.model.RModel;
import de.walware.statet.r.core.renv.IREnv;
import de.walware.statet.r.core.rhelp.IREnvHelp;
import de.walware.statet.r.core.rhelp.IRHelpManager;
import de.walware.statet.r.core.rhelp.IRHelpPage;
import de.walware.statet.r.core.rsource.ast.FCall;
import de.walware.statet.r.core.rsource.ast.NodeType;
import de.walware.statet.r.core.rsource.ast.RAstNode;


public class RHelpHover implements IInfoHover {
	
	
	private final int mode;
	
	
	public RHelpHover() {
		this(MODE_TOOLTIP);
	}
	
	public RHelpHover(final int mode) {
		this.mode = mode;
	}
	
	
	@Override
	public Object getHoverInfo(final AssistInvocationContext context) {
		final AstSelection selection = context.getAstSelection();
		if (!(selection.getCovering() instanceof RAstNode)) {
			return null;
		}
		final RAstNode rNode = (RAstNode) selection.getCovering();
		RElementName name = searchName(rNode, context, true);
		if (Thread.interrupted()) {
			return null;
		}
		if ((this.mode & MODE_FOCUS) != 0 && name == null) {
			RAstNode parent;
			switch (rNode.getNodeType()) {
			case SYMBOL:
			case STRING_CONST:
				parent = rNode.getRParent();
				if (parent != null && parent.getNodeType() == NodeType.F_CALL_ARG
						&& ((FCall.Arg) parent).getNameChild() == rNode) {
					name = searchNameOfFunction(parent, context);
				}
				break;
			case F_CALL:
			case F_CALL_ARGS:
			case F_CALL_ARG:
				name = searchNameOfFunction(rNode, context);
				break;
			default:
				break;
			}
			if (Thread.interrupted()) {
				return null;
			}
		}
		if (name == null) {
			return null;
		}
		
		final IRCoreAccess rCoreAccess= (context.getSourceUnit() instanceof IRSourceUnit) ?
				((IRSourceUnit) context.getSourceUnit()).getRCoreAccess() :
				RCore.WORKBENCH_ACCESS;
		IREnv rEnv= rCoreAccess.getREnv();
		if (rEnv == null) {
			rEnv= RCore.WORKBENCH_ACCESS.getREnv();
		}
		Object helpObject= null;
		
		final IRHelpManager rHelpManager = RCore.getRHelpManager();
		final IREnvHelp help = rHelpManager.getHelp(rEnv);
		if (help != null) {
			try {
				if (RElementName.isPackageFacetScopeType(name.getType())) {
					helpObject = help.getPkgHelp(name.getSegmentName());
				}
				else {
					if (name.getScope() != null
							&& RElementName.isPackageFacetScopeType(name.getScope().getType()) ) {
						helpObject= help.getPageForTopic(name.getScope().getSegmentName(),
								name.getSegmentName() );
					}
					if (helpObject== null) {
						final List<IRHelpPage> topics = help.getPagesForTopic(name.getSegmentName());
						if (topics == null || topics.isEmpty()) {
							return null;
						}
						if (topics.size() == 1) {
							helpObject = topics.get(0);
						}
						else {
							final IRFrameInSource frame = RModel.searchFrame((RAstNode) selection.getCovering());
							if (frame == null) {
								return null;
							}
							helpObject= searchFrames(topics, RModel.createDirectFrameList(frame));
							final ISourceUnit su = context.getSourceUnit();
							if (helpObject == null && su instanceof IRSourceUnit) {
								helpObject = searchFrames(topics,
										RModel.createProjectFrameList(null, (IRSourceUnit) su) );
							}
						}
					}
				}
			}
			catch (final CoreException e) {
				// CANCELLED
				return null;
			}
			finally {
				help.unlock();
			}
		}
		if (Thread.interrupted() || helpObject == null) {
			return null;
		}
		final String httpUrl = rHelpManager.toHttpUrl(helpObject, RHelpUIServlet.INFO_TARGET);
		if (httpUrl != null) {
			return new RHelpInfoHoverCreator.Data(context.getSourceViewer().getTextWidget(),
					helpObject, httpUrl);
		}
		return null;
	}
	
	private IRHelpPage searchFrames(final List<IRHelpPage> helpPages, final List<IRFrame> frames) {
		if (frames == null) {
			return null;
		}
		for (final IRFrame frame : frames) {
			if (frame.getFrameType() == IRFrame.PACKAGE) {
				for (final IRHelpPage helpPage : helpPages) {
					if (helpPage.getPackage().getName().equals(
							frame.getElementName().getSegmentName() )) {
						return helpPage;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return new RHelpInfoHoverCreator(this.mode);
	}
	
	static RElementName searchName(RAstNode rNode, final IRegion region, final boolean checkInterrupted) {
		RElementAccess access = null;
		while (rNode != null && access == null) {
			if (checkInterrupted && Thread.currentThread().isInterrupted()) {
				return null;
			}
			final List<Object> attachments= rNode.getAttachments();
			for (final Object attachment : attachments) {
				if (attachment instanceof RElementAccess) {
					access = (RElementAccess) attachment;
					final IRFrame frame = access.getFrame();
					if ((frame != null && frame.getFrameType() != IRFrame.FUNCTION)
							|| (RElementName.isPackageFacetScopeType(access.getType())) ) {
						final RElementName e = getElementAccessOfRegion(access, region);
						if (e != null) {
							return e;
						}
					}
				}
			}
			rNode = rNode.getRParent();
		}
		return null;
	}
	
	static RElementName searchNameOfFunction(RAstNode rNode, final IRegion region) {
		while (rNode != null) {
			if (rNode.getNodeType() == NodeType.F_CALL) {
				final FCall fcall = (FCall) rNode;
				if (fcall.getArgsOpenOffset() != Integer.MIN_VALUE
						&& fcall.getArgsOpenOffset() <= region.getOffset()) {
					final List<Object> attachments= ((FCall) rNode).getAttachments();
					for (final Object attachment : attachments) {
						if (attachment instanceof RElementAccess) {
							final RElementAccess access= (RElementAccess) attachment;
							final IRFrame frame = access.getFrame();
							if (access.getNode() == fcall
									&& frame != null && frame.getFrameType() != IRFrame.FUNCTION
									&& access.getNextSegment() == null) {
								final RElementName fName= RElementName.normalize(access);
								if (RElementName.isRegularMainType(fName.getType())) {
									return fName;
								}
							}
						}
					}
				}
				return null;
			}
			rNode = rNode.getRParent();
		}
		return null;
	}
	
	static RElementName getElementAccessOfRegion(final RElementAccess access, final IRegion region) {
		if (access.getSegmentName() == null) {
			return null;
		}
		
		int segmentCount= 0;
		RElementAccess current= access;
		while (current != null) {
			segmentCount++;
			final RAstNode nameNode= current.getNameNode();
			if (nameNode != null
					&& nameNode.getOffset() <= region.getOffset()
					&& nameNode.getEndOffset() >= region.getOffset() + region.getLength() ) {
				if (RElementName.isRegularMainType(access.getType())) {
					return access;
				}
				if (segmentCount == 1) {
					if (RElementName.isPackageFacetScopeType(access.getType())) {
						return access;
					}
				}
				else /* (segmentCount > 1) */ {
					if (RElementName.isPackageFacetScopeType(access.getType())
							&& RElementName.isRegularMainType(access.getNextSegment().getType())
							&& access.getNextSegment().getSegmentName() != null) {
						return RElementName.normalize(access);
					}
				}
				return null;
			}
			current = current.getNextSegment();
		}
		
		return null;
	}
	
}
