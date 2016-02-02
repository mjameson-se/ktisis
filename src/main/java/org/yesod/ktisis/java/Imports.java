package org.yesod.ktisis.java;

import java.util.List;
import java.util.stream.Collectors;

import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;
import org.yesod.ktisis.base.FunctionsPlugin.FunctionRegistration;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Handles writing the imports section of the file.
 */
public class Imports
{
  private static ListMultimap<Integer, String> imports = ArrayListMultimap.create();

  /**
   * Add an import to the imports section
   */
  public static void addImport(Class<?> clazz)
  {
    addImport(clazz.getName());
  }

  /**
   * Add an import to the imports section
   */
  public static void addImport(String imprt)
  {
    imports.put(sortPriority(imprt), imprt);
  }

  private static int sortPriority(String clazz)
  {
    String[] parts = clazz.split("\\.");
    switch (parts[0])
    {
      case "java":
        return 0;
      case "javax":
        return 1;
      case "org":
        return 2;
      case "com":
        return 3;
      default:
        return 4;
    }
  }

  @FunctionRegistration("imports")
  public String writeImports(String[] args, VariableResolver ctx)
  {
    try
    {
      List<String> sections = imports.asMap()
                                     .values()
                                     .stream()
                                     .map(c -> Joiner.on(System.lineSeparator())
                                                     .join(c.stream()
                                                            .distinct()
                                                            .map(i -> String.format("import %s;", i))
                                                            .sorted()
                                                            .collect(Collectors.toList())))
                                     .collect(Collectors.toList());
      return Joiner.on(System.lineSeparator() + System.lineSeparator()).join(sections);
    }
    finally
    {
      imports.clear();
    }
  }

  @ExtensionPoint("imports")
  public String writeImportsFn(VariableResolver variableResolver)
  {
    List<?> importsList = variableResolver.getAs("imports", List.class).orElse(null);
    if (importsList != null)
    {
      importsList.forEach(i -> addImport(i.toString()));
    }
    return "#{imports()}";
  }
}
