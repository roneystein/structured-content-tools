/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.elasticsearch.tools.content;

import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.support.XContentMapValues;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


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


        DateTime previousDate;
        DateTime eventDate;

        int hoursSpent = 0;

        if (data == null)
            return null;

        try {
            previousDate = handleDateExtractionAndParsing(fieldSourceCreateDate, sourceDateFormat, data, chainContext);
        } catch (DataProblemException e) {
            return data;
        }

        // Changelog fields
        // # of changes : changelog.total
        // Trans. Time  : changelog.n.created
        // # of Items   : changelog.n.items (LIST)
        // Field        : changelog.n.n.field
        // Source Status: changelog.n.n.fromString
        // Dest Status  : changelog.n.n.toString


        // If the changelog is not empty
        Object numHistoriesObject = XContentMapValues.extractValue("changelog.total", data);
        int numHistories = 0;

        if ( ! ValueUtils.isEmpty(numHistoriesObject) ) {
            numHistories = Integer.parseInt(numHistoriesObject.toString());
        }

        // Testing
        StructureUtils.putValueIntoMapOfMaps(data, "fields.NumeroHistories", numHistories);

        // "histories" is the list of changes
        List<Object> historyList = (List<Object>) XContentMapValues.extractValue("changelog.histories", data);

        // TODO: change for length
        //for (int i = 0; i < numHistories; i++) {
        for (int i = 0; i < historyList.size(); i++) {

            // Get this history
            Map<String, Object> oneHistory = (Map<String,Object>) historyList.get(i);
            // Get this history's items list
            List<Object> itemsList = (List<Object>) oneHistory.get("items");
            int numItems = itemsList.size();

            // Testing: number of items stored
            oneHistory.put("numberOfItems", numItems);

            // In each item...
            for (int n = 0; n < itemsList.size(); n++) {

                // Get the item
                Object itemObject = itemsList.get(n);
                if ( itemObject instanceof Map ) {
                    Map<String, Object> item = (Map<String, Object>) itemsList.get(n);
                    Object fieldValueObj = item.get("field");
                    if (fieldValueObj instanceof String) {
                        String fieldValue = (String) fieldValueObj;
                        if (fieldValue.equals("status")) {
                            // This is a status transition
                            // Get the history/change timestamp
                            try {
                                eventDate = handleDateParsing(sourceDateFormat, oneHistory.get("created"), chainContext);
                            } catch (DataProblemException e) {
                                eventDate = null;
                            }
                            //TODO: Computes the date difference and stores it
                            hoursSpent = 1;
                            item.put(fieldTarget, hoursSpent);

                            previousDate = eventDate;

                        }
                    } else { System.out.println("__NOT_INSTANCE_STRING"); }
                } else { System.out.println("__NOT_INSTANCE_MAP"); }
            }
        }
        return data;
    }



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


	public String getFieldTarget() {
		return fieldTarget;
	}


	/**
	 * An util method to extract date value out from the field and parse it using the given date format.
	 *
	 * @param settings
	 * @param cfgDateLocation
	 * @param cfgDateFormat
	 * @return parsed date object
	 */
	protected DateTime handleDateExtractionAndParsing(String dateField, String dateFormat, Map<String, Object> data,
												  PreprocessChainContext chainContext) throws DataProblemException {

        if (dateField == null)
            return null;

        Object dateFieldData = null;
        if (dateField.contains(".")) {
            dateFieldData = XContentMapValues.extractValue(dateField, data);
        } else {
            dateFieldData = data.get(dateField);
        }

        return handleDateParsing(dateFormat, dateFieldData, chainContext);
    }

    /**
     * An util method to parse date value using the given date format.
     *
     * @param dateFormat Date format
     * @param dateFieldData object from the data field extraction
     * @param chainContext chainContext
     * @return parsed date object
     */
    protected DateTime handleDateParsing(String dateFormat, Object dateFieldData, PreprocessChainContext chainContext) throws DataProblemException {

        if (dateFieldData == null)
            return null;

        DateTime resultDate = null;

        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(dateFormat);

        if (dateFieldData != null) {
            if (!(dateFieldData instanceof String)) {
                String msg = "Value for field is not a String, so can't be parsed to the date object.";
                addDataWarning(chainContext, msg);
                throw new DataProblemException();
            } else {
                String dateStr = dateFieldData.toString();
                if (dateStr != null && !dateStr.isEmpty()) {
//                    synchronized (dateFormatter) {
//                        dateFormatter.applyPattern(dateFormat);

                        try {
                            resultDate = dateFormatter.parseDateTime(dateStr);
                        } catch (Exception e) {
                            String msg = "Parameter value of " + dateStr + " could not be parsed using " + dateFormat
                                    + " format.";
                            addDataWarning(chainContext, msg);
                            throw new DataProblemException();
                        }
//                    }
                }
            }
        }

        return resultDate;
    }


    /**
     * An utility exception to handle data exceptions navigation nicely in this preprocessor.
     */
    class DataProblemException extends Exception {
        private static final long serialVersionUID = 1L;
    }

}
