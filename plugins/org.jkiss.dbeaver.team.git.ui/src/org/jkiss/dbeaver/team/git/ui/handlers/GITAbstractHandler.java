/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.team.git.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.EditorUtils;

public abstract class GITAbstractHandler extends AbstractHandler {

    private static final Log log = Log.getLog(GITAbstractHandler.class);

    protected IResource[] getResourcesInScope(ExecutionEvent event)
        throws ExecutionException {
        try {
            IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            if (activePart instanceof IEditorPart) {
                IFile editorFile = EditorUtils.getFileFromInput(((IEditorPart) activePart).getEditorInput());
                if (editorFile != null) {
                    return new IResource[] { editorFile };
                } else {
                    return new IResource[0];
                }
            } else {
                IResource[] selectedResources = getSelectedResources(event);
                if (selectedResources.length > 0) {
                    IWorkbenchPart part = HandlerUtil.getActivePart(event);
                    return GitScopeUtil.getRelatedChanges(part, selectedResources);
                } else {
                    return new IResource[0];
                }
            }
        } catch (Exception e) {
            // ignore, we will not show the commit dialog in case the user
            // cancels the scope operation
            log.error(e);
            return null;
        }
    }

    protected Repository getRepository(boolean warn, ExecutionEvent event) {
        Shell shell = HandlerUtil.getActiveShell(event);
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof IEditorPart) {
            IFile editorFile = EditorUtils.getFileFromInput(((IEditorPart) activePart).getEditorInput());
            if (editorFile != null) {
                return SelectionUtils.getRepositoryOrWarn(new StructuredSelection(editorFile), shell);
            } else {
                return null;
            }
        } else {
            IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
            if (warn) {
                return SelectionUtils.getRepositoryOrWarn(selection, shell);
            } else {
                return SelectionUtils.getRepository(selection);
            }
        }
    }

    protected IResource[] getSelectedResources(ExecutionEvent event) {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        return SelectionUtils.getSelectedResources(selection);
    }

    protected Repository[] getRepositories(ExecutionEvent event) {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof IEditorPart) {
            IFile editorFile = EditorUtils.getFileFromInput(((IEditorPart) activePart).getEditorInput());
            if (editorFile != null) {
                Repository repository = SelectionUtils.getRepository(new StructuredSelection(editorFile));
                if (repository != null) {
                    return new Repository[] { repository };
                }
            }
            return null;
        }
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        return SelectionUtils.getRepositories(selection);
    }

}
