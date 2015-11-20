package org.yesod.ktisis.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class BasicBuilder
{
  private static final Pattern COLLECTION_VALUE_TYPE_PATTERN = Pattern.compile("<(\\w+)>");
  private static final Pattern MAP_KV_PATTERN = Pattern.compile("Map<(\\w+), (\\w+)>");

  @ExtensionPoint("builder")
  public String builder(VariableResolver variableResolver) throws IOException
  {
    try (InputStream template = TemplateProcessor.getResource("templates/ktisis/java/Builder.template", BasicBuilder.class))
    {
      VariableResolver subResolver = (s) ->
      {
        switch (s)
        {
          case "builder_ctor_args":
            return ctorArgs(variableResolver);
          default:
            return variableResolver.apply(s);
        }
      };
      return TemplateProcessor.processTemplate(template, subResolver);
    }
  }

  public static boolean isCollector(Map<?, ?> field)
  {
    // TODO: validate collector type
    return field.get("collector") == Boolean.TRUE;
  }

  private static String collectorField(Map<?, ?> field)
  {
    String type = (String) field.get("type");
    String name = (String) field.get("name");
    if (type.contains("Map"))
    {
      Matcher matcher = MAP_KV_PATTERN.matcher(type);
      Preconditions.checkArgument(matcher.find());
      return String.format("    Map<%s, %s> %s = new HashMap<>();", matcher.group(1), matcher.group(2), name);
    }
    String collectType;
    if (type.contains("List"))
    {
      collectType = "ArrayList";
    }
    else if (type.contains("Set"))
    {
      collectType = "HashSet";
    }
    else
    {
      throw new IllegalArgumentException("Type not supported for collector");
    }
    Matcher matcher = COLLECTION_VALUE_TYPE_PATTERN.matcher(type);
    Preconditions.checkArgument(matcher.find());
    String paramType = matcher.group(1);
    return String.format("    Collection<%s> %s = new %s<>();", paramType, name, collectType);
  }

  private String collectorCtorArg(Map<?, ?> field)
  {
    String type = (String) field.get("type");
    String name = (String) field.get("name");
    String collection = "Collection";
    if (type.contains("Map"))
    {
      collection = "Map";
    }
    else if (type.contains("List"))
    {
      collection = "List";
    }
    else if (type.contains("Set"))
    {
      collection = "Set";
    }
    return String.format("Immutable%s.copyOf(%s)", collection, name);
  }

  private String ctorArgs(VariableResolver variableResolver)
  {
    List<String> builder = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(variableResolver))
    {
      if (isCollector(fieldAttrs))
      {
        builder.add(collectorCtorArg(fieldAttrs));
      }
      else
      {
        builder.add(fieldAttrs.get("name").toString());
      }
    }
    return Joiner.on(", ").join(builder);
  }

  @ExtensionPoint("builder_fields")
  public String builderFields(VariableResolver variableResolver)
  {
    Collection<String> lines = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(variableResolver))
    {
      if (isCollector(fieldAttrs))
      {
        lines.add(collectorField(fieldAttrs));
      }
      else
      {
        String format = "    private ${type} ${name};";
        if (fieldAttrs.containsKey("default"))
        {
          format = String.format("    private ${type} ${name} = %s;", fieldAttrs.get("default"));
        }
        lines.add(TemplateProcessor.processTemplate(format, VariableResolver.merge(fieldAttrs::get, variableResolver)));
      }
    }
    return Joiner.on("\n").join(lines);
  }

  @ExtensionPoint("builder_setters")
  public String builderSetters(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(variableResolver))
    {
      if (isCollector(fieldAttrs))
      {
        String type = (String) fieldAttrs.get("type");
        if (type.contains("Map"))
        {
          Matcher matcher = MAP_KV_PATTERN.matcher(type);
          Preconditions.checkArgument(matcher.find());
          Map<String, String> extra = ImmutableMap.of("key_type", matcher.group(1), "value_type", matcher.group(2));
          try (InputStream template = TemplateProcessor.getResource("templates/ktisis/java/MapSetter.template", BasicBuilder.class))
          {
            lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(extra::get, fieldAttrs::get, variableResolver)));
          }
        }
        else
        {
          Matcher matcher = COLLECTION_VALUE_TYPE_PATTERN.matcher(type);
          Preconditions.checkArgument(matcher.find());
          Map<String, String> extra = ImmutableMap.of("value_type", matcher.group(1));
          try (InputStream template = TemplateProcessor.getResource("templates/ktisis/java/CollectionSetter.template", BasicBuilder.class))
          {
            lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(extra::get, fieldAttrs::get, variableResolver)));
          }
        }
      }
      else
      {
        try (InputStream template = TemplateProcessor.getResource("templates/ktisis/java/Setter.template", BasicBuilder.class))
        {
          lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(fieldAttrs::get, variableResolver)));
        }
      }
    }
    return Joiner.on("\n").join(lines);
  }

  @ExtensionPoint("copy_body")
  public String copyBuilder(VariableResolver ctx)
  {
    Collection<String> lines = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(ctx))
    {
      String name = (String) fieldAttrs.get("name");
      String type = (String) fieldAttrs.get("type");
      if (isCollector(fieldAttrs))
      {
        lines.add(String.format("      this.add%s(original.%s());", ClassBase.upcase(name), ClassBase.getterName(name, type)));
      }
      else if (ClassBase.isOptional(fieldAttrs))
      {
        String def = (String) fieldAttrs.get("default");
        if (def != null)
        {
          lines.add(String.format("      this.%s = original.%s().or(%s);", name, ClassBase.getterName(name, type), def));
        }
        else
        {
          lines.add(String.format("      this.%s = original.%s().orNull();", name, ClassBase.getterName(name, type), def));
        }
      }
      else
      {
        lines.add(String.format("      this.%s = original.%s();", name, ClassBase.getterName(name, type)));
      }
    }
    return Joiner.on("\n").join(lines);
  }
}
