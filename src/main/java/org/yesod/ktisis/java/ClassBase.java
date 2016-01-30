package org.yesod.ktisis.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;
import org.yesod.ktisis.base.WhitespaceHelper;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

public class ClassBase
{
  public static List<Map<?, ?>> getSuperFields(VariableResolver ctx)
  {
    Builder<Map<?, ?>> builder = ImmutableList.builder();
    Map<?, ?> superdef = ctx.getAs("supertype", Map.class).orElse(null);
    if (superdef != null)
    {
      List<?> fields = (List<?>) superdef.get("fields");
      fields.stream().map(Map.class::cast).forEach(builder::add);
    }
    return builder.build();
  }

  public static List<Map<?, ?>> getFields(VariableResolver ctx)
  {
    Builder<Map<?, ?>> builder = ImmutableList.builder();
    List<?> fields = ctx.getAs("fields", List.class).orElseThrow(RuntimeException::new);
    fields.stream().map(Map.class::cast).forEach(builder::add);
    return builder.build();
  }

  public static List<Map<?, ?>> getFieldsAndSuperFields(VariableResolver ctx)
  {
    Builder<Map<?, ?>> builder = ImmutableList.builder();
    return builder.addAll(getSuperFields(ctx)).addAll(getFields(ctx)).build();
  }

  public static boolean isOptional(Map<?, ?> field)
  {
    return isOptional(field::get);
  }

  public static boolean isOptional(VariableResolver fieldCtx)
  {
    return fieldCtx.getAs("optional", Boolean.class).orElse(null) == Boolean.TRUE;
  }

  @ExtensionPoint("class")
  public String writeClass(VariableResolver variableResolver) throws IOException
  {
    try (InputStream template = TemplateProcessor.getResource("templates/ktisis/java/ClassBase.template", ClassBase.class))
    {
      ImmutableMap<?, ?> subParts = ImmutableMap.builder()
                                                .put("ctor_args", ctorArgs(variableResolver))
                                                .put("super_type_opt", superTypeOpt(variableResolver))
                                                .put("super_ctor_opt", superCtorOpt(variableResolver))
                                                .build();
      VariableResolver subResolver = VariableResolver.merge(subParts::get, variableResolver);
      return TemplateProcessor.processTemplate(template, subResolver);
    }
  }

  private String ctorArgs(VariableResolver ctx)
  {
    List<String> builder = new ArrayList<>();
    for (Object field : getFieldsAndSuperFields(ctx))
    {
      Map<?, ?> fieldAttrs = (Map<?, ?>) field;
      builder.add(TemplateProcessor.processTemplate("@{ctor_arg}${type} ${name}", VariableResolver.merge(fieldAttrs::get, ctx)));
    }
    int column = WhitespaceHelper.length(ctx.apply("name"), ctx.apply("scope")) + 4;
    return WhitespaceHelper.joinWithWrapIfNecessary(builder, ", ", column, 120);
  }

  private String superTypeOpt(VariableResolver variableResolver)
  {
    Map<?, ?> superdef = variableResolver.getAs("supertype", Map.class).orElse(null);
    if (superdef == null)
    {
      return "";
    }
    return String.format(" extends %s", superdef.get("name"));
  }

  @ExtensionPoint("super_ctor_opt")
  public String superCtorOpt(VariableResolver ctx)
  {
    List<Map<?, ?>> superFields = getSuperFields(ctx);
    if (superFields.isEmpty())
    {
      return "";
    }
    String args = superFields.stream().map((f) -> (String) f.get("name")).collect(Collectors.joining(", "));
    return String.format("    super(%s);", args);
  }

  @ExtensionPoint("class_ctor_body")
  public String ctorBody(VariableResolver variableResolver)
  {
    List<?> fields = variableResolver.getAs("fields", List.class).orElse(null);
    Preconditions.checkState(fields != null, "Trying to build a class without fields?");
    List<String> lines = new ArrayList<>();
    for (Object field : fields)
    {
      Map<?, ?> fieldAttrs = Map.class.cast(field);
      VariableResolver merge = VariableResolver.merge(fieldAttrs::get, variableResolver);
      if (!isOptional(fieldAttrs))
      {
        Imports.addImport(Preconditions.class);
        lines.add(TemplateProcessor.processTemplate("Preconditions.checkNotNull(${name}, \"${name} (${type}) is a required field\");", merge));
      }
      lines.add(TemplateProcessor.processTemplate("this.${name} = ${name};", merge));
    }
    return "    " + Joiner.on("\n    ").join(lines);
  }

