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
package com.opcoach.e34tools.helpers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.eclipse.pde.internal.core.util.CoreUtility;
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
public class SchemaUtil {

	public static ISchema getSchema(String uniqueIdentifier) {
		// Try to find the schema using the PDECore access...
		ISchema s = PDECore.getDefault().getSchemaRegistry().getSchema(uniqueIdentifier);
		if (s == null) {

			// Try to find it in the list of local schema
			s = getLocalSchema(uniqueIdentifier);
		}

		if (s == null) {
			// Must warn user that schema can not be found ! 
			Bundle b = FrameworkUtil.getBundle(SchemaUtil.class);
			IStatus st = new Status(IStatus.ERROR, b.getSymbolicName(), "Schema for " + uniqueIdentifier
					+ " can not be found. Check if extension point schema are in the launch configuration");
			Platform.getLog(b).log(st);
		}

		return s;
	}

	/**
	 * This method search for the schema provided by the current plugin... A
	 * copy of all org.eclipse.ui schemas has been provided in the schema folder
	 * of this plugin.
	 * 
	 * @param uniqueIdentifier
	 *            extension point name for instance : org.eclipse.ui.commands
	 * @return the ISchema read locally or null if none
	 */
	private static ISchema getLocalSchema(String uniqueIdentifier) {

		// Has only org.eclipse.ui schema copy in the cache.
		if (!uniqueIdentifier.startsWith("org.eclipse.ui"))
			return null;

		Bundle b = FrameworkUtil.getBundle(SchemaUtil.class);
		String pointName = uniqueIdentifier.replace("org.eclipse.ui.", "");
		String schema = "schema/" + pointName + ".exsd";

		// String location = model.getInstallLocation();
		URL schemaURL = b.getEntry(schema);
		if (schemaURL == null)
			return null;

		ISchemaDescriptor desc = new SchemaDescriptor(uniqueIdentifier, schemaURL);

		return desc.getSchema(true);

	}
}
