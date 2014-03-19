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
package com.opcoach.e34.tools.views;

import javax.inject.Inject;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
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

	/*private static final String NO_VALUE_COULD_BE_COMPUTED = "No value could be yet computed";
	private static final Color COLOR_IF_FOUND = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
	private static final Color COLOR_IF_NOT_COMPUTED = Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA);
	private static final Object[] EMPTY_RESULT = new Object[0];
	static final String LOCAL_VALUE_NODE = "Local values managed  by this context";
	static final String INHERITED_INJECTED_VALUE_NODE = "Inherited values injected or updated using this context";

	private static final String NO_VALUES_FOUND = "No values found";
	private static final String UPDATED_IN_CLASS = "Updated in class :";
	private static final String INJECTED_IN_FIELD = "Injected in field :";
	private static final String INJECTED_IN_METHOD = "Injected in method :";
	*/

	// Image keys constants
	/*private static final String PUBLIC_METHOD_IMG_KEY = "icons/methpub_obj.gif";
	private static final String PUBLIC_FIELD_IMG_KEY = "icons/field_public_obj.gif";
	private static final String VALUE_IN_CONTEXT_IMG_KEY = "icons/valueincontext.gif";
	private static final String INHERITED_VARIABLE_IMG_KEY = "icons/inher_co.gif";
	private static final String LOCAL_VARIABLE_IMG_KEY = "icons/letter-l-icon.png";
	private static final String CONTEXT_FUNCTION_IMG_KEY = "icons/contextfunction.gif";
	private static final String INJECT_IMG_KEY = "icons/annotation_obj.gif"; */
	private static final String IMG_DEPRECATED = "icons/deprecated.gif";

	private ImageRegistry imgReg;

	private Font boldFont;

	/**
	 * This value is set if this provider is used as column, else it is null for
	 * the 1st column (extension point)
	 */
	private IPluginModelBase plugin = null;

	@Inject
	public PluginDataProvider()
	{
		super();
		initFonts();
		initializeImageRegistry();
	}

	@Override
	public void dispose()
	{
		imgReg = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}

	public Object[] getElements(Object inputElement)
	{
		return E4MigrationRegistry.getExtensionsToParse().toArray();
		
	}

	public Object[] getChildren(Object parentElement)
	{

		if (parentElement instanceof IExtensionPoint)
		{

			// Must search for elements defined in this extension point */
			IExtensionPoint ep = (IExtensionPoint) parentElement;
			String epUID = ep.getUniqueIdentifier();
			ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(ep.getUniqueIdentifier());

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

		return null;

	
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


		return super.getText(element);

	}

	private Color red = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

	@Override
	public Color getForeground(Object element)
	{
		// Get red if deprecated in first column, or if number is > 0 and deprecated
		if (plugin == null)
			return isDeprecated(element) ? red : null;
		
		// We are in a plugin column... must check if value is > 1 and deprecated
		String txt = getText(element);
		int val = Integer.parseInt(txt);
		
		return (val > 0 && isDeprecated(element)) ? red : null;
	}

	private boolean isDeprecated(Object element)
	{
		boolean deprecated = false;
		if (element instanceof IExtensionPoint)
		{
			deprecated = PDECore.getDefault().getSchemaRegistry().getSchema(((IExtensionPoint) element).getUniqueIdentifier())
					.isDeperecated();
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
		return ((plugin == null) && isDeprecated(element)) ? imgReg.get(IMG_DEPRECATED) : null;


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
		/*
		 * if (element == LOCAL_VALUE_NODE || element ==
		 * INHERITED_INJECTED_VALUE_NODE) return null;
		 */
		// Not computed
		return null;

	}

	@Override
	public boolean hasChildren(Object element)
	{

		return element instanceof IExtensionPoint;

	}

	private void initializeImageRegistry()
	{
		Bundle b = FrameworkUtil.getBundle(this.getClass());
		imgReg = new ImageRegistry();

		imgReg.put(IMG_DEPRECATED, ImageDescriptor.createFromURL(b.getEntry(IMG_DEPRECATED)));
	
	}

	private void initFonts()
	{
		FontData[] fontData = Display.getCurrent().getSystemFont().getFontData();
		String fontName = fontData[0].getName();
		FontRegistry registry = JFaceResources.getFontRegistry();
		boldFont = registry.getBold(fontName);
	}

}
