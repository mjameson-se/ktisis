package org.yesod.ktisis.java;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.reflection.ClassStream;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class AnnotationPlugin implements TemplatePlugin
{
  private static Pattern annotationMatcher = Pattern.compile("@\\{([\\w]*)\\}");
  private Multimap<String, Function<VariableResolver, String>> annotations = ArrayListMultimap.create();

  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationRegistration
  {
    String value();
  }

  @Override
  public Pattern pattern()
  {
    return annotationMatcher;
  }

  @Override
  public String process(Matcher matcher, VariableResolver context)
  {
    ArrayList<String> strs = new ArrayList<>();
    for (Function<VariableResolver, String> annotation : annotations.get(matcher.group(1)))
    {
      String str = annotation.apply(context);
      if (str != null)
      {
        strs.add(str);
      }
    }
    return Joiner.on("").skipNulls().join(strs);
  }

  public void registerAnnotation(String position, String template)
  {
    registerAnnotation(position, (ctx) -> TemplateProcessor.processTemplate(template, ctx));
  }

  public void registerAnnotation(String position, Function<VariableResolver, String> template)
  {
    annotations.put(position, template);
  }

  @Override
  public void load(ClassStream cs)
  {
    cs.mapMethods()
      .withAnnotation(AnnotationRegistration.class)
      .withReturnType(String.class)
      .withParameterTypes(VariableResolver.class)
      .publicOnly()
      .sorted()
      .<String, Function<VariableResolver, String>> asInterface(b -> b::invoke)
      .forEach(w ->
      {
        registerAnnotation(w.getMethod().getAnnotation(AnnotationRegistration.class).value(), w.getInterface());
      });
  }
}
