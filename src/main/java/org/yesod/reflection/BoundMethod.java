package org.yesod.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Throwables;

public class BoundMethod<T>
{
  private final Method invoker;
  private final Object instance;

  public BoundMethod(Method m, Object instance)
  {
    this.invoker = m;
    this.instance = instance;
  }

  @SuppressWarnings("unchecked")
  public T invoke(Object... objects)
  {
    try
    {
      return (T) invoker.invoke(instance, objects);
    }
    catch (InvocationTargetException | IllegalAccessException e)
    {
      throw Throwables.propagate(e);
    }
  }

  static <T> BoundMethod<T> of(Method m, Object instance)
  {
    return new BoundMethod<>(m, instance);
  }

  public Object getInstance()
  {
    return instance;
  }

  public Method getMethod()
  {
    return invoker;
  }
}
