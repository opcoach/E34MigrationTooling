/*******************************************************************************
 * Copyright (c) 2017 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *     Jérôme FALLEVOZ (Tech Advantage): export csv file
 *******************************************************************************/
package com.opcoach.e34tools.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.opcoach.e34tools.Migration34Activator;
import com.opcoach.e34tools.io.CvsExport;
import com.opcoach.e34tools.model.CustomExtensionPoint;

@SuppressWarnings("restriction")
public class MigrationStatsE4View
{

	private static final String COUNT_COLUMN = "Count";

	private static final String HELP_TXT = "This window displays statistics regarding an E4 migration."
			+ "\n\nUSAGE\n-------"
			+ "\nSelect one plugin or several plugins in your package explorer and get statistics."
			+ "\nYOU MUST IMPORT THE latest org.eclipse.ui plugin (version 4.X) in your workspace"
			+ "\n\nCONTENTS\n----------" + "\nThe first column contains the list of org.eclipse.ui extension points."
			+ "\nMiddle columns contains sums of extensions point occurency" + "\nLast column displays the sum."
			+ "\nDeprecated extension points or elements are displayed in red."
			+ "\nThe upper dashboards summarizes information"
			+ "\nThe more red you have, the more difficult will be your migration." + "\n\nFILTERS\n-------"
			+ "\nThe filter buttons can filter lines that have a nul total count, or deprecated elements";

	private MigrationDataComparator comparator;

	private final Map<IPluginModelBase, TreeViewerColumn> columnsCache = new HashMap<IPluginModelBase, TreeViewerColumn>();

	private TreeViewerColumn countCol = null;

	/**
	 * List of prefix to be removed in column name : with 'org.eclipse' it will
	 * display only : emf.ecore in the column name
	 */
	private Collection<String> prefixFilters = new ArrayList<String>();
	private String prefixFiltersString = "";

	private final Map<String, Label> countLabels = new HashMap<String, Label>();

	private PluginDataProvider provider;

	private CountDataProvider countProvider;

	// selected plugins must not appear twice ! (Fix issue #8)
	private HashSet<IPluginModelBase> currentSelectedPlugins;


	private Collection<IPluginModelBase> displayedPlugins = Collections.emptyList();

	private TreeViewer tv;

	private FilterStats filter;

	private Group maindashboard;

	private Group deprdashboard;