  @ExtensionPoint("class_fields")
  public String writeClassFields(VariableResolver variableResolver)
  {
    Collection<String> lines = new ArrayList<>();
    List<?> fields = variableResolver.getAs("fields", List.class).orElse(null);
    Preconditions.checkState(fields != null, "Trying to build a class without fields?");
    for (Object field : fields)
    {
      Map<?, ?> fieldAttrs = Map.class.cast(field);
      lines.add(TemplateProcessor.processTemplate("  @{field}private final ${type} ${name};",
                                                  VariableResolver.merge(fieldAttrs::get, variableResolver)));
    }
    return Joiner.on("\n").join(lines);
  }

  public static String upcase(String name)
  {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  public static String getterName(String name, String type)
  {
    return String.format("%s%s", type.equals("Boolean") ? "is" : "get", upcase(name));
  }

  @ExtensionPoint("getters")
  public String writeClassGetters(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    List<?> fields = variableResolver.getAs("fields", List.class).orElse(null);
    Preconditions.checkState(fields != null, "Trying to build a class without fields?");
    for (Object field : fields)
    {
      Map<?, ?> fieldAttrs = Map.class.cast(field);
      String name = fieldAttrs.get("name").toString();
      String type = fieldAttrs.get("type").toString();
      boolean isOptional = isOptional(fieldAttrs);
      if (isOptional)
      {
        Imports.addImport(Optional.class);
      }
      String getterName = getterName(name, type);
      String returnType = isOptional ? String.format("Optional<%s>", type) : type;
      String returnStr = isOptional ? String.format("Optional.fromNullable(%s)", name) : name;
      Map<?, ?> overrides = ImmutableMap.builder()
                                        .put("getter_name", getterName)
                                        .put("type", returnType)
                                        .put("return", returnStr)
                                        .build();
      try (InputStream template = TemplateProcessor.getResource("templates/ktisis/java/Getter.template", ClassBase.class))
      {
        lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(overrides::get, fieldAttrs::get, variableResolver)));
      }
    }
    return Joiner.on("\n").join(lines);
  }

  @ExtensionPoint("end_class")
  public String writeHashCode(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    List<?> fields = variableResolver.getAs("fields", List.class).orElse(null);
    Preconditions.checkState(fields != null, "Trying to build a class without fields?");
    List<Map<?, ?>> superFields = getSuperFields(variableResolver);
    if (!superFields.isEmpty())
    {
      lines.add("super.hashCode()");
    }
    for (Object field : fields)
    {
      Map<?, ?> fieldAttrs = (Map<?, ?>) field;
      if (fieldAttrs.get("hashcode") != Boolean.FALSE)
      {
        lines.add(fieldAttrs.get("name").toString());
      }
    }
    String hashCodeBody = WhitespaceHelper.joinWithWrapIfNecessary(lines, ", ", 13, 120);
    VariableResolver inner = (s) -> hashCodeBody;
    try (InputStream is = TemplateProcessor.getResource("templates/ktisis/java/HashCode.template", getClass()))
    {
      return TemplateProcessor.processTemplate(is, inner);
    }
  }

  @ExtensionPoint("end_class")
  public String writeEquals(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    List<?> fields = variableResolver.getAs("fields", List.class).orElse(null);
    Preconditions.checkState(fields != null, "Trying to build a class without fields?");
    List<Map<?, ?>> superFields = getSuperFields(variableResolver);
    if (!superFields.isEmpty())
    {
      lines.add("super.equals(that)");
    }
    Imports.addImport(Objects.class);
    for (Object field : fields)
    {
      Map<?, ?> fieldAttrs = (Map<?, ?>) field;
      if (fieldAttrs.get("equals") != Boolean.FALSE)
      {
        String name = fieldAttrs.get("name").toString();
        lines.add(String.format("Objects.equal(this.%s, that.%s)", name, name));
      }
    }
    String equalsBody = WhitespaceHelper.joinWithWrapIfNecessary(lines, "", " && ", 10, 120);
    VariableResolver inner = (s) -> equalsBody;

    try (InputStream is = TemplateProcessor.getResource("templates/ktisis/java/Equals.template", getClass()))
    {
      return TemplateProcessor.processTemplate(is, VariableResolver.merge(variableResolver, inner));
    }
  }

}
