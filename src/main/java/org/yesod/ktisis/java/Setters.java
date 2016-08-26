/**
 * Copyright (c) 2016 Pelco. All rights reserved.
 * $Id$
 *
 * This file contains trade secrets of Pelco.  No part may be reproduced or
 * transmitted in any form by any means or for any purpose without the express
 * written permission of Pelco.
 */

package org.yesod.ktisis.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;
import org.yesod.ktisis.base.FeatureTags.Feature;
import org.yesod.ktisis.base.WhitespaceHelper;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Setters! 
 */
@Feature(Setters.TAG)
public class Setters
{
  public static final String TAG = "setters";
  private static final Pattern COLLECTION_VALUE_TYPE_PATTERN = Pattern.compile("<(\\w+)>");
  private static final Pattern MAP_KV_PATTERN = Pattern.compile("Map<(\\w+), (\\w+)>");

  private InputStream getTemplate(String baseName, VariableResolver ctx) throws IOException
  {
    return TemplateProcessor.getResource("templates/ktisis/java/setters/" + baseName, Setters.class);
  }

  @ExtensionPoint("getters")
  public String setters(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    HashMap<String, String> extra = new HashMap<>();
    extra.put("classname", variableResolver.getAs("name", String.class).get());
    for (Map<?, ?> fieldAttrs : ClassBase.getFields(variableResolver))
    {
      if (BasicBuilder.isCollector(fieldAttrs))
      {
        String type = (String) fieldAttrs.get("type");
        if (type.contains("Map"))
        {
          Matcher matcher = MAP_KV_PATTERN.matcher(type);
          Preconditions.checkArgument(matcher.find());
          extra.putAll(ImmutableMap.of("key_type", matcher.group(1), "value_type", matcher.group(2)));
          try (InputStream template = getTemplate("MapSetter.template", variableResolver))
          {
            lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(extra::get, fieldAttrs::get, variableResolver)));
          }
        }
        else
        {
          Matcher matcher = COLLECTION_VALUE_TYPE_PATTERN.matcher(type);
          Preconditions.checkArgument(matcher.find());
          extra.putAll(ImmutableMap.of("value_type", matcher.group(1)));
          try (InputStream template = getTemplate("CollectionSetter.template", variableResolver))
          {
            lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(extra::get, fieldAttrs::get, variableResolver)));
          }
        }
      }
      else
      {
        try (InputStream template = getTemplate("Setter.template", variableResolver))
        {
          lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(extra::get, fieldAttrs::get, variableResolver)));
        }
      }
    }
    return Joiner.on(WhitespaceHelper.lf()).join(lines);
  }
}
