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

package de.walware.statet.r.ui.editors;

import static de.walware.statet.r.core.rsource.IRDocumentPartitions.RDOC_COMMENT;
import static de.walware.statet.r.core.rsource.IRDocumentPartitions.RDOC_DEFAULT;
import static de.walware.statet.r.core.rsource.IRDocumentPartitions.RDOC_DOCUMENT_PARTITIONING;
import static de.walware.statet.r.core.rsource.IRDocumentPartitions.RDOC_PARTITIONS;
import static de.walware.statet.r.core.rsource.IRDocumentPartitions.RDOC_PLATFORM_SPECIF;
import static de.walware.statet.r.ui.text.rd.IRdTextTokens.COMMENT;
import static de.walware.statet.r.ui.text.rd.IRdTextTokens.PLATFORM_SPECIF;
import static de.walware.statet.r.ui.text.rd.IRdTextTokens.TASK_TAG;

import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;

import de.walware.eclipsecommons.ui.util.ColorManager;

import de.walware.statet.base.ui.sourceeditors.StatextSourceViewerConfiguration;
import de.walware.statet.base.ui.util.ISettingsChangedHandler;
import de.walware.statet.ext.ui.text.CommentScanner;
import de.walware.statet.ext.ui.text.SingleTokenScanner;
import de.walware.statet.ext.ui.text.StatextTextScanner;
import de.walware.statet.r.core.IRCoreAccess;
import de.walware.statet.r.core.RCore;
import de.walware.statet.r.ui.RUIPreferenceConstants;
import de.walware.statet.r.ui.text.rd.RdCodeScanner;
import de.walware.statet.r.ui.text.rd.RdDoubleClickStrategy;


/**
 * Default Configuration for SourceViewer of R documentations.
 */
public class RdSourceViewerConfiguration extends StatextSourceViewerConfiguration
		implements ISettingsChangedHandler {
	
	
	private RdCodeScanner fDocScanner;
	private CommentScanner fCommentScanner;
	private SingleTokenScanner fPlatformSpecifScanner;
	
	private RdDoubleClickStrategy fDoubleClickStrategy;
	
	private IRCoreAccess fRCoreAccess;
	
	
	public RdSourceViewerConfiguration(final IRCoreAccess rCoreAccess, final IPreferenceStore preferenceStore, final ColorManager colorManager) {
		super(null);
		setup(preferenceStore, colorManager);
		fRCoreAccess = rCoreAccess;
		if (fRCoreAccess == null) {
			fRCoreAccess = RCore.getWorkbenchAccess();
		}
		setScanners(initializeScanners());
	}
	
	/**
	 * Initializes the scanners.
	 */
	protected StatextTextScanner[] initializeScanners() {
		final IPreferenceStore store = getPreferences();
		final ColorManager colorManager = getColorManager();
		fDocScanner = new RdCodeScanner(colorManager, store);
		fCommentScanner = new CommentScanner(colorManager, store, fRCoreAccess.getPrefs(), RUIPreferenceConstants.Rd.TS_GROUP_ID,
				COMMENT, TASK_TAG);
		fPlatformSpecifScanner = new SingleTokenScanner(colorManager, store, RUIPreferenceConstants.Rd.TS_GROUP_ID,
				PLATFORM_SPECIF);
		
		fDoubleClickStrategy = new RdDoubleClickStrategy();
		
		return new StatextTextScanner[] { fDocScanner, fCommentScanner, fPlatformSpecifScanner };
	}
	
	@Override
	public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
		return RDOC_PARTITIONS;
	}
	
	@Override
	public String getConfiguredDocumentPartitioning(final ISourceViewer sourceViewer) {
		return RDOC_DOCUMENT_PARTITIONING;
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(final ISourceViewer sourceViewer, final String contentType) {
		return fDoubleClickStrategy;
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
		final PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(fDocScanner);
		reconciler.setDamager(dr, RDOC_DEFAULT);
		reconciler.setRepairer(dr, RDOC_DEFAULT);
		
		dr = new DefaultDamagerRepairer(fCommentScanner);
		reconciler.setDamager(dr, RDOC_COMMENT);
		reconciler.setRepairer(dr, RDOC_COMMENT);
		
		dr = new DefaultDamagerRepairer(fPlatformSpecifScanner);
		reconciler.setDamager(dr, RDOC_PLATFORM_SPECIF);
		reconciler.setRepairer(dr, RDOC_PLATFORM_SPECIF);
		
		return reconciler;
	}
	
	@Override
	public String[] getDefaultPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		return new String[] { "%", "" }; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Override
	public boolean handleSettingsChanged(final Set<String> groupIds, final Object options) {
		return super.handleSettingsChanged(groupIds, fRCoreAccess.getPrefs());
	}
	
}
