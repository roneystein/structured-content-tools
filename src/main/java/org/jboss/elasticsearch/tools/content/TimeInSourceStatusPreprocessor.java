/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.elasticsearch.tools.content;

import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.support.XContentMapValues;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * JIRA Content preprocessor which calculates time spent on source status and store as a new field in the changelog
 * 
 * <pre>
 * { 
 *     "name"     : "State Time Processor",
 *     "class"    : "org.jboss.elasticsearch.tools.content.TimeInSourceStatusPreprocessor",
 *     "settings" : {
 *         "target_field"  : "time_in_source"
 *     }
 * }
 * </pre>
 * 
 * Options are:
 * <ul>
 * <li><code>target_field</code> - single value field to store time in number of hours. Dot notation can be used
 * here for structure nesting. If collected list is empty nothing is stored here.
 * </ul>
 * 
 * @author Roney Stein
 */
public class TimeInSourceStatusPreprocessor extends StructuredContentPreprocessorBase {

	protected static final String CFG_TARGET_FIELD = "target_field";
	// JIRA Date format: "2015-10-06T13:42:55.837-0300"
	protected static final String CFG_DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	protected static final String CFG_DEFAULT_CREATE_DATE_FIELD = "fields.created";
	protected String fieldTarget;
	protected String fieldSourceCreateDate;
	protected String sourceDateFormat;
	protected SimpleDateFormat dateFormatter = new SimpleDateFormat();

	@SuppressWarnings("unchecked")
	@Override
	public void init(Map<String, Object> settings) throws SettingsException {
		if (settings == null) {
			throw new SettingsException("'settings' section is not defined for preprocessor " + name);
		}
		fieldTarget = XContentMapValues.nodeStringValue(settings.get(CFG_TARGET_FIELD), null);
		validateConfigurationStringNotEmpty(fieldTarget, CFG_TARGET_FIELD);
		sourceDateFormat = CFG_DEFAULT_DATE_FORMAT;
		fieldSourceCreateDate = CFG_DEFAULT_CREATE_DATE_FIELD;
	}

	@Override
	public Map<String, Object> preprocessData(Map<String, Object> data, PreprocessChainContext chainContext) {

		Date createDate;
		Date eventDate;

		if (data == null)
			return null;

		createDate = handleDateExtractionAndParsing(fieldSourceCreateDate, sourceDateFormat, data, chainContext);

		// Gets the changelog

		// Parses the changelog



		/**
		for (String sourceField : fieldsSource) {
			if (ValueUtils.isEmpty(sourceField))
				continue;
			Object v = XContentMapValues.extractValue(sourceField, data);
			collectValue(vals, v);
		}
		if (vals != null && !vals.isEmpty()) {
			StructureUtils.putValueIntoMapOfMaps(data, fieldTarget, new ArrayList<Object>(vals));
		} else {
			StructureUtils.putValueIntoMapOfMaps(data, fieldTarget, null);
		}
		 **/

		return data;
	}

	@SuppressWarnings("unchecked")
	private void collectValue(Set<Object> values, Object value) {
		if (value != null) {
			if (value instanceof Collection) {
				for (Object o : ((Collection<Object>) value))
					if ( fieldDeepCopy ) {
						collectValue(values, StructureUtils.getADeepStructureCopy(o));
					} else {
						collectValue(values, o);
					}
			} else {
				if ( fieldDeepCopy ) {
					values.add(StructureUtils.getADeepStructureCopy(value));
				} else {
					values.add(value);
				}
			}
		}
	}

	public String getFieldTarget() {
		return fieldTarget;
	}

	public List<String> getFieldsSource() {
		return fieldsSource;
	}


	/**
	 * An util method to extract date value out from the field and parse it using the given date format.
	 *
	 * @param settings
	 * @param cfgDateLocation
	 * @param cfgDateFormat
	 * @return parsed date object
	 */
	protected Date handleDateExtractionAndParsing(String dateField, String dateFormat, Map<String, Object> data,
												  PreprocessChainContext chainContext) throws DataProblemException {

		if (dateField == null)
			return null;

		Date resultDate = null;

		Object dateFieldData = null;
		if (dateField.contains(".")) {
			dateFieldData = XContentMapValues.extractValue(dateField, data);
		} else {
			dateFieldData = data.get(dateField);
		}

		if (dateFieldData != null) {
			if (!(dateFieldData instanceof String)) {
				String msg = "Value for field '" + dateField + "' is not a String, so can't be parsed to the date object.";
				addDataWarning(chainContext, msg);
				throw new DataProblemException();
			} else {
				String dateStr = dateFieldData.toString();
				if (dateStr != null && !dateStr.isEmpty()) {
					synchronized(dateFormatter) {
						dateFormatter.applyPattern(dateFormat);
						try {
							resultDate = dateFormatter.parse(dateStr);
						} catch (ParseException e) {
							String msg = dateField + " parameter value of " + dateStr + " could not be parsed using " + dateFormat
									+ " format.";
							addDataWarning(chainContext, msg);
							throw new DataProblemException();
						}
					}
				}
			}
		}

		return resultDate;
	}

	/**
	 * Parses a JIRA changelog for transitions, calculates the time spent in source field, adds it as number of hours.
	 *
	 * @param data with the changelog structure
	 * @return modified data
	 */
	@SuppressWarnings("unchecked")
	public static Object getADeepStructureCopy( Object data ) {

		if ( root==null ) {

			return null;

		} else if ( root instanceof List ) {

			List<Object> rootList = (List<Object>)root;
			List<Object> copy = new LinkedList<Object>();

			for ( Object elem : rootList ) {
				Object copiedElem = getADeepStructureCopy(elem);
				if ( copiedElem==null ) continue;
				copy.add(copiedElem);
			}
			return copy;

		} else if ( root instanceof Map ) {

			Map<String,Object> rootMap = (Map<String,Object>)root;
			Map<String,Object> copy = new LinkedHashMap<String,Object>(rootMap.size());

			for ( String key : rootMap.keySet() ) {
				Object copiedElem = getADeepStructureCopy( rootMap.get(key) );
				if ( copiedElem==null ) continue;
				copy.put( key, copiedElem );
			}
			return copy;

		} else {

			// Since it's neither a List nor a Map, it has to be an immutable value which we can copy by reference.
			return root;
		}
	}

}
