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
import com.google.common.base.Preconditions;

public class ToStringExtension
{
  @ExtensionPoint("end_class")
  public String writeToString(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    List<?> fields = variableResolver.getAs("fields", List.class).orElse(null);
    Preconditions.checkState(fields != null, "Trying to build a class without fields?");
    for (Object field : fields)
    {
      Map<?, ?> fieldAttrs = (Map<?, ?>) field;
      if (fieldAttrs.get("toString") != Boolean.FALSE)
      {
        String name = fieldAttrs.get("name").toString();
        lines.add(String.format(".add(\"%s\", %s)", name, name));
      }
    }
    VariableResolver inner = (s) ->
    {
      int len = lines.stream().mapToInt(String::length).sum();
      if (len > 120)
      {
        String whitespace = "\n             ";
        return whitespace + Joiner.on(whitespace).join(lines) + whitespace;
      }
      else
      {
        return Joiner.on("").join(lines);
      }
    };
    try (InputStream is = TemplateProcessor.getResource("templates/ktisis/java/ToString.template", getClass()))
    {
      return TemplateProcessor.processTemplate(is, VariableResolver.merge(variableResolver, inner));
    }
  }
}
