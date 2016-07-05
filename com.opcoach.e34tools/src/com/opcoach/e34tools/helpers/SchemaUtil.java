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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
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
public class SchemaUtil 
{
	
	public static ISchema getSchema(String uniqueIdentifier)
	{
		ISchema s = PDECore.getDefault().getSchemaRegistry().getSchema(uniqueIdentifier);
		if (s == null)
		{
			Bundle b = FrameworkUtil.getBundle(SchemaUtil.class);
			IStatus st = new Status(IStatus.ERROR, b.getSymbolicName(), "Schema for " + uniqueIdentifier
					+ " can not be found. Check if extension point schema are in the launch configuration");
			Platform.getLog(b).log(st);
			System.out.println(st.getMessage());
		}
		return s;
	}
}
