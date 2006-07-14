/*******************************************************************************
 * Copyright (c) 2005-2006 StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.ui.internal.preferences;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.preference.IPreferenceStore;

import de.walware.eclipsecommons.ui.preferences.ConfigurationBlockPreferencePage;
import de.walware.eclipsecommons.ui.util.ColorManager;
import de.walware.eclipsecommons.preferences.PreferencesUtil;

import de.walware.statet.ext.ui.editors.StatextSourceViewerConfiguration;
import de.walware.statet.ext.ui.preferences.AbstractSyntaxColoringBlock;
import de.walware.statet.ext.ui.preferences.AbstractSyntaxColoringBlock.SyntaxItem;
import de.walware.statet.r.ui.internal.RUIPlugin;
import de.walware.statet.r.ui.RUIPreferenceConstants;
import de.walware.statet.r.ui.editors.RSourceViewerConfiguration;
import de.walware.statet.r.ui.editors.RdDocumentSetupParticipant;
import de.walware.statet.r.ui.editors.RdSourceViewerConfiguration;


public class RdSyntaxColoringPreferencePage extends ConfigurationBlockPreferencePage<AbstractSyntaxColoringBlock> {

		
	public RdSyntaxColoringPreferencePage() {

		setPreferenceStore(RUIPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected AbstractSyntaxColoringBlock createConfigurationBlock() {

		AbstractSyntaxColoringBlock syntaxBlock = new AbstractSyntaxColoringBlock() {

			@Override
			protected String getPreviewFileName() {

				return "RdSyntaxColoringPreviewCode.txt"; //$NON-NLS-1$
			}

			@Override
			protected StatextSourceViewerConfiguration getSourceViewerConfiguration(
					ColorManager colorManager, IPreferenceStore store) {
				
				return new RdSourceViewerConfiguration(colorManager, 
						RSourceViewerConfiguration.createCombinedPreferenceStore(
								store, PreferencesUtil.getDefaultPrefs())
						);
			}
			
			@Override
			protected IDocumentSetupParticipant getDocumentSetupParticipant() {
				
				return new RdDocumentSetupParticipant();
			}
			
		};
		syntaxBlock.setupItems(
			new String[] { Messages.RdSyntaxColoring_CodeCategory_label, Messages.RdSyntaxColoring_CommentsCategory_label },
			new SyntaxItem[][] { {
				new SyntaxItem(Messages.RdSyntaxColoring_Default_label, Messages.RdSyntaxColoring_Default_description, RUIPreferenceConstants.Rd.TS_DEFAULT_ROOT),
//				new SyntaxItem(Messages.RdSyntaxColoring_Verbatim_label, Messages.RdSyntaxColoring_Verbatim_description, RUIPreferenceConstants.Rd.TS_VERBATIM_ROOT),
				new SyntaxItem(Messages.RdSyntaxColoring_SectionTag_label, Messages.RdSyntaxColoring_SectionTag_description, RUIPreferenceConstants.Rd.TS_SECTION_TAG_ROOT),
				new SyntaxItem(Messages.RdSyntaxColoring_SubSectionTag_label, Messages.RdSyntaxColoring_SubSectionTag_description, RUIPreferenceConstants.Rd.TS_SUBSECTION_TAG_ROOT),
				new SyntaxItem(Messages.RdSyntaxColoring_OtherTag_label, Messages.RdSyntaxColoring_OtherTag_description, RUIPreferenceConstants.Rd.TS_OTHER_TAG_ROOT),
				new SyntaxItem(Messages.RdSyntaxColoring_UnlistedTag_label, Messages.RdSyntaxColoring_UnlistedTag_description, RUIPreferenceConstants.Rd.TS_UNLISTED_TAG_ROOT),
				new SyntaxItem(Messages.RdSyntaxColoring_Brackets_label, Messages.RdSyntaxColoring_Brackets_description, RUIPreferenceConstants.Rd.TS_BRACKETS_ROOT),
				
				new SyntaxItem(Messages.RdSyntaxColoring_PlatformSpecif_label, Messages.RdSyntaxColoring_PlatformSpecif_description, RUIPreferenceConstants.Rd.TS_PLATFORM_SPECIF_ROOT),
		}, {
				new SyntaxItem(Messages.RdSyntaxColoring_Comment_label, Messages.RdSyntaxColoring_Comment_description, RUIPreferenceConstants.Rd.TS_COMMENT_ROOT),
				new SyntaxItem(Messages.RdSyntaxColoring_TaskTag_label, Messages.RdSyntaxColoring_TaskTag_description, RUIPreferenceConstants.Rd.TS_TASK_TAG_ROOT),
		} } );

		return syntaxBlock;
	}
	
}