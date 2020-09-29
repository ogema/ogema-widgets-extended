/**
 * ﻿Copyright 2014-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.iwes.widgets.reswidget.scheduleviewer.clone;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.ogema.app.timeseries.viewer.expert.ScheduleViewerBasicApp;
import org.ogema.app.timeseries.viewer.expert.gui.MainPage;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.util.extended.eval.widget.MultiSelectByButtons;
import org.ogema.widgets.reswidget.schedulecsvdownload.expert.ScheduleCsvDownloadExpert;
import org.ogema.widgets.reswidget.scheduleviewer.api.expert.ext.ScheduleViewerConfigurationExpert;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.checkbox.Checkbox;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.html.plot.api.Plot2DConfiguration;
import de.iwes.widgets.html.plot.api.PlotType;
import de.iwes.widgets.html.schedulemanipulator.ScheduleManipulator;
import de.iwes.widgets.html.schedulemanipulator.ScheduleManipulatorConfiguration;
import de.iwes.widgets.reswidget.scheduleplot.api.ScheduleData;
import de.iwes.widgets.reswidget.scheduleplot.api.TimeSeriesPlot;
import de.iwes.widgets.reswidget.scheduleplot.container.PlotTypeSelector;
import de.iwes.widgets.reswidget.scheduleplot.flot.SchedulePlotFlot;
import de.iwes.widgets.reswidget.scheduleplot.plotlyjs.SchedulePlotlyjs;
import de.iwes.widgets.reswidget.scheduleviewer.ResourceScheduleViewer;
import de.iwes.widgets.reswidget.scheduleviewer.api.ConditionalTimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.SchedulePresentationData;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewer;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SelectionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration.PreSelectionControllability;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilterExtended;
import de.iwes.widgets.reswidget.scheduleviewer.clone.ScheduleSelector.ChangeScheduleNameDropdown;
import de.iwes.widgets.reswidget.scheduleviewer.clone.helper.TablePojo;
import de.iwes.widgets.reswidget.scheduleviewer.pattern.MultiPatternScheduleViewer;
import de.iwes.widgets.reswidget.scheduleviewer.pattern.PatternScheduleViewer;
import de.iwes.widgets.reswidget.scheduleviewer.utils.DefaultScheduleViewerConfigurationProvider;
import de.iwes.widgets.reswidget.scheduleviewer.utils.DefaultSessionConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.utils.ScheduleViewerUtil;
import de.iwes.widgets.template.DisplayTemplate;

/**
 * A schedule viewer, consisting of a {@link TemplateMultiselect} widget that
 * lets the user choose from a set of schedules/time series, {@link Datepicker}s
 * for the start and end time, and a {@link SchedulePlotFlot SchedulePlot}
 * widget, that displays the selected time series.<br>
 * Note that there are specific versions of this widget available, which can
 * show all {@link Schedule}s of a specific type, or time series corresponding
 * to ResourcePattern matches, and determine the list of schedules to be
 * displayed autonomously. The present version of the ScheduleViewer, on the
 * other hand, requires the time series to be set explicitly, via the methods
 * {@link #setDefaultSchedules(Collection)}.<br>
 * 
 * Note: this is always a global widget, in particular it cannot be added as a
 * subwidget to a non-global widget. Several of the subwidgets are non-global,
 * though, such as the time series selector.
 * 
 * @param <T>
 *            the type of time series to be displayed. Schedules, RecordedData
 *            (resource log data), and {@link SchedulePresentationData} are
 *            supported, but MemoryTimeSeries are not (directly). The
 *            SchedulePresentationData can wrap any kind of ReadOnlyTimeSeries,
 *            so in order to display a memory time series, wrap it into a
 *            DefaultSchedulePresentationData object.
 * 
 * @see ResourceScheduleViewer
 * @see PatternScheduleViewer
 * @see MultiPatternScheduleViewer
 * 
 * @author cnoelle, skarge
 */
