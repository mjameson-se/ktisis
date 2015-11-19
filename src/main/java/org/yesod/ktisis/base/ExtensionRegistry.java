package org.yesod.ktisis.base;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;
import org.yesod.reflection.ClasspathSearch;
import org.yesod.reflection.MethodStream;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class ExtensionRegistry implements TemplatePlugin
{
  private static final Pattern extensionMatcher = Pattern.compile("^#! \\{(\\S*)\\}\\R{0,1}", Pattern.MULTILINE);

  private ListMultimap<String, ExtensionMethod> extensionPoints = ArrayListMultimap.create();

  private static Class<?> getExtension(String extensionName)
  {
    try
    {
      return Class.forName(extensionName);
    }
    catch (ClassNotFoundException ex)
    {
      throw new IllegalStateException(String.format("%s is not available in the current classpath", extensionName), ex);
    }
  }

  public void loadExtension(String extensionName)
  {
    Class<?> ext = getExtension(extensionName);
    new MethodStream(Arrays.stream(ext.getMethods()))
        .withFilter((m) -> Modifier.isPublic(m.getModifiers()))
        .withAnnotation(ExtensionPoint.class)
        .withParameterTypes(VariableResolver.class)
        .<String, ExtensionMethod> asInterface((b) -> b::invoke)
        .forEach(iw -> extensionPoints.put(iw.getMethod().getAnnotation(ExtensionPoint.class).value(), iw.getInterface()));
  }

  public void loadPackage(String packageName) throws IOException
  {
    new ClasspathSearch()
        .includePackage(packageName)
        .classStream()
        .mapMethods()
        .withFilter((m) -> Modifier.isPublic(m.getModifiers()))
        .withAnnotation(ExtensionPoint.class)
        .withReturnType(String.class)
        .withParameterTypes(VariableResolver.class)
        .<String, ExtensionMethod> asInterface((b) -> b::invoke)
        .forEach((iw) ->
        {
          extensionPoints.put(iw.getMethod().getAnnotation(ExtensionPoint.class).value(), iw.getInterface());
        });
  }

  @Override
  public String process(Matcher matcher, VariableResolver variableLookup)
  {
    Collection<String> builder = new ArrayList<>();
    String extension = matcher.group(1);
    for (ExtensionMethod extMethod : extensionPoints.get(extension))
    {
      builder.add(extMethod.process(variableLookup));
    }
    return builder.isEmpty() ? null : Joiner.on(System.lineSeparator()).join(builder) + System.lineSeparator();
  }

  @Override
  public Pattern pattern()
  {
    return extensionMatcher;
  }
}
