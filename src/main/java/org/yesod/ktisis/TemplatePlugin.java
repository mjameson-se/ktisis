package org.yesod.ktisis;

import java.util.regex.Pattern;

public interface TemplatePlugin
{
  String process(String line, VariableResolver context);

  Pattern pattern();
}
