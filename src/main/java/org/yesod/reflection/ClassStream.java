package org.yesod.reflection;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

public class ClassStream
{
  private Set<Class<?>> set;

  public ClassStream(Class<?> clazz)
  {
    set = ImmutableSet.of(clazz);
  }

  public ClassStream(Stream<Class<?>> stream)
  {
    this.set = stream.collect(Collectors.toSet());
  }

  public ClassStream withSuperclass(Class<?> superclass)
  {
    return new ClassStream(set.stream().filter((c) -> superclass.isAssignableFrom(c)));
  }

  public ClassStream withAnnotation(Class<? extends Annotation> annotation)
  {
    return new ClassStream(set.stream().filter((c) -> c.isAnnotationPresent(annotation)));
  }

  public MethodStream mapMethods()
  {
    return new MethodStream(set.stream().sorted((c1, c2) -> c1.getName().compareTo(c2.getName())).flatMap((c) -> Arrays.stream(c.getMethods())));
  }

  public Stream<Class<?>> stream()
  {
    return set.stream();
  }
}
