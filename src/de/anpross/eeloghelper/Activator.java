package de.anpross.eeloghelper;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.anpross.eeloghelper.enums.LogStyleEnum;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "de.anpross.eeloghelper"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		setDefaults();
	}

	private void setDefaults() {
		getPreferenceStore().setDefault(PreferencePage.PREF_LOG_STYLE, LogStyleEnum.USE_VARIABLE.name());
		getPreferenceStore().setDefault(PreferencePage.PREF_LOG_METHOD_VAR_NAME, PreferencePage.PREF_LOG_METHOD_VAR_DEFAULT);
		getPreferenceStore().setDefault(PreferencePage.PREF_IS_LOGGING_VAR_NAME, PreferencePage.PREF_IS_LOGGING_VAR_DEFAULT);
		getPreferenceStore().setDefault(PreferencePage.PREF_LOG_CLASS_VAR_NAME, PreferencePage.PREF_LOG_CLASS_VAR_DEFAULT);
		getPreferenceStore().setDefault(PreferencePage.PREF_LOGGER_VAR_NAME, PreferencePage.PREF_LOGGER_VAR_DEFAULT);
		getPreferenceStore().setDefault(PreferencePage.PREF_DEFAULT_LEVEL_VAR_NAME, PreferencePage.PREF_DEFAULT_LEVEL_VAR_DEFAULT);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
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
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
