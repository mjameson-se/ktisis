package org.yesod.reflection;

import java.lang.reflect.Method;

public class InterfaceWrapper<T>
{
  private final T interfaceInstance;
  private final Class<?> instanceClass;
  private final Method method;

  public InterfaceWrapper(T interfaceInstance, Method method)
  {
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
}
