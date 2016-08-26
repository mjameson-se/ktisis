package org.yesod.ktisis.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.yesod.ktisis.VariableResolver;

/**
 * Annotate extension methods with the value used to invoke this extension from
 * a template. The annotated method should have the same signature as
 * {@link #process(VariableResolver)}
 */
public interface ExtensionMethod
{
  String process(VariableResolver variableResolver);

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface ExtensionPoint
  {
    String[] value();
  }
}
