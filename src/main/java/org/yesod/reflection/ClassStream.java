package org.yesod.reflection;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;

public class ClassStream
{
  private Stream<Class<?>> stream;

  public ClassStream(Stream<Class<?>> stream)
  {
    this.stream = stream;
  }

  public ClassStream withSuperclass(Class<?> superclass)
  {
    return new ClassStream(stream.filter((c) -> superclass.isAssignableFrom(c)));
  }

  public ClassStream withAnnotation(Class<? extends Annotation> annotation)
  {
    return new ClassStream(stream.filter((c) -> c.isAnnotationPresent(annotation)));
  }

  public MethodStream mapMethods()
  {
    return new MethodStream(stream.flatMap((c) -> Arrays.stream(c.getMethods())));
  }
}
