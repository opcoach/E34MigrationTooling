/*******************************************************************************
 * Copyright (c) 2014 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Grégory COCHON (Tech Advantage)
 *******************************************************************************/
package com.opcoach.e34.tools.views;

import java.util.Collection;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import com.opcoach.e34.tools.model.CustomExtensionPoint;
import com.opcoach.e34.tools.model.CustomSchema;

/**
 * Implements a count data provider to get the sum of a line Keep same behavior for color, fonts and
 * navigation
 * 
 * @author gregory
 */
@SuppressWarnings("restriction")
public class CountDataProvider extends PluginDataProvider {
    private Collection<IPluginModelBase> plugins;

    public void setPlugins(Collection<IPluginModelBase> plugins) {
        this.plugins = plugins;
    }

    @Override
    public String getText(Object element) {
        String subKey = "";
        if (element instanceof IExtensionPoint) {
            IExtensionPoint ep = (IExtensionPoint) element;
            subKey = E4MigrationRegistry.getDefault().getXPath(ep);
            return "" + E4MigrationRegistry.getDefault().countNumberOfExtensions(subKey, plugins);
        }
        else if (element instanceof ISchemaElement) {

            ISchemaElement se = (ISchemaElement) element;
            subKey = E4MigrationRegistry.getDefault().getXPath(se);
            return "" + E4MigrationRegistry.getDefault().countNumberOfExtensions(subKey, plugins);

        }
        else if (element instanceof CustomExtensionPoint) {

            CustomExtensionPoint cep = (CustomExtensionPoint) element;
            subKey = E4MigrationRegistry.getDefault().getXPath(cep);
            return "" + E4MigrationRegistry.getDefault().countNumberOfExtensions(subKey, plugins);

        }
        else if (element instanceof CustomSchema) {
            CustomSchema cs = (CustomSchema) element;
            subKey = E4MigrationRegistry.getDefault().getXPath(cs);
            return "" + E4MigrationRegistry.getDefault().countNumberOfExtensions(subKey, plugins);

        }
        return super.getText(element);

    }

    @Override
    public Image getImage(Object element) {
        // No Image for count
        return null;
    }

    @Override
    public Color getForeground(Object element) {
        // Must check if value is > 1 and deprecated
        String txt = getText(element);
        int val = Integer.parseInt(txt);

        return (val > 0 && isDeprecated(element)) ? red : null;
    }

}
