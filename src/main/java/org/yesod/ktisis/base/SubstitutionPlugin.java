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
  public String process(Matcher matcher, VariableResolver variableLookup)
  {
    Object var = variableLookup.apply(matcher.group(1));
    Preconditions.checkState(var != null, String.format("Variable %s was not supplied", matcher.group(1)));
    return var.toString();
  }

  @Override
  public Pattern pattern()
  {
    return variablePattern;
  }

}
