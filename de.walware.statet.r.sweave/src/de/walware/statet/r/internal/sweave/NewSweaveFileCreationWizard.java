/*******************************************************************************
 * Copyright (c) 2007-2008 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.sweave;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import de.walware.eclipsecommons.ICommonStatusConstants;
import de.walware.eclipsecommons.ui.util.DialogUtil;

import de.walware.statet.ext.templates.TemplatesUtil;
import de.walware.statet.ext.ui.wizards.NewElementWizard;
import de.walware.statet.r.core.RResourceUnit;
import de.walware.statet.r.internal.sweave.CodeGeneration.NewFileData;
import de.walware.statet.r.sweave.Sweave;


public class NewSweaveFileCreationWizard extends NewElementWizard {
	
	
	private static class NewRweaveFileCreator extends NewFileCreator {
		
		public NewRweaveFileCreator(final IPath containerPath, final String resourceName) {
			super(containerPath, resourceName);
		}
		
		@Override
		public String getContentType(final IFile newFileHandle) {
			return Sweave.R_TEX_CONTENT_ID;
		}
		
		@Override
		protected String getInitialFileContent(final IFile newFileHandle) {
			final String lineDelimiter = TemplatesUtil.getLineSeparator(newFileHandle.getProject());
			try {
				final RResourceUnit rcu = RResourceUnit.createTempUnit(newFileHandle, Sweave.R_TEX_UNIT_TYPE_ID);
				final NewFileData data = CodeGeneration.getNewRweaveTexDocContent(rcu, lineDelimiter);
				if (data != null) {
					fSelectionStart = data.selectionStart;
					fSelectionEnd = data.selectionEnd;
					return data.content;
				}
			} catch (final CoreException e) {
				SweavePlugin.logError(ICommonStatusConstants.INTERNAL_TEMPLATE, "An error occured when applying template to new Sweave file.", e); //$NON-NLS-1$
			}
			return null;
		}
	}
	
	
	private NewSweaveFileCreationWizardPage fFirstPage;
	private NewFileCreator fNewSweaveFile;
	
	
	public NewSweaveFileCreationWizard() {
		setDialogSettings(DialogUtil.getDialogSettings(SweavePlugin.getDefault(), "NewElementWizard")); //$NON-NLS-1$
		setWindowTitle(Messages.NewSweaveFileWizard_title);
	}
	
	@Override
	public void addPages() {
		super.addPages();
		fFirstPage = new NewSweaveFileCreationWizardPage(getSelection());
		addPage(fFirstPage);
	}
	
	@Override
	protected ISchedulingRule getSchedulingRule() {
		final ISchedulingRule rule = createRule(fNewSweaveFile.getFileHandle());
		if (rule != null)
			return rule;
		
		return super.getSchedulingRule();
	}
	
	@Override
	public boolean performFinish() {
		// befor super, so it can be used in getSchedulingRule
		fNewSweaveFile = new NewRweaveFileCreator(
				fFirstPage.fResourceGroup.getContainerFullPath(),
				fFirstPage.fResourceGroup.getResourceName() );
		
		final boolean result = super.performFinish();
		
		final IFile newFile = fNewSweaveFile.getFileHandle();
		if (result && newFile != null) {
			// select and open file
			selectAndReveal(newFile);
			openResource(fNewSweaveFile);
		}
		
		return result;
	}
	
	@Override
	protected void doFinish(final IProgressMonitor monitor) throws InterruptedException, CoreException, InvocationTargetException {
		try {
			monitor.beginTask("Create new file...", 1000); //$NON-NLS-1$
			
			fNewSweaveFile.createFile(new SubProgressMonitor(monitor, 900) );
			
			fFirstPage.saveSettings();
			monitor.worked(100);
		}
		finally {
			monitor.done();
		}
	}
	
}
