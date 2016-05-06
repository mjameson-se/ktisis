package org.yesod.ktisis.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;
import org.yesod.ktisis.base.FeatureTags.Feature;
import org.yesod.reflection.ClassStream;
import org.yesod.reflection.ClasspathSearch;
import org.yesod.reflection.InterfaceWrapper;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class ExtensionRegistry implements TemplatePlugin
{
  private static final Pattern extensionMatcher = Pattern.compile("^#! \\{(\\S*)\\}\\R{0,1}", Pattern.MULTILINE);

  private ListMultimap<String, InterfaceWrapper<ExtensionMethod>> extensionPoints = ArrayListMultimap.create();

  @Deprecated
  public void loadPackage(String packageName) throws IOException
  {
    load(new ClasspathSearch().includePackage(packageName).classStream());
  }

  @Override
  public void load(ClassStream cs)
  {
    cs.mapMethods()
      .publicOnly()
      .withAnnotation(ExtensionPoint.class)
      .withReturnType(String.class)
      .withParameterTypes(VariableResolver.class)
      .sorted()
      .<String, ExtensionMethod> asInterface((b) -> b::invoke)
      .forEach((iw) ->
      {
        extensionPoints.put(iw.getMethod().getAnnotation(ExtensionPoint.class).value(), iw);
      });
  }

  @Override
  public String process(Matcher matcher, VariableResolver variableLookup)
  {
    Collection<String> builder = new ArrayList<>();
    String extension = matcher.group(1);
    for (InterfaceWrapper<ExtensionMethod> extMethod : extensionPoints.get(extension))
    {
      Optional<Feature> featureOpt = extMethod.getAnnotation(Feature.class);
      if (featureOpt.isPresent() && !FeatureTags.hasTag(featureOpt.get().value(), variableLookup))
      {
        continue;
      }

      String output = extMethod.getInterface().process(variableLookup);
      if (output != null)
      {
        builder.add(output);
      }
    }
    return builder.isEmpty() ? null : Joiner.on('\n').join(builder) + '\n';
  }

  @Override
  public Pattern pattern()
  {
    return extensionMatcher;
  }
}
