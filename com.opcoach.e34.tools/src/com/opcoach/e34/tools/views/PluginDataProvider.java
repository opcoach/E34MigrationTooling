/*******************************************************************************
 * Copyright (c) 2013 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *******************************************************************************/
package com.opcoach.e34.tools.views;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.plugin.PluginElement;
import org.eclipse.pde.internal.core.text.plugin.PluginElementNode;
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
public class PluginDataProvider extends ColumnLabelProvider implements ITreeContentProvider
{

	private static final String NO_VALUE_COULD_BE_COMPUTED = "No value could be yet computed";
	private static final Color COLOR_IF_FOUND = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
	private static final Color COLOR_IF_NOT_COMPUTED = Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA);
	private static final Object[] EMPTY_RESULT = new Object[0];
	static final String LOCAL_VALUE_NODE = "Local values managed  by this context";
	static final String INHERITED_INJECTED_VALUE_NODE = "Inherited values injected or updated using this context";

	private static final String NO_VALUES_FOUND = "No values found";
	private static final String UPDATED_IN_CLASS = "Updated in class :";
	private static final String INJECTED_IN_FIELD = "Injected in field :";
	private static final String INJECTED_IN_METHOD = "Injected in method :";

	// Image keys constants
	private static final String PUBLIC_METHOD_IMG_KEY = "icons/methpub_obj.gif";
	private static final String PUBLIC_FIELD_IMG_KEY = "icons/field_public_obj.gif";
	private static final String VALUE_IN_CONTEXT_IMG_KEY = "icons/valueincontext.gif";
	private static final String INHERITED_VARIABLE_IMG_KEY = "icons/inher_co.gif";
	private static final String LOCAL_VARIABLE_IMG_KEY = "icons/letter-l-icon.png";
	private static final String CONTEXT_FUNCTION_IMG_KEY = "icons/contextfunction.gif";
	private static final String INJECT_IMG_KEY = "icons/annotation_obj.gif";

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

	@SuppressWarnings("restriction")
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		// selectedContext = (newInput instanceof EclipseContext) ?
		// (EclipseContext) newInput : null;
	}

	public Object[] getElements(Object inputElement)
	{
		return E4MigrationRegistry.getExtensionsToParse().toArray();
		
	}

	@SuppressWarnings("restriction")
	public Object[] getChildren(Object parentElement)
	{

		if (parentElement instanceof IExtensionPoint)
		{

			// Must search for elements defined in this extension point */
			IExtensionPoint ep = (IExtensionPoint) parentElement;
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

		/*
		 * if (selectedContext == null) return EMPTY_RESULT;
		 * 
		 * if (inputElement == LOCAL_VALUE_NODE) { Collection<Object> result =
		 * new ArrayList<Object>();
		 * 
		 * result.addAll(selectedContext.localData().entrySet());
		 * 
		 * // For context function, we have to compute the value (if possible),
		 * // and display it as a standard value Map<String, Object> cfValues =
		 * new HashMap<String, Object>(); for (String key :
		 * selectedContext.localContextFunction().keySet()) try {
		 * cfValues.put(key, selectedContext.get(key)); } catch (Exception e) {
		 * cfValues.put(key, NO_VALUE_COULD_BE_COMPUTED + " (Exception : " +
		 * e.getClass().getName() + ")"); } result.addAll(cfValues.entrySet());
		 * return result.toArray();
		 * 
		 * } else if (inputElement == INHERITED_INJECTED_VALUE_NODE) { // Search
		 * for all values injected using this context but defined in // parent
		 * Collection<Object> result = new ArrayList<Object>();
		 * 
		 * // Keep only the names that are not already displayed in local //
		 * values Collection<String> localKeys =
		 * selectedContext.localData().keySet(); Collection<String>
		 * localContextFunctionsKeys = selectedContext
		 * .localContextFunction().keySet();
		 * 
		 * if (selectedContext.getRawListenerNames() != null) { for (String name
		 * : selectedContext.getRawListenerNames()) { if
		 * (!localKeys.contains(name) &&
		 * !localContextFunctionsKeys.contains(name)) result.add(name); } }
		 * return result.size() == 0 ? new String[] { NO_VALUES_FOUND } :
		 * result.toArray();
		 * 
		 * } else if (inputElement instanceof Map.Entry) { Set<Computation>
		 * listeners = getListeners(inputElement); return (listeners == null) ?
		 * null : listeners.toArray(); } else if (inputElement instanceof
		 * String) { // This is the name of a raw listener in the inherited
		 * injected // value part return selectedContext.getListeners((String)
		 * inputElement) .toArray(); }
		 * 
		 * return EMPTY_RESULT;
		 */
	}

	public void setPlugin(IPluginModelBase p)
	{
		plugin = p;
	}

	@Override
	@SuppressWarnings({ "unchecked", "restriction" })
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

		/*
		 * if (element instanceof IExtensionPoint) { IExtensionPoint ep =
		 * (IExtensionPoint) element;
		 * 
		 * if (plugin == null) { return ep.getUniqueIdentifier(); } else {
		 * 
		 * // count the number of extensions in plugin for this extension //
		 * point int count = 0; for (IPluginExtension e :
		 * plugin.getExtensions().getExtensions()) { if
		 * (e.getPoint().equals(ep.getUniqueIdentifier())) count++; }
		 * 
		 * return "" + count; } } else if (element instanceof ISchemaElement) {
		 * // Count nb of element having this name in all extensions of this
		 * plugin. ISchemaElement se = (ISchemaElement) element;
		 * 
		 * if (plugin == null) return se.getName();
		 * 
		 * 
		 * String epId = se.getSchema().getQualifiedPointId();
		 * 
		 * int count = 0; for (IPluginExtension e :
		 * plugin.getExtensions(true).getExtensions()) { //
		 * System.out.println("Comparing name of point in extension : '" +
		 * e.getPoint() + "' with ext point id : '" + epId + "'" );
		 * 
		 * if (e.getPoint().equals(epId)) { System.out.println("element found");
		 * // We are in the extension, must find nb of elements inside. for
		 * (IPluginObject po : e.getChildren()) {
		 * System.out.println("Instance of po is " + po.getClass().getName()) ;
		 * if (po instanceof PluginElement) { PluginElement pen =
		 * (PluginElement) po; System.out.println("Comparing name of node : '" +
		 * pen.getName() + "' with shema elt name : '" + se.getName() + "'" );
		 * if (pen.getName().equals(se.getName())) count++; } }
		 * 
		 * } } return "  " + count;
		 * 
		 * }
		 */

		return super.getText(element);

	}

	private Color red = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

	@Override
	public Color getForeground(Object element)
	{

		return isDeprecated(element) ? red : null;
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
		/*
		 * return (element == LOCAL_VALUE_NODE || element ==
		 * INHERITED_INJECTED_VALUE_NODE) ? boldFont : null;
		 */

		return null;

	}

	@SuppressWarnings("restriction")
	@Override
	public Image getImage(Object element)
	{

		/*
		 * if (!displayKey) // No image in value column, only in key column
		 * return null;
		 * 
		 * if (element == LOCAL_VALUE_NODE) { return selectedContext == null ?
		 * null : imgReg .get(LOCAL_VARIABLE_IMG_KEY);
		 * 
		 * } else if (element == INHERITED_INJECTED_VALUE_NODE) { return
		 * selectedContext == null ? null : imgReg
		 * .get(INHERITED_VARIABLE_IMG_KEY);
		 * 
		 * } else if (element instanceof Computation) { // For a computation :
		 * display field, method or class in key column // and // value in value
		 * column String txt = super.getText(element);
		 * 
		 * if (txt.contains("#")) return imgReg.get(PUBLIC_METHOD_IMG_KEY); else
		 * if (txt.contains("@")) return imgReg.get(CONTEXT_FUNCTION_IMG_KEY);
		 * else return imgReg.get(PUBLIC_FIELD_IMG_KEY);
		 * 
		 * } else if (element instanceof Map.Entry) { if
		 * (isAContextKeyFunction(element)) return
		 * imgReg.get(CONTEXT_FUNCTION_IMG_KEY); else { // It is a value. If it
		 * is injected somewhere, display the // inject image return
		 * hasChildren(element) ? imgReg.get(INJECT_IMG_KEY) :
		 * imgReg.get(VALUE_IN_CONTEXT_IMG_KEY); }
		 * 
		 * }
		 * 
		 * return imgReg.get(INJECT_IMG_KEY);
		 */
		return null;

	}

	@SuppressWarnings("restriction")
	@Override
	public String getToolTipText(Object element)
	{

		if (isDeprecated(element))
		{
			if (element instanceof IExtensionPoint)
				return "Ce point d'extension est deprecated";
			else if (element instanceof ISchemaElement)
				return "Cet element du schema est deprecated";
		}

		return "Tooltip à définir";
		/*
		 * if (element == LOCAL_VALUE_NODE) { return
		 * "This part contains  values set in this context and then injected here or in children\n\n"
		 * +
		 * "If the value is injected using this context, you can expand the node to see where\n\n"
		 * +
		 * "If the value is injected using a child context you can find it in the second part for this child "
		 * ; } else if (element == INHERITED_INJECTED_VALUE_NODE) { return
		 * "This part contains the values injected or updated using this context, but initialized in a parent context\n\n"
		 * + "Expand nodes to see where values are injected or updated"; } else
		 * if (isAContextKeyFunction(element)) { String key = (String)
		 * ((Map.Entry<?, ?>) element).getKey(); String fname = (String)
		 * selectedContext
		 * .localContextFunction().get(key).getClass().getCanonicalName();
		 * 
		 * return "This value is created by the Context Function : " + fname; }
		 * else { if (hasChildren(element)) return
		 * "Expand this node to see where this value is injected or updated";
		 * else { if (element instanceof Map.Entry) return
		 * "This value is set here but not injected using this context (look in children context)"
		 * ; }
		 * 
		 * }
		 * 
		 * return super.getToolTipText(element);
		 */
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

	@SuppressWarnings("restriction")
	@Override
	public boolean hasChildren(Object element)
	{

		return element instanceof IExtensionPoint;

	}

	private void initializeImageRegistry()
	{
		Bundle b = FrameworkUtil.getBundle(this.getClass());
		imgReg = new ImageRegistry();

		imgReg.put(CONTEXT_FUNCTION_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(CONTEXT_FUNCTION_IMG_KEY)));
		imgReg.put(INJECT_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(INJECT_IMG_KEY)));
		imgReg.put(PUBLIC_METHOD_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(PUBLIC_METHOD_IMG_KEY)));
		imgReg.put(PUBLIC_FIELD_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(PUBLIC_FIELD_IMG_KEY)));
		imgReg.put(PUBLIC_FIELD_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(PUBLIC_FIELD_IMG_KEY)));
		imgReg.put(LOCAL_VARIABLE_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(LOCAL_VARIABLE_IMG_KEY)));
		imgReg.put(VALUE_IN_CONTEXT_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(VALUE_IN_CONTEXT_IMG_KEY)));
		imgReg.put(INHERITED_VARIABLE_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(INHERITED_VARIABLE_IMG_KEY)));

	}

	private void initFonts()
	{
		FontData[] fontData = Display.getCurrent().getSystemFont().getFontData();
		String fontName = fontData[0].getName();
		FontRegistry registry = JFaceResources.getFontRegistry();
		boldFont = registry.getBold(fontName);
	}

}
