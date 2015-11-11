package org.yesod.ktisis;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;

public abstract class TemplateProcessor
{
  private static final List<TemplatePlugin> plugins = new ArrayList<>();

  public static void registerPlugin(TemplatePlugin plugin)
  {
    plugins.add(plugin);
  }

  public static void reset()
  {
    plugins.clear();
  }

  public static String processTemplate(String templateSource, VariableResolver variableResolver)
  {
    InputStream is = new ByteArrayInputStream(templateSource.getBytes(StandardCharsets.UTF_8));
    return processTemplate(is, variableResolver);
  }

  public static String processTemplate(InputStream templateSource,
                                       VariableResolver variableResolver)
  {
    Collection<String> builder = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(templateSource, StandardCharsets.UTF_8));
    reader.lines().forEach((line) ->
    {
      String intermediate = line;
      for (TemplatePlugin entry : plugins)
      {
        if (entry.pattern().matcher(intermediate).find())
        {
          intermediate = entry.process(intermediate, variableResolver);
        }
        if (intermediate == null)
        {
          // Plugins are permitted to consume lines and return nothing
          // This will often occur when there are no extensions loaded for an extension point.
          break;
        }
      }
      builder.add(intermediate);
    });
    return Joiner.on("\n").skipNulls().join(builder);
  }

  public static InputStream getResource(String path, Class<?> ctx) throws IOException
  {
    ClassLoader cl = ctx.getClassLoader();
    InputStream is = cl.getResourceAsStream(path);
    if (is != null)
    {
      return is;
    }
    Path resourcesDir = Paths.get("resources/main");
    if (resourcesDir.toFile().exists())
    {
      return Files.newInputStream(resourcesDir.resolve(path), StandardOpenOption.READ);
    }
    return null;
  }
}