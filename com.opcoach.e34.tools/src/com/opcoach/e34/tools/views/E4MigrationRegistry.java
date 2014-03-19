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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.plugin.PluginElement;

/**
 * This class computes all information displayed in E4 Migration Stat view, and
 * provides helper methods to get tooltip texts It could be listener to
 * workspace and plugin object to recompute data
 */
@SuppressWarnings("restriction")
public class E4MigrationRegistry
{

	private Map<String, Integer> migrationData = null;

	private static E4MigrationRegistry INSTANCE;

	private static boolean computed = false;

	private E4MigrationRegistry()
	{
		migrationData = new HashMap<String, Integer>();

	}

	public static E4MigrationRegistry getDefault()
	{
		if (INSTANCE == null)
			INSTANCE = new E4MigrationRegistry();

		return INSTANCE;
	}

	public int getInstanceNumber(IExtensionPoint ep, IPluginModelBase plugin)
	{
		Integer result = migrationData.get(getKey(ep, plugin));
		if (result == null)
		{
			computeDataForPlugin(plugin);
			result = migrationData.get(getKey(ep, plugin));
		}
		return result;
	}

	public int getInstanceNumber(ISchemaElement se, IPluginModelBase plugin)
	{
		Integer result = migrationData.get(getKey(se, plugin));
		if (result == null)
		{
			computeDataForPlugin(plugin);
			result = migrationData.get(getKey(se, plugin));
		}
		return result;
	}

	public static Collection<IExtensionPoint> getExtensionsToParse()
	{
		// For the moment get all org.eclipse.ui extensions from current Eclipse
		// May me should be updated to get extension points from the
		// org.eclipse.ui of target platform !?
		Collection<IExtensionPoint> result = new ArrayList<IExtensionPoint>();
		for (IExtensionPoint ep : Platform.getExtensionRegistry().getExtensionPoints())
		{
			if (ep.getNamespaceIdentifier().equals("org.eclipse.ui"))
				result.add(ep);
		}
		return result;
	}

	public ISchemaElement[] getElementToParse(IExtensionPoint ep)
	{
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(ep.getUniqueIdentifier());

		// Search for extension element
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
	}

	private String getKey(IExtensionPoint ep, IPluginModelBase plugin)
	{
		return plugin.getBundleDescription().getName() + "/" + getXPath(ep);
	}

	private String getKey(ISchemaElement e, IPluginModelBase plugin)
	{
		return plugin.getBundleDescription().getName() + "/" + getXPath(e);
	}

	private String getKey(IPluginModelBase plugin, String xpath)
	{
		return plugin.getBundleDescription().getName() + "/" + xpath;
	}
	
	public String getXPath(IExtensionPoint ep) {
		return  ep.getUniqueIdentifier();
	}
	
	public String  getXPath(ISchemaElement e) {
		return e.getSchema().getPointId() + "/" + e.getName();
	}

	public static boolean isComputed()
	{
		return computed;
	}

	/**
	 * 
	 * @param xpath
	 *            the xpath to element for an extension point. For instance : "views/view"
	 *            it must not be prefixed by org.eclipse.ui
	 * @param plugins
	 *            the list of plugins to compute
	 * @return
	 */
	public int countNumberOfExtensions(String xpath, Collection<IPluginModelBase> plugins)
	{
		int result = 0;
		for (IPluginModelBase p : plugins)
		{
			String key = getKey(p, xpath);
			Integer i = migrationData.get(key);
			if (i != null)
				result += i;
		}
		return result;
	}

	private void computeDataForPlugin(IPluginModelBase plugin)
	{
		// Compute information for each extension for this plugin
		for (IExtensionPoint ep : getExtensionsToParse())
		{
			// First compute number of extension of this point in the
			// plugin.
			int nbExtensions = 0;
			String epID = ep.getUniqueIdentifier();
			for (IPluginExtension e : plugin.getExtensions(true).getExtensions())
			{
				if (e.getPoint().equals(epID))
					nbExtensions++;
			}
			migrationData.put(getKey(ep, plugin), nbExtensions);

			// Then compute number of element usage for this extension.
			for (ISchemaElement se : getElementToParse(ep))
			{
				int nbElt = 0;
				for (IPluginExtension ext : plugin.getExtensions(true).getExtensions())
				{
					// System.out.println("Comparing name of point in extension : '"
					// + e.getPoint() + "' with ext point id : '" + epId +
					// "'" );

					if (ext.getPoint().equals(epID))
					{
						// We are in the extension, must find nb of elements
						// inside.
						for (IPluginObject po : ext.getChildren())
						{
							// System.out.println("Instance of po is " +
							// po.getClass().getName()) ;
							if (po instanceof PluginElement)
							{
								PluginElement pen = (PluginElement) po;
								if (pen.getName().equals(se.getName()))
									nbElt++;
							}
						}

					}
				}

				migrationData.put(getKey(se, plugin), nbElt);

			}

		}

	}

}
