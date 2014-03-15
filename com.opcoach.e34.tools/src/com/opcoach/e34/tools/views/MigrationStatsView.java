package com.opcoach.e34.tools.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

public class MigrationStatsView extends ViewPart implements ISelectionListener
{

	public MigrationStatsView()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(IViewSite site) throws PartInitException
	{
		// TODO Auto-generated method stub
		super.init(site);
		site.getPage().addSelectionListener(this);
	}

	@Override
	public void dispose()
	{
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	private Collection<IPluginModelBase> selectedPlugins = new ArrayList<IPluginModelBase>();
	private TreeViewer tv;

	@Override
	public void createPartControl(Composite parent)
	{

		parent.setLayout(new GridLayout(1, false));

		tv = new TreeViewer(parent);
		PluginDataProvider provider = new PluginDataProvider();
		tv.setContentProvider(provider);
		tv.setLabelProvider(provider);
		tv.setInput(Platform.getExtensionRegistry());

		final Tree cTree = tv.getTree();
		cTree.setHeaderVisible(true);
		cTree.setLinesVisible(true);
		cTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// tv.setInput(a);
		tv.setInput("Foo"); // getElements starts alone

		// Create the first column, containing extension points
		TreeViewerColumn keyCol = new TreeViewerColumn(tv, SWT.NONE);
		keyCol.getColumn().setWidth(300);
		keyCol.getColumn().setText("Extension Points");
		PluginDataProvider labelProvider = new PluginDataProvider();
		keyCol.setLabelProvider(labelProvider);
		keyCol.getColumn().setToolTipText("Extension point in org.eclipse.ui to be migrated");

		createPluginColumns();

		// Open all the tree
		tv.expandAll();

		ColumnViewerToolTipSupport.enableFor(tv);

	}

	private void createPluginColumns()
	{
		// Must reate only missing columns.
		for (IPluginModelBase pm : selectedPlugins)
		{
			// Create the second column for the value

			// Add columns in the tree one column per selected plugin.
			// Create the first column for the key
			TreeViewerColumn keyCol = new TreeViewerColumn(tv, SWT.NONE);
			keyCol.getColumn().setWidth(300);
			keyCol.getColumn().setText(pm.getBundleDescription().getName());
			PluginDataProvider labelProvider = new PluginDataProvider();

			labelProvider.setPlugin(pm);
			;
			keyCol.setLabelProvider(labelProvider);
			keyCol.getColumn().setToolTipText("tooltip a definir");
			/*
			 * keyCol.getColumn().addSelectionListener(
			 * getHeaderSelectionAdapter(tv, keyCol.getColumn(), 0,
			 * keyLabelProvider));
			 */

		}
	}

	@Override
	public void setFocus()
	{
		// TODO Auto-generated method stub

	}

	

	@SuppressWarnings("restriction")
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{

		if (selection.isEmpty())
			return;

		// Try to find selected plugins in selection
		if (selection instanceof IStructuredSelection)
		{
			IStructuredSelection ss = (IStructuredSelection) selection;
			selectedPlugins.clear();

			for (Iterator it = ss.iterator(); it.hasNext();)
			{
				Object selected = it.next();
				IProject proj = (IProject) Platform.getAdapterManager().getAdapter(selected, IProject.class);
				if (proj != null)
				{
					IPluginModelBase m = PDECore.getDefault().getModelManager().findModel(proj);
					if (m != null)
					{
						System.out.println("Selected plugin is : " + m.getBundleDescription().getName());
						selectedPlugins.add(m);
					}
				}
			}

			if (!selectedPlugins.isEmpty())
			{
				createPluginColumns();

				tv.refresh();
			}

		}

	}
}
