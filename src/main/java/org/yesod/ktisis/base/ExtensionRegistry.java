package org.yesod.ktisis.base;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;
import org.yesod.reflection.ClasspathSearch;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;

public class ExtensionRegistry implements TemplatePlugin
{
  private static final Class<?>[] EXTENSION_PARAMETER_TYPES = ExtensionMethod.class.getDeclaredMethods()[0].getParameterTypes();
  private static final Pattern extensionMatcher = Pattern.compile("#! \\{(\\S*)\\}\\s*(.*)");

  private ListMultimap<String, ExtensionMethod> extensionPoints = ArrayListMultimap.create();

  private static class BoundMethod
  {
    Method invoker;
    Object instance;

    public BoundMethod(Method m, Object instance)
    {
      this.invoker = m;
      this.instance = instance;
    }

    String invoke(Object... objects)
    {
      try
      {
        return (String) invoker.invoke(instance, objects);
      }
      catch (InvocationTargetException | IllegalAccessException e)
      {
        throw Throwables.propagate(e);
      }
    }

    static BoundMethod of(Method m, Object instance)
    {
      return new BoundMethod(m, instance);
    }
  }

  private static Object getExtension(String extensionName)
  {
    try
    {
      Class<?> forName = Class.forName(extensionName);
      return forName.newInstance();
    }
    catch (ClassNotFoundException ex)
    {
      throw new IllegalStateException(String.format("%s is not available in the current classpath", extensionName), ex);
    }
    catch (InstantiationException | IllegalAccessException ex)
    {
      throw new IllegalStateException(String.format("%s does not have a public no-arg constructor", extensionName), ex);
    }
  }

  public void loadExtension(String extensionName)
  {
    Object ext = getExtension(extensionName);
    for (Method method : ext.getClass().getMethods())
    {
      if (Modifier.isPublic(method.getModifiers()) && method.isAnnotationPresent(ExtensionPoint.class))
      {
        Preconditions.checkState(Arrays.deepEquals(method.getParameterTypes(), EXTENSION_PARAMETER_TYPES),
                                 String.format("Extension method %s does not have the correct signature", method));
        BoundMethod bound = BoundMethod.of(method, ext);
        extensionPoints.put(method.getAnnotation(ExtensionPoint.class).value(), bound::invoke);
      }
    }
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

  public List<ExtensionMethod> getExtensionMethods(String extensionPoint, Optional<Range<Integer>> cardinalityOpt)
  {
    List<ExtensionMethod> exts = extensionPoints.get(extensionPoint);
    Preconditions.checkState(!cardinalityOpt.isPresent() || cardinalityOpt.get().contains(exts.size()));
    return exts;
  }

  @Override
  public String process(String input, VariableResolver variableLookup)
  {
    Matcher matcher = extensionMatcher.matcher(input);
    if (matcher.find())
    {
      Collection<String> builder = new ArrayList<>();
      String extension = matcher.group(1);
      for (ExtensionMethod extMethod : getExtensionMethods(extension, Optional.empty()))
      {
        builder.add(extMethod.process(variableLookup));
      }
      return builder.isEmpty() ? null : Joiner.on("\n").join(builder);
    }
    return input;
  }

  @Override
  public Pattern pattern()
  {
    return extensionMatcher;
  }
}
