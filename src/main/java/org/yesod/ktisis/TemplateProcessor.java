package org.yesod.ktisis;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import org.yesod.reflection.ClassStream;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;

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

  public static void loadAll(ClassStream cs)
  {
    plugins.forEach(t -> t.load(cs));
  }

  public static String processTemplate(InputStream templateSource,
                                       VariableResolver variableResolver)
  {
    try
    {
      String templateString = CharStreams.toString(new InputStreamReader(templateSource, StandardCharsets.UTF_8));
      return processTemplate(templateString, variableResolver);
    }
    catch (IOException ex)
    {
      throw Throwables.propagate(ex);
    }
  }

  public static String processTemplate(String templateSource, VariableResolver variableResolver)
  {
    String intermediate = templateSource;
    for (TemplatePlugin plugin : plugins)
    {
      StringBuffer buf = new StringBuffer();
      Matcher mch = plugin.pattern().matcher(intermediate);
      while (mch.find())
      {
        Optional<String> replace = Optional.ofNullable(plugin.process(mch, variableResolver));
        mch.appendReplacement(buf, Matcher.quoteReplacement(replace.orElse("")));
      }
      mch.appendTail(buf);
      intermediate = buf.toString();
    }
    return intermediate;
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