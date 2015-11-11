package org.yesod.ktisis.base;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.VariableResolver;

import com.google.common.base.Preconditions;

public class SubstitutionPlugin implements TemplatePlugin
{
  private static final Pattern variablePattern = Pattern.compile("\\$\\{([\\w\\.]+)\\}");

  @Override
  public String process(String input, VariableResolver variableLookup)
  {
    Matcher matcher = variablePattern.matcher(input);
    StringBuffer buf = new StringBuffer();
    while (matcher.find())
    {
      Object var = variableLookup.apply(matcher.group(1));
      Preconditions.checkState(var != null, String.format("Variable %s was not supplied", matcher.group(1)));
      matcher.appendReplacement(buf, Matcher.quoteReplacement(var.toString()));
    }
    matcher.appendTail(buf);
    return buf.toString();
  }

  @Override
  public Pattern pattern()
  {
    return variablePattern;
  }

}
