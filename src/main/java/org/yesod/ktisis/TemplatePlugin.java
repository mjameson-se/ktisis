package org.yesod.ktisis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.reflection.ClassStream;

/**
 * Plugin used to interpret syntax within templates.
 * Each plugin is associated with a pattern, given by {@link #pattern()}. When the pattern is matched within a template,
 * {@link #process(Matcher, VariableResolver)} will be invoked with the matcher that matched it and the current resolution context. 
 * Plugins may also optionally support bulk loading of configuration from annotated classes, by implementing the {@link #load(ClassStream)} function.
 */
public interface TemplatePlugin
{
  String process(Matcher match, VariableResolver context);

  Pattern pattern();

  default void load(ClassStream classStream)
  {
  }
}
