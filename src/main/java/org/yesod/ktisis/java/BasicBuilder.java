package org.yesod.ktisis.java;

import static org.yesod.ktisis.base.WhitespaceHelper.lf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;
import org.yesod.ktisis.base.FeatureTags.Feature;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@Feature(BasicBuilder.TAG)
public class BasicBuilder
{
  public static final String TAG = "builder";
  private static final Pattern COLLECTION_VALUE_TYPE_PATTERN = Pattern.compile("<(\\w+)>");
  private static final Pattern MAP_KV_PATTERN = Pattern.compile("Map<(\\w+), (\\w+)>");

  private InputStream getTemplate(String baseName, VariableResolver ctx) throws IOException
  {
    return TemplateProcessor.getResource("templates/ktisis/java/builder/" + baseName, BasicBuilder.class);
  }

  private boolean isExtensible(VariableResolver ctx)
  {
    return ctx.apply("extensible") == Boolean.TRUE;
  }

  private boolean superIsExtensible(VariableResolver ctx)
  {
    Map<?, ?> superProps = ctx.getAs("supertype", Map.class).orElse(Collections.EMPTY_MAP);
    return superProps.get("extensible") == Boolean.TRUE;
  }

  private String fieldScope(VariableResolver ctx)
  {
    return isExtensible(ctx) ? "protected" : "private";
  }

  @ExtensionPoint("builder")
  public String builder(VariableResolver variableResolver) throws IOException
  {
    try (InputStream template = getTemplate("Builder.template", variableResolver))
    {
      VariableResolver subResolver = (s) ->
      {
        switch (s)
        {
          case "builder_ctor_args":
            return ctorArgs(variableResolver);
          case "extend_clause":
            return superIsExtensible(variableResolver) ? String.format(" extends %s.Builder", superName(variableResolver)) : "";
          case "super_copy_clause":
            return superIsExtensible(variableResolver) ? lf() + "      super(original);" : "";
          default:
            return variableResolver.apply(s);
        }
      };
      return TemplateProcessor.processTemplate(template, subResolver);
    }
  }

  /** 
   * @param variableResolver
   * @return
   */
  private String superName(VariableResolver ctx)
  {
    Map<?, ?> superProps = ctx.getAs("supertype", Map.class).orElse(Collections.EMPTY_MAP);
    return superProps.get("name").toString();
  }

  public static boolean isCollector(Map<?, ?> field)
  {
    // TODO: validate collector type
    return field.get("collector") == Boolean.TRUE;
  }

  public static String baseCollectionType(String type)
  {
    // TODO: List<Map> will foil this
    if (type.contains("Map"))
    {
      return "Map";
    }
    if (type.contains("List"))
    {
      return "List";
    }
    if (type.contains("Set"))
    {
      return "Set";
    }
    return "Collection";
  }

  private static String collectorField(Map<?, ?> field, String fieldScope)
  {
    String type = (String) field.get("type");
    String name = (String) field.get("name");
    if (type.contains("Map"))
    {
      Matcher matcher = MAP_KV_PATTERN.matcher(type);
      Preconditions.checkArgument(matcher.find());
      Imports.addImport(HashMap.class);
      Imports.addImport(Map.class);
      return String.format("    %s Map<%s, %s> %s = new HashMap<>();", fieldScope, matcher.group(1), matcher.group(2), name);
    }
    String collectType;
    String baseType;
    if (type.contains("List"))
    {
      Imports.addImport(ArrayList.class);
      Imports.addImport(List.class);
      collectType = "ArrayList";
      baseType = "List";
    }
    else if (type.contains("Set"))
    {
      Imports.addImport(HashSet.class);
      Imports.addImport(Set.class);
      collectType = "HashSet";
      baseType = "Set";
    }
    else
    {
      throw new IllegalArgumentException("Type not supported for collector");
    }
    Imports.addImport(Collection.class);
    Matcher matcher = COLLECTION_VALUE_TYPE_PATTERN.matcher(type);
    Preconditions.checkArgument(matcher.find());
    String paramType = matcher.group(1);
    return String.format("    %s %s<%s> %s = new %s<>();", fieldScope, baseType, paramType, name, collectType);
  }

