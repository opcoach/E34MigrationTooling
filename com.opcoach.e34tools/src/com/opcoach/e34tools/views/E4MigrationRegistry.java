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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEExtensionRegistry;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundlePluginModel;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.plugin.PluginElement;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.schema.Schema;

import com.opcoach.e34tools.model.CustomExtensionPoint;
import com.opcoach.e34tools.model.CustomSchema;

/**
 * This class computes all information displayed in E4 Migration Stat view, and
 * provides helper methods to get tooltip texts It could be listener to
 * workspace and plugin object to recompute data
 */
@SuppressWarnings("restriction")
public class E4MigrationRegistry implements IResourceChangeListener
{

	private Map<String, Integer> migrationData = null;

	private Set<CustomExtensionPoint> customExtensionPoint = null;

	private boolean reparseExtensions = true;

	private static E4MigrationRegistry INSTANCE;

	private static boolean computed = false;

	private E4MigrationRegistry()
	{
		migrationData = new HashMap<String, Integer>();
		customExtensionPoint = new HashSet<CustomExtensionPoint>();

		// Listen to workspace to reparse extensions in case of change
		try
		{
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			if (ws != null)
				ws.addResourceChangeListener(this);
		} catch (IllegalStateException ex)
		{
			// There is no workspace to listen.. we don't care (may be in test
			// running)
		}
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

	public int getInstanceNumber(CustomExtensionPoint cep, IPluginModelBase plugin)
	{
		Integer result = migrationData.get(getKey(cep, plugin));
		if (result == null)
		{
			computeDataForPlugin(plugin);
			result = migrationData.get(getKey(cep, plugin));
		}
		return result;
	}

	public int getInstanceNumber(CustomSchema cs, IPluginModelBase plugin)
	{
		Integer result = migrationData.get(getKey(cs, plugin));
		if (result == null)
		{
			computeDataForPlugin(plugin);
			result = migrationData.get(getKey(cs, plugin));
		}
		if (result == null)
			return 0;
		return result;
	}

	private Collection<IExtensionPoint> extensionsToParse = new ArrayList<IExtensionPoint>();

	public Collection<IExtensionPoint> getExtensionsToParse()
	{
		if (!reparseExtensions)
			return extensionsToParse;

		IExtensionRegistry wsExtRegistry = null;
		// Read the workspace extensions. Must introspect to get the workspace
		// extension registry (hidden)
		try
		{
			PDEExtensionRegistry reg = PDECore.getDefault().getExtensionsRegistry();
			Field f = reg.getClass().getDeclaredField("fRegistry");

			f.setAccessible(true);
			// @SuppressWarnings("unchecked")
			wsExtRegistry = (IExtensionRegistry) f.get(reg);
		} catch (Throwable e)
		{
			e.printStackTrace();
		}

		if (wsExtRegistry != null)
		{
			IWorkspace ws = PDECore.getWorkspace();

			for (IProject project : ws.getRoot().getProjects())
			{
				WorkspacePluginModelBase fModel = null;

				// This code comes from NewProjectCreationOPeration...
				IFile fragmentXml = PDEProject.getFragmentXml(project);
				IFile pluginXml = PDEProject.getPluginXml(project);
				IFile manifest = PDEProject.getManifest(project);
				if (fragmentXml.exists())
				{
					fModel = new WorkspaceBundleFragmentModel(manifest, fragmentXml);
				} else
				// if (pluginXml.exists())
				{
					fModel = new WorkspaceBundlePluginModel(manifest, pluginXml);
				}

				if (fModel == null)
					break;
				for (IExtensionPoint ep : wsExtRegistry.getExtensionPoints(project.getName()))
				{
					String epId = ep.getNamespaceIdentifier();
					extensionsToParse.add(ep);
				}
			}
		}

		// Read the extensions in the current eclipse configuration (if luna ->
		// read luna extensions)
		IExtensionRegistry extReg = Platform.getExtensionRegistry();
		for (IExtensionPoint ep : extReg.getExtensionPoints())
		{
			String epId = ep.getNamespaceIdentifier();
			if (epId.equals("org.eclipse.ui"))
			{
				// Check if extension already exists
				boolean mustAdd = true;
				for (IExtensionPoint iep : extensionsToParse)
					if (iep.getUniqueIdentifier().equals(ep.getUniqueIdentifier()))
					{
						mustAdd = false;
						break;
					}
				if (mustAdd)
				{
					extensionsToParse.add(ep);
					System.out.println("Found this extensions to parse : " + ep.getUniqueIdentifier());

				}
			}
		}

		reparseExtensions = false;

		return extensionsToParse;
	}

	public Collection<CustomExtensionPoint> getCustomExtensionToParse()
	{
		return new ArrayList<CustomExtensionPoint>(customExtensionPoint);
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

	private String getKey(CustomExtensionPoint cep, IPluginModelBase plugin)
	{
		return plugin.getBundleDescription().getName() + "/" + getXPath(cep);
	}

	private String getKey(CustomSchema cs, IPluginModelBase plugin)
	{
		return plugin.getBundleDescription().getName() + "/" + getXPath(cs);
	}

	private String getKey(IPluginModelBase plugin, String xpath)
	{
		return plugin.getBundleDescription().getName() + "/" + xpath;
	}

	public String getXPath(IExtensionPoint ep)
	{
		return ep.getUniqueIdentifier();
	}

	public String getXPath(ISchemaElement e)
	{
		Schema o = (Schema) e.getParent();
		return o.getQualifiedPointId() + "/" + e.getName();
	}

	public String getXPath(CustomExtensionPoint cep)
	{
		return cep.getUniqueId();
	}

	public String getXPath(CustomSchema cs)
	{
		return cs.getCustomExtentionPointId() + "/" + cs.getId();
	}

	public static boolean isComputed()
	{
		return computed;
	}

	/**
	 * 
	 * @param xpath
	 *            the xpath to element for an extension point. For instance :
	 *            "views/view" it must not be prefixed by org.eclipse.ui
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
			// System.out.println("Put : " + getKey(ep, plugin) +
			// " -> value =" + nbExtensions);

			// Then compute number of element usage for this extension.
			for (ISchemaElement se : getElementToParse(ep))
			{
				int nbElt = 0;
				for (IPluginExtension ext : plugin.getExtensions(true).getExtensions())
				{
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

		if (customExtensionPoint.size() != 0)
		{
			for (IPluginExtension ext : plugin.getExtensions(true).getExtensions())
			{
				String epId = ext.getPoint();
				for (CustomExtensionPoint cep : customExtensionPoint)
				{
					String id = cep.getUniqueId();
					Integer r = migrationData.get(getKey(cep, plugin));
					int nbElt = 0;
					if (r != null)
					{
						nbElt = r;
					}
					if (accept(id, epId))
					{
						nbElt++;
						for (IPluginObject po : ext.getChildren())
						{
							if (po instanceof PluginElement)
							{
								PluginElement pen = (PluginElement) po;
								CustomSchema cs = cep.getSchema(pen.getName());

								Integer rcs = migrationData.get(getKey(cs, plugin));
								int nbSchElt = 0;
								if (rcs != null)
								{
									nbSchElt = rcs;
								}
								nbSchElt++;
								migrationData.put(getKey(cs, plugin), nbSchElt);
							}
						}
					}
					migrationData.put(getKey(cep, plugin), nbElt);
				}
			}
		}
	}

	private boolean accept(String candidateId, String epId)
	{

		return epId.equals(candidateId);
	}

	public void addCustomExtensionPointIdentifier(String id)
	{
		CustomExtensionPoint cep = new CustomExtensionPoint(id);
		customExtensionPoint.add(cep);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event)
	{
		reparseExtensions = true;
		extensionsToParse.clear();
	}

}
