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
import org.yesod.ktisis.base.WhitespaceHelper;
import org.yesod.ktisis.base.WhitespaceHelperConfig;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class ToStringExtension
{
  @ExtensionPoint("end_class")
  public String writeToString(VariableResolver variableResolver) throws IOException
  {
    Collection<String> lines = new ArrayList<>();
    List<?> fields = variableResolver.getAs("fields", List.class).orElse(null);
    Preconditions.checkState(fields != null, "Trying to build a class without fields?");
    Imports.addImport(MoreObjects.class);

    for (Object field : fields)
    {
      Map<?, ?> fieldAttrs = (Map<?, ?>) field;
      if (fieldAttrs.get("toString") != Boolean.FALSE)
      {
        String name = fieldAttrs.get("name").toString();
        lines.add(String.format(".add(\"%s\", %s)", name, name));
      }
    }
    List<Map<?, ?>> superFields = ClassBase.getSuperFields(variableResolver);
    if (!superFields.isEmpty())
    {
      lines.add(".add(\"super\", super.toString())");
    }
    lines.add(".omitNullValues()");
    lines.add(".toString()");

    WhitespaceHelperConfig cfg = new WhitespaceHelperConfig.Builder().wrappedIndent(22)
                                                                     .lineLength(80)
                                                                     .extraNewlinesIfWrapped(true)
                                                                     .build();
    VariableResolver inner = (s) -> WhitespaceHelper.join(cfg, lines);
    try (InputStream is = TemplateProcessor.getResource("templates/ktisis/java/ToString.template", getClass()))
    {
      return TemplateProcessor.processTemplate(is, VariableResolver.merge(variableResolver, inner));
    }
  }
}
