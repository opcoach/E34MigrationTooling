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
package com.opcoach.e34.tools;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * The activator class controls the plug-in life cycle
 */
public class Migration34Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.opcoach.e34.tools"; //$NON-NLS-1$

    public static final String IMG_DEPRECATED = "icons/deprecated.gif";

    public static final String IMG_FILTER = "icons/filter.gif";

    public static final String IMG_HELP = "icons/help.gif";

    public static final String IMG_EXPAND = "icons/expandall.gif";

    public static final String IMG_COLLAPSE = "icons/collapseall.gif";

    public static final String IMG_EXTENSION = "icons/extensions_obj.gif";

    public static final String IMG_EXPORT = "icons/export.gif";

    // The shared instance
    private static Migration34Activator plugin;

    /**
     * The constructor
     */
    public Migration34Activator() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        displayFeatures();
    }

    private void displayFeatures() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Migration34Activator getDefault() {
        return plugin;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
        Bundle b = FrameworkUtil.getBundle(this.getClass());

        reg.put(IMG_DEPRECATED, ImageDescriptor.createFromURL(b.getEntry(IMG_DEPRECATED)));
        reg.put(IMG_FILTER, ImageDescriptor.createFromURL(b.getEntry(IMG_FILTER)));
        reg.put(IMG_COLLAPSE, ImageDescriptor.createFromURL(b.getEntry(IMG_COLLAPSE)));
        reg.put(IMG_EXPAND, ImageDescriptor.createFromURL(b.getEntry(IMG_EXPAND)));
        reg.put(IMG_EXTENSION, ImageDescriptor.createFromURL(b.getEntry(IMG_EXTENSION)));
        reg.put(IMG_HELP, ImageDescriptor.createFromURL(b.getEntry(IMG_HELP)));
        reg.put(IMG_EXPORT, ImageDescriptor.createFromURL(b.getEntry(IMG_EXPORT)));

    }
}
