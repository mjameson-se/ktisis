package org.yesod.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComparisonChain;

public class MethodStream
{
  private static LoadingCache<Class<?>, Object> instanceCache = CacheBuilder.newBuilder().build(CacheLoader.from(MethodStream::newInstance));
  private Stream<Method> methods;

  public MethodStream(Stream<Method> methods)
  {
    this.methods = methods;
  }

  private static Object newInstance(Class<?> c)
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
    return withFilter(m -> m.isAnnotationPresent(a));
  }

  public MethodStream withParameterTypes(Class<?>... classes)
  {
    return withFilter(m -> Arrays.deepEquals(m.getParameterTypes(), classes));
  }

  public MethodStream publicOnly()
  {
    return withFilter(m -> Modifier.isPublic(m.getModifiers()));
  }

  public MethodStream withReturnType(Class<?> clazz)
  {
    return withFilter(m -> clazz == m.getReturnType());
  }

  public MethodStream withFilter(Predicate<Method> filter)
  {
    return new MethodStream(methods.filter(filter));
  }

  public MethodStream sorted()
  {
    return new MethodStream(methods.sorted((m1, m2) -> ComparisonChain.start()
                                                                      .compare(m1.getDeclaringClass().getName(), m2.getDeclaringClass().getName())
                                                                      .compare(m1.getName(), m2.getName())
                                                                      .result()));
  }

  public <X, Y> Stream<InterfaceWrapper<Y>> asInterface(Function<BoundMethod<X>, Y> transform)
  {
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
