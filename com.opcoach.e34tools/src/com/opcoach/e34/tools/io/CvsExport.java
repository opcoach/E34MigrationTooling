/*******************************************************************************
 * Copyright (c) 2014 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Grégory COCHON (Tech Advantage)
 *     Jérôme FALLEVOZ (Tech Advantage)
 *******************************************************************************/
package com.opcoach.e34.tools.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.opcoach.e34.tools.model.CustomExtensionPoint;
import com.opcoach.e34.tools.model.CustomSchema;
import com.opcoach.e34.tools.views.E4MigrationRegistry;

public class CvsExport {

    public void save(String path, Collection<IExtensionPoint> extPts,
            Collection<CustomExtensionPoint> cExtPts, Collection<IPluginModelBase> plugins)
            throws IOException {
        BufferedWriter buffer = new BufferedWriter(new FileWriter(path));
        // Write header
        StringBuffer buf = new StringBuffer();
        buf.append("Extension Point;Schema");

        for (IPluginModelBase plugin : plugins) {
            buf.append(";").append(plugin.getBundleDescription().getName());
        }
        buffer.write(buf.toString());
        buffer.newLine();

        // For each extension point
        for (IExtensionPoint extPt : extPts) {
            buf = new StringBuffer();
            String uniqueIdentifier = extPt.getUniqueIdentifier();
            buf.append(uniqueIdentifier).append(";");
            // for each plugin
            for (IPluginModelBase plugin : plugins) {
                int nb = E4MigrationRegistry.getDefault().getInstanceNumber(extPt, plugin);
                if (nb == 0) {
                    buf.append(";").append(" ");
                }
                else {
                    buf.append(";").append(nb);
                }
            }
            buffer.write(buf.toString());
            buffer.newLine();
            ISchema schema = getSchema(uniqueIdentifier);

            ISchemaElement extensionElement = null;
            for (ISchemaElement e : schema.getElements()) {
                if ("extension".equals(e.getName())) {
                    extensionElement = e;
                    break;
                }
            }
            ISchemaElement[] ses = schema.getCandidateChildren(extensionElement);
            for (ISchemaElement se : ses) {
                buf = new StringBuffer();
                buf.append(uniqueIdentifier).append(";").append(se.getSchema().getPointId());
                for (IPluginModelBase plugin : plugins) {
                    int nb = E4MigrationRegistry.getDefault().getInstanceNumber(se, plugin);
                    if (nb == 0) {
                        buf.append(";").append(" ");
                    }
                    else {
                        buf.append(";").append(nb);
                    }
                }
                buffer.write(buf.toString());
                buffer.newLine();
            }

        }
        // For each custom extension
        for (CustomExtensionPoint cExtPt : cExtPts) {
            buf = new StringBuffer();
            buf.append(cExtPt.getUniqueId()).append(";");
            // for each plugin
            for (IPluginModelBase plugin : plugins) {
                int nb = E4MigrationRegistry.getDefault().getInstanceNumber(cExtPt, plugin);
                buf.append(";").append(nb);
            }
            buffer.write(buf.toString());
            buffer.newLine();

            for (CustomSchema s : cExtPt.getSchemas()) {
                buf = new StringBuffer();
                buf.append(cExtPt.getUniqueId()).append(";").append(s.getId());
                for (IPluginModelBase plugin : plugins) {
                    int nb = E4MigrationRegistry.getDefault().getInstanceNumber(s, plugin);
                    if (nb == 0) {
                        buf.append(";").append(" ");
                    }
                    else {
                        buf.append(";").append(nb);
                    }
                }
                buffer.write(buf.toString());
                buffer.newLine();
            }

        }

        buffer.close();

    }

    private ISchema getSchema(String uniqueIdentifier) {
        ISchema s = PDECore.getDefault().getSchemaRegistry().getSchema(uniqueIdentifier);
        if (s == null) {
            Bundle b = FrameworkUtil.getBundle(this.getClass());
            IStatus st = new Status(
                    IStatus.ERROR,
                    b.getSymbolicName(),
                    "Schema for "
                            + uniqueIdentifier
                            + " can not be found. Check if extension point schema are in the launch configuration");
            Platform.getLog(b).log(st);
            System.out.println(st.getMessage());
        }
        return s;
    }

}
