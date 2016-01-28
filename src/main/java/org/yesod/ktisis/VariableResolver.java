package org.yesod.ktisis;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface VariableResolver extends Function<String, Object>
{

  default <T> Optional<T> getAs(String key, Class<T> clazz)
  {
    Object obj = apply(key);
    if (obj != null && clazz.isAssignableFrom(obj.getClass()))
    {
      return Optional.of(clazz.cast(obj));
    }
    return Optional.empty();
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