@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class ScheduleViewerExtended extends PageSnippet implements ScheduleViewer<ReadOnlyTimeSeries> {

	private static final long serialVersionUID = 8360241089115449033L;
	public static final long STANDARD_BUFFER_WINDOW = 24 * 60 * 60 * 1000L;

	/**
	 * Configuration options
	 */
	//public final ScheduleViewerConfiguration configuration;
	protected final ScheduleViewerBasicApp app;
	protected final String scheduleViewerWidgetId;
	protected ScheduleViewerConfigurationProvider configProvider(OgemaHttpRequest req) {
		String providerId = MainPage.getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_PROVIDER_ID);
		ScheduleViewerConfigurationProvider provider = app.getScheduleProviders().get(providerId);
		if(provider == null)
			return DefaultScheduleViewerConfigurationProvider.DEFAULT_SCHEDULEVIEWER_CONFIGURATION_PROVIDER;
		return provider;
	}
	protected SessionConfiguration sessionConfig(OgemaHttpRequest req) {
		String configId =  MainPage.getPageParameter(req, page, ScheduleViewerBasicApp.PARAM_SELECTION_CONFIG_ID);
		ScheduleViewerConfigurationProvider provider = configProvider(req);
		if(provider == null) {
			return null;
		}
		SessionConfiguration sessionConfig = provider.getSessionConfiguration(configId);
		if (sessionConfig == null) {
			sessionConfig = DefaultSessionConfiguration.DEFAULT_SESSION_CONFIGURATION;
		}
		return sessionConfig;
	}
	protected ScheduleViewerConfiguration configuration(OgemaHttpRequest req) {
		SessionConfiguration sessionConfig = sessionConfig(req);
		ScheduleViewerConfiguration config = sessionConfig.viewerConfiguration();
		if (config == null) {
			config = ScheduleViewerConfiguration.DEFAULT_CONFIGURATION;
		}
		return config;
	}

	protected final ApplicationManager am;
	private final Alert alert;
	protected final List<Label> programSelectorLabels;
	protected List<Label> filterSelectorLabels;
	protected final Label scheduleSelectorLabel;
	protected final Label scheduleStartLabel;
	protected final Label dropdownScheduleNameLabel;
	protected final Label updateLabel;
	protected final Label scheduleEndLabel;
	protected Label nrDataPointsLabel;
	protected Label triggerIndividualConfigLabel;
	protected final List<TemplateMultiselect<TimeSeriesFilterExtended>> programSelectors;
	protected List<ConditionalProgramSelector> filterSelectors;
	//protected final ScheduleSelector scheduleSelector;
	public ScheduleSelector scheduleSelector(OgemaHttpRequest req) {
		ScheduleSelector scheduleSelector = selectorSnippet.getData(req).scheduleSelector;
		if(scheduleSelector == null) {
			scheduleSelector = selectorSnippet.getData(req).scheduleSelector = new ScheduleSelector(selectorSnippet, scheduleViewerWidgetId + "_scheduleSelector",
					sessionConfig(req), req, this);
			scheduleSelector.triggerAction(scheduleStartPicker, POST_REQUEST, GET_REQUEST);
			scheduleSelector.triggerAction(scheduleEndPicker, POST_REQUEST, GET_REQUEST);
			scheduleSelector.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST, 1);
			scheduleSelector.selectAllOrDeselectAllButton.triggerAction(scheduleStartPicker, POST_REQUEST, GET_REQUEST);
			scheduleSelector.selectAllOrDeselectAllButton.triggerAction(scheduleEndPicker, POST_REQUEST, GET_REQUEST);
			scheduleSelector.selectAllOrDeselectAllButton.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST, 1);
			if (programSelectors != null) {
				for (OgemaWidget programSelector : programSelectors) {
					programSelector.triggerAction(scheduleSelector, POST_REQUEST, GET_REQUEST);
				}
			}
			if (filterSelectors != null) {
				for (ConditionalProgramSelector filterSelector : filterSelectors) {
					filterSelector.instanceSelector.triggerAction(scheduleSelector, POST_REQUEST, GET_REQUEST);
					filterSelector.filterSelector.triggerAction(scheduleSelector, POST_REQUEST, GET_REQUEST);
				}
			}
			return scheduleSelector;
		}
		return scheduleSelector;
	}
	protected final ViewerDatepicker scheduleStartPicker;
	protected final ViewerDatepicker scheduleEndPicker;
	protected Label nrDataPoints;
	protected Button updateButton;
	//protected SchedulePlotFlot schedulePlot; // TODO generic interface
	//protected SchedulePlotlyjs schedulePlot; // TODO generic interface
	protected TimeSeriesPlot<?,?,?> schedulePlot;
	protected ScheduleManipulator manipulator;
	protected Header manipulatorHeader;
	protected ScheduleCsvDownloadExpert<ReadOnlyTimeSeries> csvDownload;
	protected Header downloadHeader;
	protected Label optionsLabel;
	protected Checkbox optionsCheckbox;
	protected MultiSelectByButtons multiSelectOptions;
	protected Button triggerIndividualConfigPopupButton;
	protected ConfigPopup individualConfigPopup;
	
	protected TemplateDropdown<PlotType> lineTypeSelector;
	protected Label lineTypeLabel;

	protected final Button saveConfigurationButton;
	protected final ChangeScheduleNameDropdown dropdownScheduleNames;
	public static final String PARAM_SESSION_CONFIG_ID = "configId";
	public static final String PARAM_EXPERT_MODE = "expertMode";
	public static final String PARAM_PROVIDER_ID = "providerId";
	public final static String FIX_INTERVAL_OPT = System.getProperty(
			"org.ogema.app.timeseries.viewer.expert.gui.fixintervalonschedswitchlabel", "Fix interval on schedule switch");
	private final static String SHOW_EMPTY_OPT = System.getProperty(
			"org.ogema.app.timeseries.viewer.expert.gui.showemptyschedslabel", "Show empty schedules");
	public static final String SHORT_NAME = "Shortname";
	public static final String LONG_NAME = "Longname (Devicetype/name, Sensor/Actortype, Room)";
	public static final String LOCATION = "Locaton (Path)";
	private final WidgetPage<?> page;
	private static final TriggeredAction GET_REQUEST = TriggeredAction.GET_REQUEST;
	private static final TriggeringAction GET_REQUEST2 = TriggeringAction.GET_REQUEST;
	private static final TriggeringAction POST_REQUEST = TriggeringAction.POST_REQUEST;
	//protected final ScheduleViewerConfigurationProvider configProvider;
	protected Label intervalDropLabel;
	protected TemplateDropdown<Long> intervalDrop;
	//private Button selectAllSchedulesButton;
	public PageSnippetSelector selectorSnippet;

	/**
	 * Create a schedule viewer with default configuration. This means, in
	 * particular, that no schedule manipulator is shown, and the widget name
	 * service is used to determine the schedule labels.
	 * 
	 * @param page
	 * @param id
	 * @param am
	 */
	public ScheduleViewerExtended(WidgetPage<?> page, String id, final ScheduleViewerBasicApp app) {
		this(page, id, app, null);
	}

	/*public ScheduleViewerExtended(WidgetPage<?> page, String id, final ApplicationManager am,
			DisplayTemplate<ReadOnlyTimeSeries> displayTemplate) {
		this(page, id, am, displayTemplate, null, null);
	}*/

	@Override
	public void onGET(OgemaHttpRequest req) {
		initProgramSelector(scheduleViewerWidgetId, configuration(req).programs, sessionConfig(req));
		scheduleStartPicker.getData(req).init = true;
		scheduleEndPicker.getData(req).init = true;
	}

	/**
	 * Create a schedule viewer with custom configuration.
	 * 
	 * @param page
	 * @param id
	 * @param am
	 * @param sessionConfig
	 *            Configuration object for initial configuration. May be null, in which case a default
	 *            configuration is used. If a provider and a configurationId for the provider is specified
	 *            for the page that uses the schedule viewer this should be the configuration returned by
	 *            the provider for the configurationId.
	 * @param displayTemplate
	 *            Display template for the time series. May be null, in which case a
	 *            DefaultTimeSeriesDisplayTemplate is used.
	 * @param configProvider May be null, in which case a
	 *            DefaultScheduleViewerConfigurationProvider is used.
	 */
	public ScheduleViewerExtended(WidgetPage<?> page, String id, final ScheduleViewerBasicApp app,
			DisplayTemplate<ReadOnlyTimeSeries> displayTemplate) {
		super(page, id, true);

		this.page = page;
		this.app = app;
		this.am = app.appManager;
		this.scheduleViewerWidgetId = id;

		this.alert = new Alert(page, id + "_alert", "");
		alert.setDefaultVisibility(false);
		this.append(alert, null);

		final boolean showProgramSelector = true; //(config.programs != null);
		final boolean showFilterSelector = true;  //(config.filters != null);

		this.scheduleSelectorLabel = new Label(page, id + "_scheduleSelectorLabel",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.selectschedulelabel", "Select schedule"));
		this.dropdownScheduleNameLabel = new Label(page, id + "_scheduleNameLabel", "Change schedule-naming");
		this.updateLabel = new Label(page, id + "_updateLabel", "");
		this.scheduleStartLabel = new Label(page, id + "_scheduleStartLabel",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.selectstarttimelabel", "Select start time"));
		this.scheduleEndLabel = new Label(page, id + "_scheduleEndLabel",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.selectendtimelabel", "Select end time"));

		this.programSelectorLabels = new ArrayList<>();
		this.programSelectors = new ArrayList<>();
		//if (showProgramSelector) {
		//	initProgramSelector(id); //config.programs, sessionConfig);
		//}
		initRow0SelectFilter(page, id, am, showFilterSelector); //config, sessionConfig, showFilterSelector);

		//this.scheduleSelector = new ScheduleSelector(page, id + "_scheduleSelector", sessionConfig, this);

		initRow1Options(page, id);
		this.scheduleStartPicker = new ViewerDatepicker(page, id + "_scheduleStartPicker", true, this);
		this.scheduleEndPicker = new ViewerDatepicker(page, id + "_scheduleEndPicker", false, this);

		initRow2NoOfDataPoints(page, id, am);
		initRow3ConfigButton(page, id);
		final boolean showCheckboxes = true; //configuration.showOptionsSwitch;
		initSchedulePlot(page, id, am, showCheckboxes);
		initRow4Downloaddata(page, id, am);
		initRow5ManipulateSchedule(page);
		initRow678SelectIntervall(page, id);
		initRow9SelectLineType(page, id);

		saveConfigurationButton = getSaveConfigurationButton(id);
		//selectAllSchedulesButton = new SelectAllButton(page, id + "_selectScheduleButton");
		dropdownScheduleNames = new ChangeScheduleNameDropdown(page, id + "_scheduleNameChanger");
		//selectAllSchedulesButton = scheduleSelector.selectAllOrDeselectAllButton;
		//dropdownScheduleNames = scheduleSelector.scheduleNameDropDown;
		finishBuildingPage(id, showProgramSelector, showFilterSelector);
		setDependencies();
	}

	/**
	 * Initializes the first row of the dynamicTable with filter selectors
	 * 
	 * @param page
	 * @param id
	 * @param am
	 * @param config
	 * @param sessionConfig
	 * @param showFilterSelector
	 */
	private void initRow0SelectFilter(WidgetPage<?> page, String id, final ApplicationManager am,
			//ScheduleViewerConfiguration config, SessionConfiguration sessionConfig,
			final boolean showFilterSelector) {
		if (!showFilterSelector) {
			this.filterSelectorLabels = Collections.emptyList();
			this.filterSelectors = Collections.emptyList();
		} else {
			this.filterSelectorLabels = new ArrayList<>();
			this.filterSelectors = new ArrayList<>();
			Label filterSelectorLabel;
			ConditionalProgramSelector filterSelector;
			int cnt = 0;
			/*for (Map<String, ConditionalTimeSeriesFilter<?>> filters : config.filters) {
				filterSelectorLabel = new Label(page, id + "_filterSelectorLabel_" + cnt, "Select a filter");
				filterSelector = new ConditionalProgramSelector(page, id + "_filterSelector" + cnt++, filters,
						am.getResourcePatternAccess(), sessionConfig) {

					private static final long serialVersionUID = 1L;

					@Override
					public void updateDependentWidgets(OgemaHttpRequest req) {
						scheduleSelector.updateByProgramOrFilterChange(req);
					}
				};

				filterSelectorLabels.add(filterSelectorLabel);
				filterSelectors.add(filterSelector);
			}*/
		}
	}

	/**
	 * Initializes the row of the dynamicTable with a DownloadButton
	 * 
	 * @param page
	 * @param id
	 * @param am
	 */
	private void initRow4Downloaddata(WidgetPage<?> page, String id, final ApplicationManager am) {
		downloadHeader = new Header(page, "downloadHeader", "Download data") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showCsvDownload || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
		};
		downloadHeader.setDefaultHeaderType(3);
		//downloadHeader.addStyle(HtmlStyle.ALIGNED_CENTER);
		downloadHeader.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		this.csvDownload = new ScheduleCsvDownloadExpert<ReadOnlyTimeSeries>(page, id + "_dataDownload", am.getWebAccessManager()) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showCsvDownload || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
					return;
				}
				List<Collection<TimeSeriesFilter>> programs = ScheduleSelector.parseFilters(configuration(req).programs);// sessionconfig.programsPreselected();
				List<Collection<TimeSeriesFilterExtended>> filterCollection = ScheduleViewerUtil.getInstance()
						.parse(programs, am.getResourceAccess());
				List<TimeSeriesFilterExtended> filters = new ArrayList<>();
				for(Collection<TimeSeriesFilterExtended> item : filterCollection) {
					filters.addAll(item);
				}	
				
				this.setSchedules(scheduleSelector(req).getSelectedItems(req), filters, req);
			}
		};
	}

	/**
	 * Initializes the row of the dynamicTable with a ScheduleManipulator
	 * 
	 * @param page
	 */
	private void initRow5ManipulateSchedule(WidgetPage<?> page) {
		//if (!configuration.showManipulator) {
		//	return;
		//}

		this.manipulatorHeader =  new Header(page, "manipulatorHeader",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.manipulatorheading", "Manipulate schedule")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showManipulator || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
		};
		manipulatorHeader.setDefaultHeaderType(3);
		manipulatorHeader.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		//new StaticHeader(3, System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.manipulatorheading", "Manipulate schedule"));
		//this.manipulatorHeader.addStyle(HtmlStyle.ALIGNED_CENTER);
		//ScheduleManipulatorConfiguration smc = configuration.manipulatorConfiguration;
		//ScheduleManipulatorConfiguration newConfig = new ScheduleManipulatorConfiguration(alert,
		//		smc.isShowInterpolationMode(), smc.isShowQuality());
		ScheduleManipulatorConfiguration newConfig = new ScheduleManipulatorConfiguration(alert,
				true, true);
		this.manipulator = new ScheduleManipulator(page, "manipulator", newConfig, am) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showManipulator || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
					return;
				}
				List<ReadOnlyTimeSeries> selectedSchedules = scheduleSelector(req).getSelectedItems(req);
				if (selectedSchedules.size() != 1 || !(selectedSchedules.get(0) instanceof Schedule)) {
					// we display the schedule manipulator only if exactly one schedule is selected
					setSchedule(null, req);
					return;
				}
				setSchedule((Schedule) selectedSchedules.get(0), req);
				long startTime = scheduleStartPicker.getDateLong(req);
				setStartTime(startTime, req);
			}
		};
	}

	/**
	 * Initializes the row of the dynamicTable with a Intervall and a Start-
	 * End-Timepicker
	 * 
	 * @param page
	 * @param id
	 */
	private void initRow678SelectIntervall(WidgetPage<?> page, String id) {
		this.intervalDropLabel = new Label(page, id + "_intervalDropLabel", "Select interval") {

			private static final long serialVersionUID = -793624698242225307L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showStandardIntervals || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
				;
			}
		};
		final List<Long> defaultValues = Arrays.asList(0L, 10 * 60 * 1000L, 60 * 60 * 1000L, 24 * 60 * 60 * 1000L,
				2 * 24 * 60 * 60 * 1000L, 7 * 24 * 60 * 60 * 1000L, 30 * 24 * 60 * 60 * 1000L,
				365 * 24 * 60 * 60 * 1000L);
		this.intervalDrop = new TemplateDropdown<Long>(page, id + "_intervalDrop") {

			private static final long serialVersionUID = 5595511208289921378L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showStandardIntervals || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				scheduleStartPicker.getData(req).fixedInterval = true;
				scheduleEndPicker.getData(req).fixedInterval = true;
			}
		};

		intervalDrop.setTemplate(ScheduleViewerUtil.intervalDisplayTemplate);
		intervalDrop.setComparator(ScheduleViewerUtil.defaultLongComparator);
		intervalDrop.setDefaultItems(defaultValues);
	}

	/**
	 * Initializes the row of the dynamicTable with a options Checkbox and
	 * FixSchedule
	 * 
	 * @param page
	 * @param id
	 */
	private void initRow1Options(WidgetPage<?> page, String id) {
		this.optionsLabel = new Label(page, id + "_fixIntervalLabel",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.optionslabel", "Options")) {

			private static final long serialVersionUID = -1032652251225301073L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showOptionsSwitch || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
		};
		if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.usemultiselectbybuttons")) {
			this.optionsCheckbox = null;
			//init of MultiSelectbyButtons is done below. This will always be visible
		} else {
			this.optionsCheckbox = new Checkbox(page, id + "_optionsCheckbox") {
	
				private static final long serialVersionUID = 685168574654L;
	
				@Override
				public void onGET(OgemaHttpRequest req) {
					if (configuration(req).showOptionsSwitch || isExpertMode(req)) {
						setWidgetVisibility(true, req);
					} else {
						setWidgetVisibility(false, req);
					}
				}
			};
		}
		Map<String, Boolean> opts = new LinkedHashMap<String, Boolean>();
		opts.put(FIX_INTERVAL_OPT, true); //TODO: set this depending on session
		opts.put(SHOW_EMPTY_OPT, true);
		if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.usemultiselectbybuttons")) {
			List<String> items = new ArrayList<>(opts.keySet());
			multiSelectOptions = new MultiSelectByButtons(items, "msroom", page,
					ButtonData.BOOTSTRAP_GREEN, ButtonData.BOOTSTRAP_LIGHTGREY);
			multiSelectOptions.setDefaultSelectedItems(items);
		} else
			optionsCheckbox.setDefaultList(opts);
	}

	private void initRow9SelectLineType(WidgetPage<?> page, String id) {
		this.lineTypeLabel = new Label(page, id + "_lineTypeLabel",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.selectlinetypelabel", "Select the line type")) {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).isShowPlotTypeSelector() || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}			
		};
		this.lineTypeSelector = new PlotTypeSelector(page, id + "_lineTypeSelector", schedulePlot) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).isShowPlotTypeSelector() || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
		};
		lineTypeSelector.selectDefaultItem(PlotType.LINE);
	}
	
	/**
	 * Initializes the SchedulePlot and the Apply-Button
	 */
	private void initSchedulePlot(WidgetPage<?> page, String id, final ApplicationManager am,
//			ScheduleViewerConfiguration config,
			final boolean showCheckboxes) {
		if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.useplotlyjs")) {
			schedulePlot = new SchedulePlotlyjs(page, id + "_schedulePlot", false, STANDARD_BUFFER_WINDOW) { //config.bufferWindow) {
	
				private static final long serialVersionUID = -8867287385992011041L;
	
				@Override
				public void onGET(OgemaHttpRequest req) {
					if (getSessionConfiguration(req) != null && getSessionConfiguration(req).generateGraphImmediately()) {
						updatePlot(this, am, req, showCheckboxes);
					}
				}
			};
		} else {
			schedulePlot = new SchedulePlotFlot(page, id + "_schedulePlot", false, STANDARD_BUFFER_WINDOW) { //config.bufferWindow) {
				
				private static final long serialVersionUID = -8867287385992011041L;
	
				@Override
				public void onGET(OgemaHttpRequest req) {
					if (getSessionConfiguration(req) != null && getSessionConfiguration(req).generateGraphImmediately()) {
						updatePlot(this, am, req, showCheckboxes);
					}
				}
			};			
		}
		schedulePlot.getDefaultConfiguration().doScale(true); // can be overwritten in app
		schedulePlot.setDefaultHeight("700px");
		
		updateButton = new Button(page, id + "_updateButton",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.applylabel", "Apply")) {

			private static final long serialVersionUID = -2768928390239131942L;

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				updatePlot(schedulePlot, am, req, showCheckboxes);
			}
		};
	}

	/**
	 * Updates the Plot (start, endtime, Timeseries)
	 * @param plot
	 * @param am
	 * @param req
	 * @param showCheckboxes
	 */
	private void updatePlot(TimeSeriesPlot<?, ?, ?> plot, final ApplicationManager am, OgemaHttpRequest req,
			boolean showCheckboxes) {
		final SessionConfiguration cfg = getSessionConfiguration(req);
		// TODO set line type
		ScheduleViewerConfiguration viewerConfig = sessionConfig(req).viewerConfiguration();
		if(viewerConfig.isShowPlotTypeSelector()) {
			PlotType plotType = this.lineTypeSelector.getSelectedItem(req);
			getPlotConfiguration(req).setPlotType(plotType);
		}
		else if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.plotlines"))
			getPlotConfiguration(req).setPlotType(PlotType.LINE);
		//else: default is dots
		if(viewerConfig instanceof ScheduleViewerConfigurationExpert) {
			ScheduleViewerConfigurationExpert exp = (ScheduleViewerConfigurationExpert) viewerConfig;
			if(!exp.doScale)
				getPlotConfiguration(req).doScale(false);
		}
		
		final List<ReadOnlyTimeSeries> selectedSchedules = scheduleSelector(req).getSelectedItems(req);
		long startTime = scheduleStartPicker.getDateLong(req);
		long endTime = scheduleEndPicker.getDateLong(req);

		boolean showEmpty;
		if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.usemultiselectbybuttons")) {
			showEmpty = showCheckboxes ? multiSelectOptions.getSelectedItems(req).contains(SHOW_EMPTY_OPT) : true;
		} else
			showEmpty = showCheckboxes ? optionsCheckbox.getCheckboxList(req).get(SHOW_EMPTY_OPT) : true;
		if (startTime > endTime)
			startTime = endTime;
		plot.setInterval(startTime, endTime, req);
		Map<String, SchedulePresentationData> schedules = new LinkedHashMap<String, SchedulePresentationData>();
		for (ReadOnlyTimeSeries sched : selectedSchedules) {
			if (!showEmpty && sched.isEmpty(startTime, endTime))
				continue;
			Resource parent = null;
			Class<?> type = null;
			List<Collection<TimeSeriesFilter>> programs = ScheduleSelector.parseFilters(configuration(req).programs);// sessionconfig.programsPreselected();
			List<Collection<TimeSeriesFilterExtended>> filterCollection = ScheduleViewerUtil.getInstance()
					.parse(programs, am.getResourceAccess());
			List<TimeSeriesFilterExtended> filters = new ArrayList<>();
			for(Collection<TimeSeriesFilterExtended> item : filterCollection) {
				filters.addAll(item);
			}	
			for(TimeSeriesFilterExtended filter : filters) {
				if(filter.accept(sched)) {
					type = filter.type(sched);
				}
			}
			if(type == null) {
				if (sched instanceof SchedulePresentationData) {
					type = ((SchedulePresentationData) sched).getScheduleType();
				} else if (sched instanceof Schedule) {
					parent = ((Schedule) sched).getParent();
				} else if (sched instanceof RecordedData) {
					String path = ((RecordedData) sched).getPath();
					parent = am.getResourceAccess().getResource(path);
				} //else
				//	continue;
			}
			if ((type == null) && (parent != null)) {
				if ((parent instanceof SingleValueResource) && (!(parent instanceof StringResource)))
					type = parent.getResourceType();
			}
			String label = getLabelForPlot(req, sched);
			schedules.put(label, new DefaultSchedulePresentationData(sched, type, label));
		}
		ScheduleData<?> data = plot.getScheduleData(req);
		data.setSchedules(schedules);
	}

	/**
	 * Returns the Label for the Plot (Longname, Shortname oder Location)
	 * @param req
	 * @param sched
	 * @return
	 */
	private String getLabelForPlot(OgemaHttpRequest req, ReadOnlyTimeSeries sched) {
		//TODO: This should be adaptable
		return scheduleSelector(req).templateShort.getLabel(sched, req.getLocale());
		/*String naming = scheduleSelector.scheduleNameDropDown.getSelectedValue(req);
		if (SHORT_NAME.equals(naming)) {
			return scheduleSelector.templateShort.getLabel(sched, req.getLocale());
		} else if (LONG_NAME.equals(naming)) {
			return scheduleSelector.templateLong.getLabel(sched, req.getLocale());
		}

		return scheduleSelector.templateLocation.getLabel(sched, req.getLocale());
*/
	}


	private void initRow2NoOfDataPoints(WidgetPage<?> page, String id, final ApplicationManager am) {
		this.nrDataPointsLabel = new Label(page, id + "_nrDataPointsLabel",
				System.getProperty("org.ogema.app.timeseries.viewer.expert.gui.nrdatapointslabel", "Number of data points")) {

			private static final long serialVersionUID = -793624698242225307L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showNrPointsPreview || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
		};
		this.nrDataPoints = new Label(page, id + "_nrDataPoints", "0") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showNrPointsPreview || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
					return;
				}
				List<ReadOnlyTimeSeries> schedules = scheduleSelector(req).getSelectedItems(req);
				long t0 = System.currentTimeMillis();
				if (am != null) {
					t0 = am.getFrameworkTime();
				}
				long startTime = t0;
				long endTime = t0;
				try {
					startTime = scheduleStartPicker.getDateLong(req);
					endTime = scheduleEndPicker.getDateLong(req);
				} catch (Exception e) {
				}
				if(schedules.isEmpty()) {
					final SessionConfiguration sessionConfig = getSessionConfiguration(req);
					//from scheduleSelector(req).overwriteScheduleSelectorWithProvider(req, sessionConfig);
					List<ReadOnlyTimeSeries> selectedTimeSeries = sessionConfig.timeSeriesSelected();
					if (selectedTimeSeries.size() > 10) {
						schedules = selectedTimeSeries.subList(0, 9);
					} else {
						schedules = selectedTimeSeries;
					}
					//schedules = scheduleSelector(req).getSelectedItems(req);
				}
				if (schedules == null || schedules.isEmpty() || startTime > endTime) {
					setText("0", req);
					return;
				}
				int size = 0;
				for (ReadOnlyTimeSeries sched : schedules) {
					//FIXME: just for testing
					int sizeLoc = sched.size(startTime, endTime);
					List<SampledValue> dps = sched.getValues(startTime, endTime);
					Iterator<SampledValue> it = sched.iterator(startTime, endTime);
					if((it.hasNext()) && (sizeLoc == 0)) {
						am.getLogger().error("This should never occur and may cause performance issues: Data in ReadOnlyTimeries obtainable via iterator, but size zero!");
						int count = 0;
						while(it.hasNext()) {
							count++;
							it.next();
						}
						//System.out.println("size:"+sizeLoc+", but iterator has "+count+" elements");
						size += count;
					}
					else size += dps.size();
					//size += sched.size(startTime, endTime);
				}
		        NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);		   

				setText(nf.format(size), req);
			}
		};
	}

	/**
	 * Initializes the row of the dynamicTable with a ConfigPopup-Button
	 * @param page
	 * @param id
	 */
	private void initRow3ConfigButton(WidgetPage<?> page, String id) {
		this.triggerIndividualConfigLabel = new Label(page, id + "_triggerIndvConfigLabel",
				"Configure display settings") {

			private static final long serialVersionUID = 4680903101539028489L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showIndividualConfigBtn || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
		};
		this.triggerIndividualConfigPopupButton = new Button(page, id + "_triggerIndividualConfigPopup",
				"Open settings") {

			private static final long serialVersionUID = 175423937072595444L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (configuration(req).showIndividualConfigBtn || isExpertMode(req)) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
		};
		this.individualConfigPopup = new ConfigPopup(page, id + "_individualConfigPopup", this);
	}

	/**
	 * Initializes the rows of the dynamicTable with the 0...n programSelectors
	 * @param id
	 * @param allPrograms
	 * @param sessionConfig
	 */
	private void initProgramSelector(String id,
			List<Map<String, TimeSeriesFilter>> allPrograms, SelectionConfiguration sessionConfig) {

		Label programSelectorLabel;
		TemplateMultiselect<TimeSeriesFilterExtended> programSelector;

		int cnt = 0;

		for (Map<String, TimeSeriesFilter> filters : allPrograms) {

			programSelectorLabel = new Label(page, id + "_programSelectorLabel_" + cnt, "Select a program");
			programSelector = new TemplateMultiselect<TimeSeriesFilterExtended>(page, id + "_programSelector" + cnt++) {

				private static final long serialVersionUID = -146823525912151858L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					SessionConfiguration sessionConfig = getSessionConfiguration(req);
					updateProgramsPreselected(req, sessionConfig);
					if (sessionConfig.timeSeriesSelectionControllability() == PreSelectionControllability.FIXED
							|| sessionConfig
									.timeSeriesSelectionControllability() == PreSelectionControllability.MAX_SIZE) {
						// user selection not Possible
						disable(req);
					} else {
						// user selection Possible
						enable(req);
					}
				}

				@Override
				public void updateDependentWidgets(OgemaHttpRequest req) {
					scheduleSelector(req).updateByProgramOrFilterChange(req);
				}

				private void updateProgramsPreselected(OgemaHttpRequest req, SessionConfiguration sessionConfig) {

					Collection<TimeSeriesFilterExtended> preselectedFilters = new ArrayList<>();
					List<Collection<TimeSeriesFilterExtended>> programsPreselected = ScheduleViewerUtil.getInstance()
							.parse(sessionConfig.programsPreselected(), am.getResourceAccess());
					for (Collection<TimeSeriesFilterExtended> outer : programsPreselected) {
						for (TimeSeriesFilterExtended inner : outer) {
							preselectedFilters.add(inner);
						}
					}

					Collection<TimeSeriesFilterExtended> allFilters = getItems(req);

					if (isExpertMode(req)) {
						// Im Expert-Mode werden auch alle Standard-Filter angeboten
						Collection<TimeSeriesFilterExtended> filterToshow = getSubsetwithPreselectedFilters(allFilters,
								preselectedFilters);
						selectItems(filterToshow, req);
					} else {
						// Im Non-Expert-Mode werden nur die gew�hlten Timeseries im Schedule-Selector
						// angeboten, keine weiteren TimeSeries angeboten.
						selectItems(preselectedFilters, req);
					}
				}
			};

			List<TimeSeriesFilterExtended> extendedFilters = ScheduleViewerUtil.getInstance().parse(filters.values(),
					am.getResourceAccess());
			List<Collection<TimeSeriesFilterExtended>> preSelectedExtendedFilters = ScheduleViewerUtil.getInstance()
					.parse(sessionConfig.programsPreselected(), am.getResourceAccess());
			programSelector.selectDefaultItems(extendedFilters);

			for (Collection<TimeSeriesFilterExtended> programmPreselected : preSelectedExtendedFilters) {
				Collection<TimeSeriesFilterExtended> subSet = getSubsetwithPreselectedFilters(extendedFilters,
						programmPreselected);
				if (!subSet.isEmpty()) {
					programSelector.setDefaultSelectedItems(programmPreselected);
				}
			}

			programSelector.setTemplate(new DisplayTemplate<TimeSeriesFilterExtended>() {

				@Override
				public String getId(TimeSeriesFilterExtended object) {
					return object.id();
				}

				@Override
				public String getLabel(TimeSeriesFilterExtended object, OgemaLocale locale) {
					return object.label(locale);
				}
			});

			programSelector.setDefaultWidth("100%");
			programSelectorLabels.add(programSelectorLabel);
			programSelectors.add(programSelector);
		}
	}

	/**
	 * creates a dynamic table for the expert view, in which all filter options are available, 
	 * and a user-friendly view in which only the important or necessary filter options are available.
	 * @param id
	 * @param showProgramSelector
	 * @param showFilterSelector
	 */
	protected void appendDynamicTable(final String id, boolean showProgramSelector, boolean showFilterSelector) {

		final List<TablePojo> EXPERT_LIST = getTableElements(id, showProgramSelector, showFilterSelector, true);
		final List<TablePojo> USERFRIENDLY_LIST = getTableElements(id, showProgramSelector, showFilterSelector, false);

		DynamicTable<TablePojo> table = new DynamicTable<TablePojo>(page, id + "_table") {

			private static final long serialVersionUID = 5879356788178926843L;

			@Override
			public void onGET(OgemaHttpRequest req) {

				if (isExpertMode(req)) {
					updateRows(EXPERT_LIST, req);
				} else {
					updateRows(USERFRIENDLY_LIST, req);
				}
			}
		};
		table.setRowTemplate(new RowTemplate<TablePojo>() {

			@Override
			public Row addRow(TablePojo pojo, OgemaHttpRequest req) {
				final Row row = new Row();
				if (pojo.getLabel() != null) {
					String label = pojo.getLabel().getText(req);
					row.addCell("label", label);
				}

				row.addCell("widget", pojo.getSnippet());
				row.addCell("empty", "");
				return row;
			}

			@Override
			public String getLineId(TablePojo pojo) {
				return pojo.getId();
			}

			@Override
			public Map<String, Object> getHeader() {
				return null;
			}

		});
		this.append(table, null);

	}

	/**
	 * Returns a list of FilterOptions widgets that are displayed as rows in the dynamicTable
	 * @param mainId
	 * @param showProgramSelector
	 * @param showFilterSelector
	 * @param isExpert
	 * @return
	 */
	private List<TablePojo> getTableElements(final String mainId, boolean showProgramSelector,
			boolean showFilterSelector, final boolean isExpert) {
		final ArrayList<TablePojo> list = new ArrayList<>();
		String id = mainId + "_expert_" + isExpert;

		if ((showProgramSelector || isExpert) && programSelectors != null) {
			for (int i = 0; i < programSelectors.size(); i++) {
				list.add(new TablePojo(id, programSelectorLabels.get(i), programSelectors.get(i), page));
			}
		}
		if ((showFilterSelector || isExpert) && filterSelectors != null) {
			for (int i = 0; i < filterSelectors.size(); i++) {
				list.add(new TablePojo(id, filterSelectorLabels.get(i), filterSelectors.get(i), page));
			}
		}

		list.add(new TablePojo(id, scheduleSelectorLabel, page, this));
		if(!Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.hideschedulenametypedropdown"))
			list.add(new TablePojo(id, dropdownScheduleNameLabel, dropdownScheduleNames, page));
		list.add(new TablePojo(id, scheduleStartLabel, scheduleStartPicker, page));
		list.add(new TablePojo(id, scheduleEndLabel, scheduleEndPicker, page));

		//if (configuration.showStandardIntervals || isExpert) {
		//	list.add(new TablePojo(id, intervalDropLabel, intervalDrop, page));
		//}

		//if (configuration.showNrPointsPreview || isExpert) {
		list.add(new TablePojo(id, nrDataPointsLabel, nrDataPoints, page));
		//}

		//if (configuration.showOptionsSwitch || isExpert) {
		if(Boolean.getBoolean("org.ogema.app.timeseries.viewer.expert.gui.usemultiselectbybuttons"))
			list.add(new TablePojo(id, optionsLabel, multiSelectOptions.getStaticTable(), null, page));
		else
			list.add(new TablePojo(id, optionsLabel, optionsCheckbox, page));
		//}
		list.add(new TablePojo(id, lineTypeLabel, lineTypeSelector, page));

		//if (configuration.showIndividualConfigBtn || isExpert) {
		//	list.add(new TablePojo(id, triggerIndividualConfigLabel, triggerIndividualConfigPopupButton, page));
		//}

		list.add(new TablePojo(id, updateLabel, updateButton, page));

		return list;
	}

	protected void finishBuildingPage(String tableId, boolean showProgramSelector, boolean showFilterSelector) {
		appendDynamicTable(tableId, showProgramSelector, showFilterSelector);

		if (saveConfigurationButton != null) {
			this.append(saveConfigurationButton, null).linebreak(null);
		}
		this.append(schedulePlot, null).linebreak(null);
		if (csvDownload != null) {
			this.append(downloadHeader, null).linebreak(null).append(csvDownload, null).linebreak(null);
		}

		if (manipulator != null) {
			this.append(manipulatorHeader, null).linebreak(null).append(manipulator, null);
		}
		if (individualConfigPopup != null)
			this.append(individualConfigPopup, null);
	}

	// we cannot use registerDependentWidget here, because this would make
	// scheduleSelector a governing widget, and
	// hence we could not use it anymore to trigger any other widgets on the page
	private void setDependencies() { 
		//scheduleSelector.triggerAction(scheduleStartPicker, POST_REQUEST, GET_REQUEST);
		//scheduleSelector.triggerAction(scheduleEndPicker, POST_REQUEST, GET_REQUEST);
		//if (configuration.showNrPointsPreview) {
		//scheduleSelector.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST, 1);
		scheduleStartPicker.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST);
		scheduleEndPicker.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST);
		//}

		if (manipulator != null)
			updateButton.triggerAction(manipulator, POST_REQUEST, GET_REQUEST);
		if (csvDownload != null)
			updateButton.triggerAction(csvDownload, POST_REQUEST, GET_REQUEST);
		updateButton.triggerAction(schedulePlot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);

		if (programSelectors != null) {
			for (OgemaWidget programSelector : programSelectors) {
//				programSelector.triggerAction(scheduleSelector, POST_REQUEST, GET_REQUEST);
				programSelector.triggerAction(scheduleStartPicker, POST_REQUEST, GET_REQUEST, 1);
				programSelector.triggerAction(scheduleEndPicker, POST_REQUEST, GET_REQUEST, 1);
				//if (configuration.showNrPointsPreview)
				programSelector.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST, 2);
			}
		}
		if (filterSelectors != null) {
			for (ConditionalProgramSelector filterSelector : filterSelectors) {
//				filterSelector.instanceSelector.triggerAction(scheduleSelector, POST_REQUEST, GET_REQUEST);
				filterSelector.instanceSelector.triggerAction(scheduleStartPicker, POST_REQUEST, GET_REQUEST, 1);
				filterSelector.instanceSelector.triggerAction(scheduleEndPicker, POST_REQUEST, GET_REQUEST, 1);
//				filterSelector.filterSelector.triggerAction(scheduleSelector, POST_REQUEST, GET_REQUEST);
				filterSelector.filterSelector.triggerAction(scheduleStartPicker, POST_REQUEST, GET_REQUEST, 1);
				filterSelector.filterSelector.triggerAction(scheduleEndPicker, POST_REQUEST, GET_REQUEST, 1);
				//if (configuration.showNrPointsPreview) {
				filterSelector.instanceSelector.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST, 2);
				filterSelector.filterSelector.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST, 2);
				//}
			}
		}
		if (intervalDrop != null) {
			intervalDrop.triggerAction(scheduleStartPicker, POST_REQUEST, GET_REQUEST);
			intervalDrop.triggerAction(scheduleEndPicker, POST_REQUEST, GET_REQUEST);
			// must be updated after start and end picker
			if (nrDataPoints != null)
				intervalDrop.triggerAction(nrDataPoints, POST_REQUEST, GET_REQUEST, 1);
		}

		// trigger subwidgets
		//if (configuration.showNrPointsPreview)
		this.triggerAction(nrDataPoints, GET_REQUEST2, GET_REQUEST, 1);
		if (triggerIndividualConfigPopupButton != null)
			individualConfigPopup.trigger(triggerIndividualConfigPopupButton);
	}

	/**
	 * updates the ScheduleSelector and TimeSeriesFilter depending on the ConfigurationProvider in the onGet of the parent widgets
	 * @param req
	 * @param configId
	 */
	protected void updateSelectionConfiguration(OgemaHttpRequest req, String configId) {
		final SessionConfiguration sessionConfig = configProvider(req).getSessionConfiguration(configId);
		if (sessionConfig != null) {
			scheduleSelector(req).overwriteScheduleSelectorWithProvider(req, sessionConfig);
			updateConditionalTimeSeriesFilterCategoryPreselected(req, sessionConfig);
			updateFiltersPreSelected(req, sessionConfig);
		}

	}

	/**
	 * updates the TimeSeriesFilter depending on the ConfigurationProvider in the onGet of the parent widgets
	 * @param req
	 * @param sessionConfig
	 */
	private void updateConditionalTimeSeriesFilterCategoryPreselected(OgemaHttpRequest req,
			SessionConfiguration sessionConfig) {
		Integer preselectedFilter = sessionConfig.conditionalTimeSeriesFilterCategoryPreselected();

		if (configuration(req).filters != null && !configuration(req).filters.isEmpty()) {
			final List<Map<String, ConditionalTimeSeriesFilter<?>>> providerFilters = configuration(req).filters;
			for (ConditionalProgramSelector dropdown : filterSelectors) {
				if (preselectedFilter < providerFilters.size()) {
					Map<String, ConditionalTimeSeriesFilter<?>> providerFilter = providerFilters.get(preselectedFilter);
					boolean sameFilterSet = dropdown.sameFilterIds(providerFilter.keySet());
					if (sameFilterSet) {
						if (providerFilter != null && !providerFilter.isEmpty()) {
							dropdown.filterSelector.selectDefaultItem(providerFilter.get("0"));
						}
					} else {
						dropdown.filterSelector.selectDefaultItem(null);
					}
				}
			}
		}

	}

	/**
	 * updates the FiltersPreSelected depending on the ConfigurationProvider in the onGet of the parent widgets
	 * @param req
	 * @param sessionConfig
	 */
	private void updateFiltersPreSelected(OgemaHttpRequest req, SessionConfiguration sessionConfig) {
		final boolean overwrite = sessionConfig.overwriteConditionalFilters();
		final PreSelectionControllability filterControllability = sessionConfig.filterControllability();
		List<Map<String, ConditionalTimeSeriesFilter<?>>> providerFilters;
		if (configuration(req).filters != null) {
			providerFilters = configuration(req).filters;
		} else {
			return;
		}

		if (overwrite) { // show only the filters from the sessionConfig
			for (ConditionalProgramSelector dropdown : filterSelectors) {
				for (Map<String, ConditionalTimeSeriesFilter<?>> providerFilter : providerFilters) {
					boolean sameFilterSet = dropdown.sameFilterIds(providerFilter.keySet());
					if (sameFilterSet) {
						for (ConditionalTimeSeriesFilter<?> preSelectedFilter : sessionConfig.filtersPreSelected()) {
							dropdown.filterSelector.selectDefaultItem(preSelectedFilter);
						}

						boolean show = !sessionConfig.filtersPreSelected().isEmpty();
						// if filters from config isEmpty then hide the dropdown
						dropdown.setDefaultVisibility(show);
					}
				}
			}
		} else {// show all Filters
			for (ConditionalProgramSelector dropdown : filterSelectors) {
				for (Map<String, ConditionalTimeSeriesFilter<?>> providerFilter : providerFilters) {
					boolean sameFilterSet = dropdown.sameFilterIds(providerFilter.keySet());
					if (sameFilterSet) {
						for (ConditionalTimeSeriesFilter<?> preSelectedFilter : sessionConfig.filtersPreSelected()) {
							dropdown.filterSelector.selectDefaultItem(preSelectedFilter);
						}
						dropdown.setDefaultVisibility(true);
					}
				}
			}
		}

		if (filterControllability == PreSelectionControllability.FIXED
				|| filterControllability == PreSelectionControllability.MAX_SIZE) {
			disable(req);
		}
	}

	/**
	 * Adds 2 Time Series Filter Extended Collections to one - without duplications
	 * @param allFilters
	 * @param programmPreselected
	 * @return
	 */
	private Collection<TimeSeriesFilterExtended> getSubsetwithPreselectedFilters(
			Collection<? extends TimeSeriesFilter> allFilters,
			Collection<TimeSeriesFilterExtended> programmPreselected) {

		final TreeSet<String> allFilterIds = new TreeSet<>();
		final Collection<TimeSeriesFilter> result = new ArrayList<>();

		for (final TimeSeriesFilter filter : allFilters) {
			allFilterIds.add(filter.id());
		}

		for (final TimeSeriesFilter filter : programmPreselected) {
			if (allFilterIds.contains(filter.id())) {
				result.add(filter);
			}
		}
		return ScheduleViewerUtil.getInstance().parse(result, am.getResourceAccess());
	}


	/*
	 ************* Methods to be overridden in derived class *******
	 */

	/**
	 * Determine the items that shall be displayed in the multi-select (items that
	 * are available to be chosen by user). Note that these items are not selected
	 * automatically. By default, the method returns those schedules that have been
	 * set via the methods {@link #setDefaultSchedules(Collection)} or
	 * {@link #setSchedules(List, OgemaHttpRequest)}.<br>
	 * 
	 * Override in subclass to specify a different behaviour and to perform other
	 * operations that would be placed in the onGET method of a widget.
	 * 
	 * @param req
	 * @return
	 */
	protected List<ReadOnlyTimeSeries> getDefaultSchedules(OgemaHttpRequest req) {
		return scheduleSelector(req).getItems(req);
	}

	/*
	 ************** Public methods *************
	 */

	/**
	 * Get a reference to the schedule selector widget.
	 */
	/*@Override
	public final TemplateMultiselect<ReadOnlyTimeSeries> getScheduleSelector() {
		return (TemplateMultiselect<ReadOnlyTimeSeries>) scheduleSelector;
	}*/

	@Override
	//public final SchedulePlotlyjs getSchedulePlot() {
	public final TimeSeriesPlot<?,?,?> getSchedulePlot() {
		return schedulePlot;
	}

	/**
	 * Set the selectable schedules for a particular user session.
	 * 
	 * @param schedules
	 * @param req
	 */
	public void setSchedules(Collection<ReadOnlyTimeSeries> schedules, OgemaHttpRequest req) {
		// FIXME: System.out.println("setSchedules: " + schedules.size());
		scheduleSelector(req).update(schedules, req);
	}

	/**
	 * Set the selectable schedules globally.
	 * 
	 * @param items
	 */
	/*public void setDefaultSchedules(Collection<ReadOnlyTimeSeries> items) {
		scheduleSelector.selectDefaultItems(items); 
	}*/

	/**
	 * Get all schedules for a particular session.
	 * 
	 * @param req
	 * @return
	 */
	@Override
	public List<ReadOnlyTimeSeries> getSchedules(OgemaHttpRequest req) {
		return scheduleSelector(req).getItems(req);
	}

	/**
	 * Get all selected schedules in a particular session.
	 * 
	 * @param req
	 * @return
	 */
	public List<ReadOnlyTimeSeries> getSelectedSchedules(OgemaHttpRequest req) {
		final HashSet<ReadOnlyTimeSeries> set = new HashSet<>(scheduleSelector(req).getSelectedItems(req));
		return new ArrayList<ReadOnlyTimeSeries>(set);
	}

	@Override
	public void selectSchedules(Collection<ReadOnlyTimeSeries> selected, OgemaHttpRequest req) {
		// FIXME: System.out.println("selectSchedules: " + selected.size() +"/"+scheduleSelector.getItems(req).size());
		scheduleSelector(req).selectItems(new HashSet<>(selected), req);
	}

	@Override
	public List<ReadOnlyTimeSeries> getSelectedItems(OgemaHttpRequest req) {
		final HashSet<ReadOnlyTimeSeries> set = new HashSet<>(scheduleSelector(req).getSelectedItems(req));
		return new ArrayList<ReadOnlyTimeSeries>(set);
	}

	@Override
	public void setStartTime(long start, OgemaHttpRequest req) {
		scheduleStartPicker.getData(req).explicitDate = start;
	}

	public long getStartTime(OgemaHttpRequest req) {
		return scheduleStartPicker.getDateLong(req);
	}

	@Override
	public void setEndTime(long end, OgemaHttpRequest req) {
		scheduleEndPicker.getData(req).explicitDate = end;
	}

	public long getEndTime(OgemaHttpRequest req) {
		return scheduleEndPicker.getDateLong(req);
	}

	/**
	 * Set default plot configurations
	 * 
	 * @return
	 */
	@Override
	public Plot2DConfiguration getDefaultPlotConfiguration() {
		return schedulePlot.getDefaultConfiguration();
	}

	/**
	 * Set session-specific plot configurations
	 * 
	 * @param req
	 * @return
	 */
	@Override
	public Plot2DConfiguration getPlotConfiguration(OgemaHttpRequest req) {
		return schedulePlot.getConfiguration(req);
	}

	/**
	 * May be null
	 * 
	 * @return
	 */
	public TemplateDropdown<Long> getIntervalDropdown() {
		return intervalDrop;
	}


	public DisplayTemplate<ReadOnlyTimeSeries> getDisplayTemplate(OgemaHttpRequest req) {
		return scheduleSelector(req).getData(req).getTemplate();
	}
	
	SessionConfiguration getSessionConfiguration(OgemaHttpRequest req) {
		String configurationId = ScheduleViewerUtil.getPageParameter(req, page, PARAM_SESSION_CONFIG_ID);
		return configProvider(req).getSessionConfiguration(configurationId);
	}
	
	/*private boolean isWrongProvider(OgemaHttpRequest req) {
		final String providerId = configProvider.getConfigurationProviderId();
		final String providerIdFromURL = ScheduleViewerUtil.getPageParameter(req, page, PARAM_PROVIDER_ID);		
		return !providerId.equals(providerIdFromURL);
	}*/
	

	private Button getSaveConfigurationButton(String id) {
		Button button = new Button(page, id + "_saveConfigurationButton", "Save Configuration") {

			private static final long serialVersionUID = -6490863998981034894L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if (!Boolean.getBoolean("de.iwes.widgets.reswidget.scheduleviewer.expert.clone.hideSaveButton")) {
					setWidgetVisibility(true, req);
				} else {
					setWidgetVisibility(false, req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				Integer conditionalTimeSeriesFilterCategoryPreselected = 0;
				final PersistentSelectionConfiguration currentConfiguration = new PersistentSelectionConfiguration();

				if (filterSelectors != null) {
					for (ConditionalProgramSelector selector : filterSelectors) {
						ConditionalTimeSeriesFilter<?> item = selector.filterSelector.getSelectedItem(req);

						if (item != null) {
							currentConfiguration.filtersPreSelected().add(item);
						}
					}
				}
				currentConfiguration.setConditionalTimeSeriesFilterCategoryPreselected(
						conditionalTimeSeriesFilterCategoryPreselected);

				List<TemplateMultiselect<TimeSeriesFilterExtended>> myProgramSelectors = programSelectors;

				for (TemplateMultiselect<TimeSeriesFilterExtended> selector : myProgramSelectors) {
					List<TimeSeriesFilter> items = ScheduleViewerUtil.reparse(selector.getSelectedItems(req));
					currentConfiguration.programsPreselected().add(items);
				}

				List<ReadOnlyTimeSeries> schedules = getSelectedSchedules(req);
				currentConfiguration.timeSeriesSelected().addAll(schedules);
				configProvider(req).saveCurrentConfiguration(currentConfiguration, null);
			}
		};

		return button;
	}

	/**
	 * Returns true if an "expert mode" parameter is set in the URL and this is true, otherwise false is returned
	 * @param req
	 * @return
	 */
	private boolean isExpertMode(OgemaHttpRequest req) {
		String expertMode = ScheduleViewerUtil.getPageParameter(req, page, PARAM_EXPERT_MODE);
		return Boolean.valueOf(expertMode);
	}

	@Override
	public TemplateMultiselect<ReadOnlyTimeSeries> getScheduleSelector() {
		throw new IllegalStateException("ScheduleSelector is not global here, must be accessed via session!");
	}

}