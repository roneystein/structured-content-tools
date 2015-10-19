/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.elasticsearch.tools.content;

import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;



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
	protected static final String CFG_DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	protected static final String CFG_DEFAULT_CREATE_DATE_FIELD = "fields.created";
	protected String fieldTarget;
	protected String fieldSourceCreateDate;
	protected String sourceDateFormat;
    protected SimpleDateFormat dateFormatter = new SimpleDateFormat();
    protected boolean removeNotStatus = true;

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

        String previousDateStr;
        String eventDateStr;

        int hoursSpent = 0;

        if (data == null)
            return null;

        Object previousDateObj = XContentMapValues.extractValue(fieldSourceCreateDate, data);
        if ( previousDateObj instanceof String) {
            previousDateStr = (String) previousDateObj;
        } else {
            previousDateStr = null;
        }

        // Get some fields to replicate to the child changelog
        String issue_type = (String) XContentMapValues.extractValue("fields.issuetype.name", data);
        String project_name = (String) XContentMapValues.extractValue("fields.project.name", data);

        // If the changelog is not empty
        Object numHistoriesObject = XContentMapValues.extractValue("changelog.total", data);
        int numHistories = 0;

        if ( ! ValueUtils.isEmpty(numHistoriesObject) ) {
            numHistories = Integer.parseInt(numHistoriesObject.toString());
        }

        // "histories" is the list of changes
        List<Object> historyList = (List<Object>) XContentMapValues.extractValue("changelog.histories", data);

        for (int i = 0; i < historyList.size(); i++) {

            // Get this history
            Map<String, Object> oneHistory = (Map<String,Object>) historyList.get(i);

            // Adds some fields...
            oneHistory.put("issue_type", issue_type);
            oneHistory.put("project_name", project_name);

            // Get this history's items list
            List<Object> itemsList = (List<Object>) oneHistory.get("items");


            List<Integer> itemsToRemove = new ArrayList<>();

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

                            eventDateStr = StructureUtils.getStringValue(oneHistory, "created");

                            //TODO: Computes the date difference and stores it
                            WorkingTime workingTime = new WorkingTime(previousDateStr, eventDateStr);
                            hoursSpent = workingTime.getWorkingHoursRoundUp();
                            item.put(fieldTarget, hoursSpent);

                            previousDateStr = eventDateStr;

                        } else {
                            itemsToRemove.add(0,n);
                        }
                    }
                }
            }

            if (removeNotStatus) {
                for (int n: itemsToRemove) {
                    itemsList.remove(n);
                }
            }

        }
        return data;
    }


}