	@Inject
	public MigrationStatsE4View(Composite parent)
	{
		parent.setLayout(new GridLayout(2, false));

		provider = new PluginDataProvider();

		createDashBoard(parent);
		createDeprecatedDashBoard(parent);
		updateDashboard();

		createToolBar(parent);

		tv = new TreeViewer(parent);
		tv.setContentProvider(provider);
		tv.setLabelProvider(provider);
		tv.setInput(Platform.getExtensionRegistry());

		final Tree cTree = tv.getTree();
		cTree.setHeaderVisible(true);
		cTree.setLinesVisible(true);
		cTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1)); // hspan=2
		tv.setInput("Foo"); // getElements starts alone

		// Create the first column, containing extension points
		TreeViewerColumn epCol = new TreeViewerColumn(tv, SWT.NONE);
		epCol.getColumn().setWidth(300);
		epCol.getColumn().setText("Extension Points");
		PluginDataProvider labelProvider = new PluginDataProvider();
		epCol.setLabelProvider(labelProvider);
		epCol.getColumn().setToolTipText("Extension points defined in org.eclipse.ui to be migrated");
		epCol.getColumn().addSelectionListener(getHeaderSelectionAdapter(tv, epCol.getColumn(), 0, labelProvider));
		comparator = new MigrationDataComparator(0, labelProvider);
		tv.setComparator(comparator);

		// Set the filters.
		filter = new FilterStats();
		tv.setFilters(new ViewerFilter[] { filter });

		// Open all the tree
		tv.expandAll();

		ColumnViewerToolTipSupport.enableFor(tv);

		parent.layout();

	}

	private void createToolBar(Composite parent)
	{
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.LEFT);

		ToolItem export = new ToolItem(tb, SWT.PUSH);
		export.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_EXPORT));
		export.setToolTipText("Export");
		export.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					export(tv);
				}
			});

		ToolItem expandAll = new ToolItem(tb, SWT.PUSH);
		expandAll.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_EXPAND));
		expandAll.setToolTipText("Expand all nodes");
		expandAll.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					tv.expandAll();
				}
			});
		ToolItem collapseAll = new ToolItem(tb, SWT.PUSH);
		collapseAll
				.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_COLLAPSE));
		collapseAll.setToolTipText("Collapse nodes");
		collapseAll.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					tv.collapseAll();
				}
			});

		// Add filters...
		new ToolItem(tb, SWT.SEPARATOR);

		ToolItem ti = new ToolItem(tb, SWT.CHECK | SWT.BORDER);
		ti.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_FILTER));
		ti.setToolTipText("Filter empty lines");
		ti.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					// filter empty lines...
					filter.setFilterEmptyLines(!filter.getFilterEmptyLines());
					tv.refresh();
				}
			});

		// Create filter deprecated
		ToolItem item = new ToolItem(tb, SWT.DROP_DOWN);
		item.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_DEPRECATED));
		item.setToolTipText("Filter here deprecated extensions points");

		DropdownSelectionListener dslistener = new DropdownSelectionListener(item);
		dslistener.add(FilterStats.SHOW_ALL);
		dslistener.add(FilterStats.REMOVE_DEPRECATED);
		dslistener.add(FilterStats.ONLY_DEPRECATED);
		item.addSelectionListener(dslistener);

		// Create the prefix filter button
		ToolItem prefixTitle = new ToolItem(tb, SWT.PUSH | SWT.BORDER);
		prefixTitle.setImage(
				Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_PREFIX_COLUMNTITLE));
		prefixTitle.setToolTipText("Enter here prefixes to reduce the size of column titles");
		prefixTitle.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					// ask for the prefix to remove for column names
					askForColumnPrefixes();
				}
			});

		// Add CustomExtension button
		new ToolItem(tb, SWT.SEPARATOR);

		ToolItem extItem = new ToolItem(tb, SWT.PUSH | SWT.BORDER);
		extItem.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_EXTENSION));
		extItem.setToolTipText("Add a custom extensions points");
		extItem.addSelectionListener(new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e)
				{
					askForAdditionalExtension();
				}
			});

		// Add help button
		new ToolItem(tb, SWT.SEPARATOR);

		ToolItem th = new ToolItem(tb, SWT.PUSH | SWT.BORDER);
		th.setImage(Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_HELP));
		th.setToolTipText(HELP_TXT);

		th.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Help", HELP_TXT);
				}
			});

	}

	private void askForColumnPrefixes()
	{
		// filter empty lines...
		InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), "Column name prefix filters",
				"Enter a comma separated list of prefix filters to apply on column names", prefixFiltersString, null);

		if (dlg.open() == Dialog.OK)
		{
			computePrefixFilterList(dlg.getValue());

			for (IPluginModelBase p : columnsCache.keySet())
			{
				TreeViewerColumn tc = columnsCache.get(p);
				String newTitle = getColumnName(p);
				if (!tc.getColumn().isDisposed() && !tc.getColumn().getText().equals(newTitle))
				{
					tc.getColumn().setText(newTitle);
					tc.getColumn().pack();
				}
			}
			tv.refresh();
		}
	}

	private void askForAdditionalExtension()
	{
		// ask for additional extension
		InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), "Custom extension statistics",
				"Give the extension point as you want to follow in statistic", "", null);

		if (dlg.open() == Dialog.OK)
		{
			String value = dlg.getValue();
			E4MigrationRegistry.getDefault().addCustomExtensionPointIdentifier(value);
			tv.refresh();
		}
	}

	private void computePrefixFilterList(String value)
	{
		prefixFiltersString = value;
		StringTokenizer stk = new StringTokenizer(value, ",");

		prefixFilters.clear();
		if (prefixFiltersString.trim().length() == 0)
			return;

		do
		{
			String s = stk.nextToken();
			prefixFilters.add(s.trim());
		} while (stk.hasMoreTokens());

	}

	protected void export(TreeViewer tv)
	{
		Shell parent = Display.getCurrent().getActiveShell();
		FileDialog dialog = new FileDialog(parent, SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*.csv" });
		dialog.setOverwrite(true);
		String filePath = dialog.open();
		if (filePath != null)
		{
			Collection<IExtensionPoint> extPts = E4MigrationRegistry.getDefault().getExtensionsToParse();
			Collection<CustomExtensionPoint> cExt = E4MigrationRegistry.getDefault().getCustomExtensionToParse();
			CvsExport cvsE = new CvsExport();
			try
			{
				cvsE.save(filePath, extPts, cExt, displayedPlugins);
			} catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

	}

	private void createDashBoard(Composite parent)
	{
		maindashboard = new Group(parent, SWT.BORDER);
		maindashboard.setLayout(new FillLayout());
		maindashboard.setText("Usual Extension points");
		maindashboard.setLayout(new GridLayout(2, false));

		createCounter(maindashboard, "views/view : ", "org.eclipse.ui.views/view");
		createCounter(maindashboard, "editors/editor : ", "org.eclipse.ui.editors/editor");
		createCounter(maindashboard, "preferencePages/page : ", "org.eclipse.ui.preferencePages/page");
		createCounter(maindashboard, "propertyPages/page : ", "org.eclipse.ui.propertyPages/page");
		createCounter(maindashboard, "commands/command : ", "org.eclipse.ui.commands/command");
		createCounter(maindashboard, "handlers/handler : ", "org.eclipse.ui.handlers/handler");
		createCounter(maindashboard, "menus/menuContribution : ", "org.eclipse.ui.menus/menuContribution");
		createCounter(maindashboard, "newWizards/wizard : ", "org.eclipse.ui.newWizards/wizard");
		createCounter(maindashboard, "importWizards/wizard : ", "org.eclipse.ui.importWizards/wizard");
		createCounter(maindashboard, "exportWizards/wizard : ", "org.eclipse.ui.exportWizards/wizard");

		maindashboard.pack();

	}

	private void createDeprecatedDashBoard(Composite parent)
	{
		deprdashboard = new Group(parent, SWT.BORDER);
		deprdashboard.setLayout(new FillLayout());
		deprdashboard.setText("Deprecated Extension points");
		deprdashboard.setLayout(new GridLayout(4, false));

		for (IExtensionPoint iep : E4MigrationRegistry.getDefault().getExtensionsToParse())
		{
			// Search for deprecated elements.
			for (Object node : provider.getChildren(iep))
			{
				if (node instanceof ISchemaElement)
				{
					ISchemaElement se = (ISchemaElement) node;
					if (se.isDeprecated())
						createCounter(deprdashboard, iep.getSimpleIdentifier() + "/" + se.getName() + " : ",
								iep.getUniqueIdentifier() + "/" + se.getName());
				}
			}
		}

		deprdashboard.pack();

	}

	/**
	 * Create the counter label and remember of it to compute it according to
	 * selection
	 * 
	 * @param parent
	 * @param title
	 *            : the title for the counter
	 * @param xpath
	 *            : the xpath to search for in the plugin xml : ex : views/view,
	 *            editors/editor must not give the full extension point name,
	 *            only simple name
	 */
	public void createCounter(Composite parent, String title, String xpath)
	{
		Label titleLabel = new Label(parent, SWT.NONE);
		titleLabel.setText(title);
		titleLabel.setToolTipText(xpath);
		Label valueLabel = new Label(parent, SWT.NONE);
		valueLabel.setText("???");
		countLabels.put(xpath, valueLabel);

	}

	/**
	 * Just update the contents of dashboard according to selected plugins
	 */
	private void updateDashboard()
	{
		E4MigrationRegistry reg = E4MigrationRegistry.getDefault();

		for (String xpath : countLabels.keySet())
		{
			int count = reg.countNumberOfExtensions(xpath, displayedPlugins);
			Label label = countLabels.get(xpath);
			label.setText("" + count);
			if (label.getParent() == deprdashboard)
			{
				// stand in the deprecated group.. set red if > 0
				if (count > 0)
				{
					label.setForeground(provider.red);
				} else
				{
					label.setForeground(null);
				}
			}
		}

		maindashboard.pack();
		deprdashboard.pack();
	}

	private void createPluginColumns(IPluginModelBase pm)
	{
		// Add columns in the tree one column per selected plugin.
		// Create the first column for the key
		TreeViewerColumn col = new TreeViewerColumn(tv, SWT.NONE);
		TreeColumn swtCol = col.getColumn();
		swtCol.setText(getColumnName(pm));
		swtCol.setAlignment(SWT.CENTER);
		PluginDataProvider labelProvider = new PluginDataProvider();

		labelProvider.setPlugin(pm);
		col.setLabelProvider(labelProvider);
		swtCol.setToolTipText(pm.getBundleDescription().getName());
		swtCol.pack();

		columnsCache.put(pm, col);
	}

	private String getColumnName(IPluginModelBase pm)
	{
		BundleDescription bundleDescription = pm.getBundleDescription();
		String pluginName = (bundleDescription != null) ? bundleDescription.getName() : "NO NAME ???";
		for (String prefix : prefixFilters)
		{
			if (pluginName.startsWith(prefix))
			{
				if (prefix.length() < pluginName.length())
				{
					pluginName = pluginName.substring(prefix.length());
					// Adjust to remove the first '.' if present
					if ((pluginName.startsWith(".")) && pluginName.length() > 2)
						pluginName = pluginName.substring(1);
				}
				break;
			}
		}
		return pluginName;
	}

	private void createCountDataColumns(Collection<IPluginModelBase> pmbs)
	{
		// Always Remove column to recreate it at the end
		if (countCol != null)
		{
			countCol.getColumn().dispose();
			countCol = null;
		}

		// Add columns in the tree one column per selected plugin.
		// Create the first column for the key
		// Only if there are plugins selected
		if (pmbs.size() > 0)
		{
			countCol = new TreeViewerColumn(tv, SWT.NONE);
			TreeColumn swtCol = countCol.getColumn();
			swtCol.setText(COUNT_COLUMN);
			swtCol.setAlignment(SWT.CENTER);
			countProvider = new CountDataProvider();

			countProvider.setPlugins(pmbs);
			countCol.setLabelProvider(countProvider);
			swtCol.setToolTipText("Sum the line");
			swtCol.pack();
		}
	}

	@Focus
	public void setFocus()
	{
		tv.getControl().setFocus();
	}

	@Inject
	@Optional
	public void selectionChanged(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection ss)
	{
		if (ss == null)
			return;

		currentSelectedPlugins = new HashSet<IPluginModelBase>();
		for (@SuppressWarnings("unchecked")
		Iterator<IPluginModelBase> it = ss.iterator(); it.hasNext();)
		{
			Object selected = it.next();
			IProject proj = (IProject) Platform.getAdapterManager().getAdapter(selected, IProject.class);
			if (proj != null)
			{
				IPluginModelBase m = PDECore.getDefault().getModelManager().findModel(proj);
				if (m != null)
				{
					currentSelectedPlugins.add(m);
				} else
				{
					// Try to see if it is a feature.
					IFeatureModel fm = PDECore.getDefault().getFeatureModelManager().getFeatureModel(proj);
					if (fm != null)
					{
						for (IFeaturePlugin fp : fm.getFeature().getPlugins())
						{
							IPluginModelBase pm = PDECore.getDefault().getModelManager().findModel(fp.getId());
							if (pm != null)
								currentSelectedPlugins.add(pm);
						}
					}

				}
			}
		}

		mergeTableViewerColumns(currentSelectedPlugins);

		if (tv != null)
		{
			// Must refresh without filter and then refilter...
			tv.setFilters(new ViewerFilter[] {});
			tv.setFilters(new ViewerFilter[] { filter });

		}

		updateDashboard();

	}

	private void mergeTableViewerColumns(Collection<IPluginModelBase> currentSelectedPlugins)
	{
		// Search for plugins to be added or removed
		Collection<IPluginModelBase> toBeAdded = new ArrayList<IPluginModelBase>();
		Collection<IPluginModelBase> toBeRemoved = new ArrayList<IPluginModelBase>();

		for (IPluginModelBase p : currentSelectedPlugins)
		{
			if (!displayedPlugins.contains(p))
			{
				toBeAdded.add(p);
			}
		}

		for (IPluginModelBase p : displayedPlugins)
		{
			if (!currentSelectedPlugins.contains(p))
			{
				toBeRemoved.add(p);
			}
		}

		// Now remove and add columns in viewer..
		for (IPluginModelBase p : toBeRemoved)
		{
			TreeViewerColumn tc = columnsCache.get(p);
			if (tc != null)
			{
				tc.getColumn().dispose();
			}
		}

		for (IPluginModelBase p : toBeAdded)
		{
			createPluginColumns(p);
		}

		createCountDataColumns(currentSelectedPlugins);

		displayedPlugins = currentSelectedPlugins;

	}

	private SelectionAdapter getHeaderSelectionAdapter(final TreeViewer viewer, final TreeColumn column,
			final int columnIndex, final ILabelProvider textProvider)
	{
		SelectionAdapter selectionAdapter = new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					viewer.setComparator(comparator);
					comparator.setColumn(columnIndex);
					comparator.setLabelProvider(textProvider);
					viewer.getTree().setSortDirection(comparator.getDirection());
					viewer.getTree().setSortColumn(column);
					viewer.refresh();
				}
			};
		return selectionAdapter;
	}

	/**
	 *  * This class provides the "drop down" functionality for our dropdown
	 * tool items.  
	 */
	private class DropdownSelectionListener extends SelectionAdapter
	{
		private final Menu menu;

		private MenuItem currentSelected;

		public DropdownSelectionListener(ToolItem dropdown)
		{
			menu = new Menu(dropdown.getParent().getShell());
		}

		/**
		 * Adds an item to the dropdown list     * @param item the item to add
		 *    
		 */
		public void add(String item)
		{
			MenuItem menuItem = new MenuItem(menu, SWT.CHECK);
			menuItem.setText(item);
			menuItem.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent event)
					{
						MenuItem selected = (MenuItem) event.widget;
						if ((currentSelected != null) && (currentSelected != selected))
						{
							currentSelected.setSelection(false);
						}
						selected.setSelection(true);
						currentSelected = selected;
						// Update the filter and refresh
						filter.setFilterDeprecated(selected.getText());
						tv.refresh();
					}
				});
		}

		/**
		 * Called when either the button itself or the dropdown arrow is clicked
		 *  
		 */
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			// If they clicked the arrow, we show the list
			if (event.detail == SWT.ARROW)
			{
				// Determine where to put the dropdown list
				ToolItem item = (ToolItem) event.widget;
				Rectangle rect = item.getBounds();
				Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
				menu.setLocation(pt.x, pt.y + rect.height);
				menu.setVisible(true);
			} else
			{
				// Nothing to do...
				// System.out.println("button pressed");
			}
		}
	}

	class FilterStats extends ViewerFilter
	{
		static final String EMPTY_LINES = "Filter empty lines";

		static final String SHOW_ALL = "Show all";

		static final String REMOVE_DEPRECATED = "Remove deprecated";

		static final String ONLY_DEPRECATED = "Show only deprecated";

		private boolean filterEmptyLines = false;

		private String filterDeprecated = SHOW_ALL;

		void setFilterEmptyLines(boolean fel)
		{
			filterEmptyLines = fel;
		}

		public boolean getFilterEmptyLines()
		{
			return filterEmptyLines;
		}

		void setFilterDeprecated(String mode)
		{
			filterDeprecated = mode;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element)
		{
			if (filterDeprecated != SHOW_ALL)
			{

				// Must filter the deprecated and may be empty lines
				boolean elementIsDeprecated = provider.isDeprecated(element);
				if ((filterDeprecated == ONLY_DEPRECATED) && !elementIsDeprecated)
				{
					return false;
				}
				if ((filterDeprecated == REMOVE_DEPRECATED) && elementIsDeprecated)
				{
					return false;
				}

				// Can now check if line is empty

				return countProvider == null ? true : !(filterEmptyLines && "0".equals(countProvider.getText(element)));

			} else
			{
				return countProvider == null ? true : !(filterEmptyLines && "0".equals(countProvider.getText(element)));
			}

		}

	}

}
