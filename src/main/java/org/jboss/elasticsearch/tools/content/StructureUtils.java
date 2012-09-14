package org.jboss.elasticsearch.tools.content;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.support.XContentMapValues;

/**
 * Utility functions for structured content manipulation. Structured content is commonly represented as Map of Maps
 * structure.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StructureUtils {

  /**
   * Get node value as {@link Integer} object instance if possible.
   * 
   * @param node to get value from
   * @return Integer value or null.
   * @throws NumberFormatException if value can't be converted to the int value
   * @see XContentMapValues#nodeIntegerValue(Object, int)
   */
  public static Integer nodeIntegerValue(Object node) throws NumberFormatException {
    if (node == null) {
      return null;
    }
    if (node instanceof Integer) {
      return (Integer) node;
    } else if (node instanceof Number) {
      return new Integer(((Number) node).intValue());
    }

    return Integer.parseInt(node.toString());
  }

  /**
   * Filter data in Map. Leave here only data with keys passed in second parameter.
   * 
   * @param map to filter data inside
   * @param keysToLeave keys leaved in map. If <code>null</code> or empty then no filtering is performed!
   */
  public static <T> void filterDataInMap(Map<T, Object> map, Set<T> keysToLeave) {
    if (map == null || map.isEmpty())
      return;
    if (keysToLeave == null || keysToLeave.isEmpty())
      return;

    Set<T> keysToRemove = new HashSet<T>(map.keySet());
    keysToRemove.removeAll(keysToLeave);
    if (!keysToRemove.isEmpty()) {
      for (T rk : keysToRemove) {
        map.remove(rk);
      }
    }
  }

  /**
   * Remap data in input Map. Leave here only data with defined keys, but change these keys to new ones if necessary.
   * Some new key can be same as some other old key, but if two new keys are same, then only latest value is preserved
   * (given by <code>mapToChange</code> key iteration order).
   * 
   * @param mapToChange Map to remap data inside. Must be mutable!
   * @param remapInstructions instructions how to remap. If <code>null</code> or empty then remap is not performed and
   *          <code>mapToChange</code> is not changed! Key in this Map must be same as key in <code>mapToChange</code>
   *          which may leave there. Value in this map means new key of value in <code>mapToChange</code> after
   *          remapping.
   */
  public static <T> void remapDataInMap(Map<T, Object> mapToChange, Map<T, T> remapInstructions) {
    if (mapToChange == null || mapToChange.isEmpty())
      return;
    if (remapInstructions == null || remapInstructions.isEmpty())
      return;

    Map<T, Object> newMap = new HashMap<T, Object>();
    for (T keyOrig : mapToChange.keySet()) {
      if (remapInstructions.containsKey(keyOrig)) {
        T keyNew = remapInstructions.get(keyOrig);
        newMap.put(keyNew, mapToChange.get(keyOrig));
      }
    }

    mapToChange.clear();
    mapToChange.putAll(newMap);
  }

  /**
   * Read JSON file from classpath into Map of Map structure.
   * 
   * @param filePath path inside jar/classpath pointing to JSON file to read
   * @return parsed JSON file
   * @throws SettingsException
   */
  public static Map<String, Object> loadJSONFromJarPackagedFile(String filePath) throws SettingsException {
    XContentParser parser = null;
    try {
      parser = XContentFactory.xContent(XContentType.JSON).createParser(
          StructureUtils.class.getResourceAsStream(filePath));
      return parser.mapAndClose();
    } catch (IOException e) {
      throw new SettingsException(e.getMessage(), e);
    } finally {
      if (parser != null)
        parser.close();
    }
  }

  /**
   * Put value into Map of Maps structure. Dot notation supported for deeper level of nesting.
   * 
   * @param map Map to put value into
   * @param field to put value into. Dot notation can be used.
   * @param value to be added into Map
   * @throws IllegalArgumentException if value can't be added due something wrong in data structure
   */
  @SuppressWarnings("unchecked")
  public static void putValueIntoMapOfMaps(Map<String, Object> map, String field, Object value)
      throws IllegalArgumentException {
    if (map == null)
      return;
    if (ValueUtils.isEmpty(field)) {
      throw new IllegalArgumentException("field argument must be defined");
    }
    if (field.contains(".")) {
      String[] tokens = field.split("\\.");
      int tokensCount = tokens.length;
      Map<String, Object> levelData = map;
      for (String tok : tokens) {
        if (tokensCount == 1) {
          levelData.put(tok, value);
        } else {
          Object o = levelData.get(tok);
          if (o == null) {
            Map<String, Object> lv = new LinkedHashMap<String, Object>();
            levelData.put(tok, lv);
            levelData = lv;
          } else if (o instanceof Map) {
            levelData = (Map<String, Object>) o;
          } else {
            throw new IllegalArgumentException("Cant put value for field '" + field
                + "' because some element in the path is not Map");
          }
        }
        tokensCount--;
      }
    } else {
      map.put(field, value);
    }
  }

}