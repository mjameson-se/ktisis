package org.yesod.ktisis.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;

import com.google.common.base.Joiner;

public class BasicBuilder
{
  // TODO: copy builder
  // TODO: collectors

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

  private String ctorArgs(VariableResolver variableResolver)
  {
    List<String> builder = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(variableResolver))
    {
      builder.add(fieldAttrs.get("name").toString());
    }
    return Joiner.on(", ").join(builder);
  }

  @ExtensionPoint("builder_fields")
  public String builderFields(VariableResolver variableResolver)
  {
    Collection<String> lines = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(variableResolver))
    {
      String format = "    private ${type} ${name};";
      if (fieldAttrs.containsKey("default"))
      {
        format = String.format("    private ${type} ${name} = %s;", fieldAttrs.get("default"));
      }
      lines.add(TemplateProcessor.processTemplate(format, VariableResolver.merge(fieldAttrs::get, variableResolver)));
    }
    return Joiner.on("\n").join(lines);
  }

  @ExtensionPoint("builder_setters")
  public String builderSetters(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    for (Map<?, ?> fieldAttrs : ClassBase.getFieldsAndSuperFields(variableResolver))
    {
      try (InputStream template = TemplateProcessor.getResource("templates/ktisis/java/Setter.template", BasicBuilder.class))
      {
        lines.add(TemplateProcessor.processTemplate(template, VariableResolver.merge(fieldAttrs::get, variableResolver)));
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
      boolean optional = fieldAttrs.get("optional") == Boolean.TRUE;
      if (optional)
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
