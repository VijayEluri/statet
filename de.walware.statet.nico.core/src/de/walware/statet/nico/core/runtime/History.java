/*******************************************************************************
 * Copyright (c) 2005 StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.nico.core.runtime;

import java.util.EnumSet;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

import de.walware.eclipsecommon.preferences.PreferencesUtil;

import de.walware.statet.nico.core.NicoPreferenceNodes;
import de.walware.statet.nico.core.internal.preferences.HistoryPreferences;


/**
 * Command history.
 */
public class History {

	
	private int fMaxSize = 100;
	private int fCurrentSize = 0;
	private boolean fIgnoreCommentLines;
	
	private Entry fNewest;
	private Entry fOldest;
	
	private ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);
	
	private ToolProcess fProcess;
	private IPreferenceChangeListener fPreferenceListener;
	private HistoryPreferences fCurrentPreferences;
	private IStreamListener fStreamListener;
	
	
	/**
	 * An entry of this history.
	 */
	public class Entry {
		
		private final String fCommand;
		private boolean fIsRemoved;
		private Entry fOlder;
		private Entry fNewer;
		
		private Entry(Entry older, String command) {
			
			fCommand = command;
			fOlder = older;
		}
		
		public String getCommand() {
			
			return fCommand;
		}
		
		public Entry getNewer() {
			
			synchronized (History.this) {

				if (fIsRemoved)
					return fOldest;
				return fNewer;
			}
		}
		
		public Entry getOlder() {
			
			synchronized (History.this) {
				
				if (fIsRemoved)
					return fOldest;
				return fOlder;
			}
		}
		
		/**
		 * Returns the history, this entry belong to.
		 * 
		 * @return the history.
		 */
		public History getHistory() {
			
			return History.this;
		}
	}
	
	
	public History(ToolProcess process) {
		
		fProcess = process;
		
		fStreamListener = new IStreamListener() {
			public void streamAppended(String text, IStreamMonitor monitor) {
				addCommand(text);
			}
		};
		
		fPreferenceListener = new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				checkSettings();
			}
		};
		IEclipsePreferences[] nodes = PreferencesUtil.getInstancePrefs().getPreferenceNodes(NicoPreferenceNodes.CAT_HISTORY_QUALIFIER);
		for (IEclipsePreferences node : nodes) {
			node.addPreferenceChangeListener(fPreferenceListener);
		}
		checkSettings();
	}
	
	private void checkSettings() {
		
		HistoryPreferences prefs = new HistoryPreferences(PreferencesUtil.getInstancePrefs());
		if (prefs.equals(fCurrentPreferences)) {
			return;
		}
		ToolController controller = fProcess.getController();
		if (controller != null) {
			ToolStreamProxy streams = controller.getStreams();
			fIgnoreCommentLines = prefs.filterComments();
			
			EnumSet<SubmitType> types = prefs.getSelectedTypes();
			streams.getInputStreamMonitor().addListener(fStreamListener, types);
		}
		
		synchronized (this) {
			fMaxSize = prefs.getLimitCount();;
			if (fMaxSize < fCurrentSize) {
				Object[] listeners = fListeners.getListeners();

				while (fMaxSize < fCurrentSize) {
					Entry removed = fOldest;
					fOldest = fOldest.fNewer;
					fOldest.fOlder = null;
					fCurrentSize--;
					
					for (Object obj : listeners) {
						((IHistoryListener) obj).entryRemoved(removed);
					}
				}
			}
		}
	}
	
	private void addCommand(String command) {
		
		if (command == null || isEmpty(command)) // no empty entry
			return;
		
		Entry removedEntry = null;
		Entry newEntry = null;
		
		synchronized (this) {
			if (fNewest != null && command.equals(fNewest.fCommand)) // no dublicates
				return;
		
			newEntry = new Entry(fNewest, command);
			if (fNewest != null) {
				fNewest.fNewer = newEntry;
			}
			else {
				fOldest = newEntry;
			}
			fNewest = newEntry;
			
			if (fCurrentSize == fMaxSize) {
				removedEntry = fOldest;
				fOldest = fOldest.fNewer;
				fOldest.fOlder = null;
			} 
			else {
				fCurrentSize++;
			}
		
			Object[] listeners = fListeners.getListeners();
			for (Object obj : listeners) {
				IHistoryListener listener = (IHistoryListener) obj;
				if (removedEntry != null)
					listener.entryRemoved(removedEntry);
				listener.entryAdded(newEntry);
				
			}
		}
	}
	
	/**
	 * Return the newest history entry.
	 * 
	 * @return newest entry 
	 * 		or <code>null</null>, if history is empty.
	 */
	public synchronized Entry getNewest() {
		
		return fNewest;
	}
	
	/**
	 * Return an array with all entries.
	 * <p>
	 * Don't use this method to frequently.
	 * 
	 * @return array with all entries 
	 * 		or an array with length 0, if history is empty.
	 */
	public synchronized Entry[] toArray() {
		
		Entry[] array = new Entry[fCurrentSize];
		Entry e = fOldest; 
		for (int i = 0; i < array.length; i++) {
			array[i] = e;
			e = e.fNewer;
		}
		return array;
	}

	
    /**
     * Adds the given listener to this history. 
     * Has no effect if an identical listener is already registered.
     *
     * @param listener the listener
     */
	public void addListener(IHistoryListener listener) {
		
		fListeners.add(listener);
	}
	
    /**
     * Removes the given listener from this history. 
     * Has no effect if an identical listener was not already registered.
     *
     * @param listener the listener
     */
	public void removeListener(IHistoryListener listener) {
		
		fListeners.remove(listener);
	}
	
	/** 
	 * Checks, if this command is empty or an command
	 */
	private boolean isEmpty(String command) {
		
		int length = command.length();
		for (int i = 0; i < length; i++) {
			char c = command.charAt(i);
			switch(c) {
			
			case ' ':
			case '\t':
				continue;
			case '#':
				return fIgnoreCommentLines;
			default:
				return false;
			}
		}
		return true;
	}
}
