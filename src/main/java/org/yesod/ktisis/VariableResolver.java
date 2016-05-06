package org.yesod.ktisis;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface VariableResolver extends Function<String, Object>
{

  default <T> Optional<T> getAs(String key, Class<T> clazz)
  {
    String[] parts = key.split("\\.", 2);
    if (parts.length > 1)
    {
      VariableResolver parent = getSubContext(parts[0]);
      return parent.getAs(parts[1], clazz);
    }
    Object obj = apply(key);
    if (obj != null && clazz.isAssignableFrom(obj.getClass()))
    {
      return Optional.of(clazz.cast(obj));
    }
    if (obj != null)
    {
      System.out.println(key + " is not a " + clazz);
    }
    return Optional.empty();
  }

  default VariableResolver getSubContext(String path)
  {
    Object obj = apply(path);
    if (obj instanceof VariableResolver)
    {
      return (VariableResolver) obj;
    }
    if (obj instanceof Map)
    {
      return ((Map<?, ?>) obj)::get;
    }
    return Collections.emptyMap()::get;
  }

  static VariableResolver wrap(Map<?, ?> map)
  {
    return map::get;
  }

  static VariableResolver merge(VariableResolver... resolvers)
  {
    return (k) ->
    {
      for (VariableResolver res : resolvers)
      {
        Object o = res.apply(k);
        if (o != null)
        {
          return o;
        }
      }
      return null;
    };
  }
}
