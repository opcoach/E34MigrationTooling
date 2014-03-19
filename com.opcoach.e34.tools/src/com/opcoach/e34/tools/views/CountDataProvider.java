package com.opcoach.e34.tools.views;

import java.util.Collection;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;

public class CountDataProvider extends ColumnLabelProvider implements
		ITreeContentProvider {
	Collection<IPluginModelBase> plugins;

	public void setPlugins(Collection<IPluginModelBase> plugins) {
		this.plugins = plugins;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return E4MigrationRegistry.getExtensionsToParse().toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {

		if (parentElement instanceof IExtensionPoint) {

			// Must search for elements defined in this extension point */
			IExtensionPoint ep = (IExtensionPoint) parentElement;
			String epUID = ep.getUniqueIdentifier();
			ISchema schema = PDECore.getDefault().getSchemaRegistry()
					.getSchema(ep.getUniqueIdentifier());

			ISchemaElement extensionElement = null;
			for (ISchemaElement e : schema.getElements()) {
				if ("extension".equals(e.getName())) {
					extensionElement = e;
					break;
				}
			}
			return schema.getCandidateChildren(extensionElement);

		} else if (parentElement instanceof ISchemaElement) {
			/*
			 * ISchemaElement e = (ISchemaElement) parentElement; return
			 * e.getSchema().getCandidateChildren(e);
			 */
		}

		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof IExtensionPoint;
	}

	@Override
	public String getText(Object element) {
		String subKey = "";
		if (element instanceof IExtensionPoint) {
			IExtensionPoint ep = (IExtensionPoint) element;
			subKey = E4MigrationRegistry.getDefault().getXPath(ep);
			return ""
					+ E4MigrationRegistry.getDefault().countNumberOfExtensions(
							subKey, plugins);
		} else if (element instanceof ISchemaElement) {

			ISchemaElement se = (ISchemaElement) element;
			subKey = E4MigrationRegistry.getDefault().getXPath(se);
			return ""
					+ E4MigrationRegistry.getDefault().countNumberOfExtensions(
							subKey, plugins);

		}

		return super.getText(element);

	}
}
