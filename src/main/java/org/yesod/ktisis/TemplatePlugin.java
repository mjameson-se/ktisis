package org.yesod.ktisis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface TemplatePlugin
{
  String process(Matcher match, VariableResolver context);

  Pattern pattern();
}
