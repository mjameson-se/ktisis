package org.yesod.ktisis.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.reflection.ClassStream;

import com.google.common.base.Preconditions;

public class FunctionsPlugin implements TemplatePlugin
{
  private static Pattern matcher = Pattern.compile("#\\{(\\w+)\\(([\\w\\., ]*)\\)\\}");
  // #{toUpper(${name})}
  // #{substr(${name}, 2)}
  private Map<String, FunctionProvider> functions = new HashMap<>();

  /**
   * Annotation to facilitate automatic loading of functions
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface FunctionRegistration
  {
    String value();
  }

  /**
   * Functional interface for registering some function for execution from a template
   */
  public interface FunctionProvider extends BiFunction<String[], VariableResolver, String>
  {
  }

  /** 
   * 
   */
  public FunctionsPlugin()
  {
    registerFunction("upcase", this::upcase);
    registerFunction("now", this::now);
    registerFunction("toUpper", this::toUpper);
    registerFunction("substr", this::substring);
  }

  public String upcase(String[] args, VariableResolver ctx)
  {
    String name = args[0];
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  public void registerFunction(String name, FunctionProvider fn)
  {
    functions.put(name, fn);
  }

  public String toUpper(String[] args, VariableResolver ctx)
  {
    String arg = TemplateProcessor.processTemplate(args[0], ctx);
    return arg.toUpperCase();
  }

  public String now(String[] args, VariableResolver ctx)
  {
    return LocalDateTime.now().toString();
  }

  public String substring(String[] args, VariableResolver ctx)
  {
    if (args.length == 2)
    {
      return args[0].substring(Integer.parseInt(args[1]));
    }
    else if (args.length == 3)
    {
      return args[0].substring(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
    }
    throw new IllegalArgumentException("Substring takes 2 or 3 args");
  }

  @Override
  public String process(Matcher match, VariableResolver context)
  {
    String fn = match.group(1);
    String args = TemplateProcessor.processTemplate(match.group(2), context);
    Preconditions.checkArgument(functions.containsKey(fn), "No such function:", fn);
    return functions.get(fn).apply(args.split(", "), context);
  }

  @Override
  public Pattern pattern()
  {
    return matcher;
  }

  @Override
  public void load(ClassStream classStream)
  {
    classStream.mapMethods()
               .withAnnotation(FunctionRegistration.class)
               .withReturnType(String.class)
               .withParameterTypes(String[].class, VariableResolver.class)
               .<String, FunctionProvider> asInterface(b -> b::invoke)
               .forEach(i -> registerFunction(i.getMethod().getAnnotation(FunctionRegistration.class).value(), i.getInterface()));
  }
}
