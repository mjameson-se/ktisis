package org.yesod.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

public class InterfaceWrapper<T>
{
  private final T interfaceInstance;
  private final Class<?> instanceClass;
  private final Method method;
  private Object original;

  public InterfaceWrapper(Object original, T interfaceInstance, Method method)
  {
    this.original = original;
    this.interfaceInstance = interfaceInstance;
    this.method = method;
    this.instanceClass = method.getDeclaringClass();
  }

  public T getInterface()
  {
    return interfaceInstance;
  }

  public Class<?> getInstanceClass()
  {
    return instanceClass;
  }

  public Method getMethod()
  {
    return method;
  }

  public <A extends Annotation> Optional<A> getAnnotation(Class<A> a)
  {
    return Optional.ofNullable(method.isAnnotationPresent(a) ? method.getAnnotation(a) : original.getClass().getAnnotation(a));
  }
}