  private String collectorCtorArg(Map<?, ?> field, VariableResolver variableResolver)
  {
    String type = (String) field.get("type");
    String name = (String) field.get("name");
    String collection = "Collection";
    Class<?> importClause = ImmutableCollection.class;
    if (type.contains("Map"))
    {
      collection = "Map";
      importClause = ImmutableMap.class;
    }
    else if (type.contains("List"))
    {
      if (type.startsWith("List"))
      {
        Imports.addImport(List.class);
      }
      collection = "List";
      importClause = ImmutableList.class;
    }
    else if (type.contains("Set"))
    {
      if (type.startsWith("Set"))
      {
        Imports.addImport(Set.class);
      }
      collection = "Set";
      importClause = ImmutableSet.class;
    }
    if (!type.contains("Immutable"))
    {
      return name;
    }
    Imports.addImport(importClause);
    return String.format("Immutable%s.copyOf(%s)", collection, name);
  }

  private String ctorArgs(VariableResolver variableResolver)
  {
    List<String> builder = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(variableResolver))
    {
      if (isCollector(fieldAttrs))
      {
        builder.add(collectorCtorArg(fieldAttrs, variableResolver));
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
    String fieldScope = fieldScope(variableResolver);
    Collection<String> lines = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : getFields(variableResolver))
    {
      if (isCollector(fieldAttrs))
      {
        lines.add(collectorField(fieldAttrs, fieldScope));
      }
      else
      {
        String format = String.format("    %s ${type} ${name};", fieldScope);
        if (fieldAttrs.containsKey("default"))
        {
          format = String.format("    %s ${type} ${name} = %s;", fieldScope, fieldAttrs.get("default"));
        }
        lines.add(TemplateProcessor.processTemplate(format, VariableResolver.merge(fieldAttrs::get, variableResolver)));
      }
    }
    return Joiner.on(lf()).join(lines);
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
          try (InputStream template = getTemplate("MapSetter.template", variableResolver))
          {
            lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(extra::get, fieldAttrs::get, variableResolver)));
          }
        }
        else
        {
          Matcher matcher = COLLECTION_VALUE_TYPE_PATTERN.matcher(type);
          Preconditions.checkArgument(matcher.find());
          Map<String, String> extra = ImmutableMap.of("value_type", matcher.group(1));
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
          lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(fieldAttrs::get, variableResolver)));
        }
      }
    }
    return Joiner.on(lf()).join(lines);
  }

  private Collection<Map<?, ?>> getFields(VariableResolver ctx)
  {
    if (!superIsExtensible(ctx))
    {
      return ClassBase.getFieldsAndSuperFields(ctx);
    }
    return ClassBase.getFields(ctx);
  }

  @ExtensionPoint("copy_body")
  public String copyBuilder(VariableResolver ctx)
  {
    Collection<String> lines = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : getFields(ctx))
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
        def = def == null ? "null" : def;
        lines.add(String.format("      this.%s = original.%s().orElse(%s);", name, ClassBase.getterName(name, type), def));
      }
      else
      {
        lines.add(String.format("      this.%s = original.%s();", name, ClassBase.getterName(name, type)));
      }
    }
    return Joiner.on(lf()).join(lines);
  }

  @ExtensionPoint("builder")
  public String copyMember(VariableResolver ctx) throws IOException
  {
    if (superIsExtensible(ctx) || isExtensible(ctx))
    {
      try (InputStream template = getTemplate("CopyMember.template", ctx))
      {
        return TemplateProcessor.processTemplate(template, ctx);
      }
    }
    return null;
  }
}
