package org.yesod.ktisis.java;

import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;

public class Imports
{
  @ExtensionPoint("imports")
  public String writeImports(VariableResolver variableResolver)
  {
    return "";
    // List<?> imports = variableResolver.getAs("imports",
    // List.class).orElse(null);
    // Preconditions.checkState(imports != null, "Why did you load this
    // extension if you weren't going to use it?");
    // return Joiner.on("\n").join(imports);
  }
}
