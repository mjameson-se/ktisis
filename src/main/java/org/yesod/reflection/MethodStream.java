package org.yesod.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class MethodStream
{
  private Stream<Method> methods;

  public MethodStream(Stream<Method> methods)
  {
    this.methods = methods;
  }

  private Object newInstance(Class<?> c)
  {
    try
    {
      return c.newInstance();
    }
    catch (InstantiationException | IllegalAccessException e)
    {
      throw Throwables.propagate(e);
    }
  }

  public MethodStream withAnnotation(Class<? extends Annotation> a)
  {
    return new MethodStream(methods.filter((m) -> m.isAnnotationPresent(a)));
  }

  public MethodStream withParameterTypes(Class<?>... classes)
  {
    return new MethodStream(methods.filter((m) -> Arrays.deepEquals(m.getParameterTypes(), classes)));
  }

  public MethodStream withReturnType(Class<?> clazz)
  {
    return new MethodStream(methods.filter((m) -> clazz == m.getReturnType()));
  }

  public MethodStream withFilter(Predicate<Method> filter)
  {
    return new MethodStream(methods.filter(filter));
  }

  public <X, Y> Stream<InterfaceWrapper<Y>> asInterface(Function<BoundMethod<X>, Y> transform)
  {
    LoadingCache<Class<?>, Object> instanceCache = CacheBuilder.newBuilder().build(CacheLoader.from(this::newInstance));
    Function<Method, BoundMethod<X>> i = (m) ->
    {
      try
      {
        return BoundMethod.<X> of(m, instanceCache.get(m.getDeclaringClass()));
      }
      catch (ExecutionException e)
      {
        throw Throwables.propagate(e);
      }
    };
    return methods.map(i).map((b) -> new InterfaceWrapper<Y>(transform.apply(b), b.getMethod()));
  }
}
