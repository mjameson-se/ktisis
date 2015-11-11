package org.yesod.ktisis.java;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class AnnotationPlugin implements TemplatePlugin
{
  private static Pattern annotationMatcher = Pattern.compile("@\\{([\\w]*)\\}");
  private Multimap<String, Function<VariableResolver, String>> annotations = ArrayListMultimap.create();

  @Override
  public Pattern pattern()
  {
    return annotationMatcher;
  }

  @Override
  public String process(String line, VariableResolver context)
  {
    Matcher matcher = annotationMatcher.matcher(line);
    StringBuffer buf = new StringBuffer();
    while (matcher.find())
    {
      ArrayList<String> strs = new ArrayList<>();
      for (Function<VariableResolver, String> annotation : annotations.get(matcher.group(1)))
      {
        strs.add(annotation.apply(context));
      }
      matcher.appendReplacement(buf, Matcher.quoteReplacement(Joiner.on(" ").skipNulls().join(strs)));
    }
    matcher.appendTail(buf);
    return buf.toString();
  }

  public void registerAnnotation(String position, String template)
  {
    registerAnnotation(position, (ctx) -> TemplateProcessor.processTemplate(template, ctx));
  }

  public void registerAnnotation(String position, Function<VariableResolver, String> template)
  {
    annotations.put(position, template);
  }
}
