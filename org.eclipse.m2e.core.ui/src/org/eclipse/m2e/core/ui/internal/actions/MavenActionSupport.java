/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.actions;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


/**
 * MavenActionSupport
 * 
 * @author Jason van Zyl
 */
public abstract class MavenActionSupport implements IObjectActionDelegate {
  private static final Logger log = LoggerFactory.getLogger(MavenActionSupport.class);

  protected IStructuredSelection selection;

  protected IWorkbenchPart targetPart;

  protected Set<ArtifactKey> getArtifacts(IFile file, MavenPlugin plugin) {
    try {
      //TODO: mkleint: this is a bit troubling as it can take considerate amount of time
      // and it's being called in action's run() before the search dialog appearing.
      IMavenProjectFacade projectFacade = MavenPlugin.getMavenProjectRegistry().create(file, true,
          new NullProgressMonitor());
      if(projectFacade != null) {
        return ArtifactRef.toArtifactKey(projectFacade.getMavenProjectArtifacts());
      }
    } catch(Exception ex) {
      log.error("Can't read Maven project: " + ex.getMessage(), ex); //$NON-NLS-1$
    }
    return Collections.emptySet();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  protected Shell getShell() {
    Shell shell = null;
    if(targetPart != null) {
      shell = targetPart.getSite().getShell();
    }
    if(shell != null) {
      return shell;
    }

    IWorkbench workbench = M2EUIPluginActivator.getDefault().getWorkbench();
    if(workbench == null) {
      return null;
    }

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    return window == null ? null : window.getShell();
  }

  protected IFile getPomFileFromPomEditorOrViewSelection() {
    IFile file = null;

    //350136 we need to process the selection first! that's what is relevant for any popup menu action we have.
    //the processing of active editor first might have been only relevant when we had the actions in main menu, but even
    // then the popups were wrong..
    Object o = selection.iterator().next();

    if(o instanceof IProject) {
      file = ((IProject) o).getFile(IMavenConstants.POM_FILE_NAME);
    } else if(o instanceof IFile) {
      file = (IFile) o;
    }
    if(file != null) {
      return file;
    }
    //
    // If I am in the POM editor I want to get hold of the IFile that is currently in the buffer
    //
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

    if(window != null) {
      IWorkbenchPage page = window.getActivePage();
      if(page != null) {
        IEditorPart editor = page.getActiveEditor();
        if(editor != null) {
          IEditorInput input = editor.getEditorInput();
          if(input instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput) input;
            file = fileInput.getFile();
            if(file.getName().equals(IMavenConstants.POM_FILE_NAME)) {
              return file;
            }
          }
        }
      }
    }
    return null;
  }

}
