/*******************************************************************************
 * Copyright (c) 2014 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *******************************************************************************/
package com.opcoach.e34tools.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.opcoach.e34tools.Migration34Activator;
import com.opcoach.e34tools.model.CustomExtensionPoint;
import com.opcoach.e34tools.model.CustomSchema;

/**
 * The column Label and content Provider used to display information in context
 * data TreeViewer. Two instances for label provider are created : one for key,
 * one for values
 * 
 * @see ContextDataPart
 */
@SuppressWarnings("restriction")
public class PluginDataProvider extends ColumnLabelProvider implements ITreeContentProvider
{

	private Font boldFont;

	/**
	 * This value is set if this provider is used as column, else it is null for
	 * the 1st column (extension point)
	 */
	private IPluginModelBase plugin = null;

	public PluginDataProvider()
	{
		super();
		initFonts();
	}

	@Override
	public void dispose()
	{
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}

	public Object[] getElements(Object inputElement)
	{
		List<Object> objs = new ArrayList<Object>(E4MigrationRegistry.getDefault().getExtensionsToParse());
		objs.addAll(E4MigrationRegistry.getDefault().getCustomExtensionToParse());
		return objs.toArray();

	}

	public Object[] getChildren(Object parentElement)
	{

		if (parentElement instanceof IExtensionPoint)
		{

			// Must search for elements defined in this extension point */
			IExtensionPoint ep = (IExtensionPoint) parentElement;
			String uniqueIdentifier = ep.getUniqueIdentifier();
			ISchema schema = getSchema(uniqueIdentifier);

			ISchemaElement extensionElement = null;
			for (ISchemaElement e : schema.getElements())
			{
				if ("extension".equals(e.getName()))
				{
					extensionElement = e;
					break;
				}
			}
			return schema.getCandidateChildren(extensionElement);

		} else if (parentElement instanceof ISchemaElement)
		{
			/*
			 * ISchemaElement e = (ISchemaElement) parentElement; return
			 * e.getSchema().getCandidateChildren(e);
			 */
		}
		else if (parentElement instanceof CustomExtensionPoint) {
			CustomExtensionPoint cep = (CustomExtensionPoint)parentElement;
			Collection<CustomSchema> ses = cep.getSchemas();
			return ses.toArray();
		}

		return null;

	}

	private ISchema getSchema(String uniqueIdentifier)
	{
		ISchema s = PDECore.getDefault().getSchemaRegistry().getSchema(uniqueIdentifier);
		if (s == null)
		{
			Bundle b = FrameworkUtil.getBundle(this.getClass());
			IStatus st = new Status(IStatus.ERROR, b.getSymbolicName(), "Schema for " + uniqueIdentifier
					+ " can not be found. Check if extension point schema are in the launch configuration");
			Platform.getLog(b).log(st);
			System.out.println(st.getMessage());
		}
		return s;
	}

	public void setPlugin(IPluginModelBase p)
	{
		plugin = p;
	}

	@Override
	public String getText(Object element)
	{

		if (element instanceof IExtensionPoint)
		{
			IExtensionPoint ep = (IExtensionPoint) element;

			if (plugin == null)
				return ep.getUniqueIdentifier();
			else
				return "" + E4MigrationRegistry.getDefault().getInstanceNumber(ep, plugin);
		} else if (element instanceof ISchemaElement)
		{
			ISchemaElement se = (ISchemaElement) element;
			if (plugin == null)
				return se.getName();
			else
				return "" + E4MigrationRegistry.getDefault().getInstanceNumber(se, plugin);

		}
		else if (element instanceof CustomExtensionPoint) {
			CustomExtensionPoint cep = (CustomExtensionPoint) element;
			if (plugin == null)
				return cep.getUniqueId();
			else
				return "" + E4MigrationRegistry.getDefault().getInstanceNumber(cep, plugin);
		}
		else if (element instanceof CustomSchema) {
			CustomSchema cs = (CustomSchema) element;
			if (plugin == null)
				return cs.getId();
			else
				return "" + E4MigrationRegistry.getDefault().getInstanceNumber(cs, plugin);
		}

		return super.getText(element);

	}

    Color red = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

	@Override
	public Color getForeground(Object element)
	{
		// Get red if deprecated in first column, or if number is > 0 and
		// deprecated
		if (plugin == null)
			return isDeprecated(element) ? red : null;

		// We are in a plugin column... must check if value is > 1 and
		// deprecated
		String txt = getText(element);
		int val = Integer.parseInt(txt);

		return (val > 0 && isDeprecated(element)) ? red : null;
	}

	public boolean isDeprecated(Object element)
	{
		boolean deprecated = false;
		if (element instanceof IExtensionPoint)
		{
			String uniqueIdentifier = ((IExtensionPoint) element).getUniqueIdentifier();
			deprecated = getSchema(uniqueIdentifier).isDeperecated();
		} else if (element instanceof ISchemaElement)
		{
			deprecated = ((ISchemaElement) element).isDeprecated();
		}
		return deprecated;
	}

	/** Get the bold font for keys that are computed with ContextFunction */
	public Font getFont(Object element)
	{
		return (plugin != null) && (getForeground(element) == red) ? boldFont : null;

	}

	@Override
	public Image getImage(Object element)
	{
		if ((plugin == null) && isDeprecated(element)) 
			return  Migration34Activator.getDefault().getImageRegistry().get(Migration34Activator.IMG_DEPRECATED);
		
		  return null;

	}

	@Override
	public String getToolTipText(Object element)
	{

		if (isDeprecated(element))
		{
			if (element instanceof IExtensionPoint)
				return "This extension point is deprecated";
			else if (element instanceof ISchemaElement)
				return "This element is deprecated";
		}

		return "Tooltip to be defined";

	}

	@Override
	public Image getToolTipImage(Object object)
	{
		return getImage(object);
	}

	@Override
	public int getToolTipStyle(Object object)
	{
		return SWT.SHADOW_OUT;
	}

	@Override
	public Object getParent(Object element)
	{
		// Not computed
		return null;

	}

	@Override
	public boolean hasChildren(Object element)
	{

		return element instanceof IExtensionPoint;

	}

	

	private void initFonts()
	{
		FontData[] fontData = Display.getCurrent().getSystemFont().getFontData();
		String fontName = fontData[0].getName();
		FontRegistry registry = JFaceResources.getFontRegistry();
		boldFont = registry.getBold(fontName);
	}
	
	
	public class DeprecatedFilter extends ViewerFilter
	{
		static final int MODE_VIEW_ALL = 0;
		static final int MODE_VIEW_ONLY_DEPRECATED = 1;
		static final int MODE_VIEW_NO_DEPRECATED = 2;
		
		private int mode;
		public void setMode(int m)
		{
			mode = m;
		}
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element)
		{
			if (mode != MODE_VIEW_ALL)
			{
				if (mode ==  MODE_VIEW_ONLY_DEPRECATED)
					return isDeprecated(element);
				else if (mode == MODE_VIEW_NO_DEPRECATED)
					return !isDeprecated(element);
			}
			return true;
		}
	}
	
	/** Compute if all the line is with null values
	 * 
	 * @author olivier
	 *
	 */
	public class ZeroLineFilter extends ViewerFilter
	{
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element)
		{
			return isDeprecated(element);
		}
		
	}

}
