package org.yesod.ktisis.base;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class WhitespaceHelper
{
  public static String joinWithWrapIfNecessary(Collection<String> parts, String join, int column, int limit)
  {
    return joinWithWrapIfNecessary(parts, join, "", column, limit);
  }

  public static String joinWithWrapIfNecessary(Collection<String> parts, String joinPre, String joinPost, int column, int limit)
  {
    WhitespaceHelperConfig cfg = new WhitespaceHelperConfig.Builder().preJoin(joinPre)
                                                                     .postJoin(joinPost)
                                                                     .lineLength(limit)
                                                                     .wrappedIndent(column)
                                                                     .build();
    return join(cfg, parts);
  }

  public static String spaces(int count)
  {
    return IntStream.range(0, count).mapToObj((i) -> " ").collect(Collectors.joining());
  }

  public static int length(Object... objects)
  {
    int len = 0;
    for (Object obj : objects)
    {
      if (obj != null)
      {
        len += obj.toString().length();
      }
    }
    return len;
  }

  public static String join(WhitespaceHelperConfig config, Collection<String> parts)
  {
    int total = parts.stream().mapToInt(String::length).sum() + (config.getPostJoin().length() + config.getPreJoin().length()) * (parts.size() - 1);
    if (total > config.getLineLength())
    {
      String ws = System.lineSeparator() + spaces(config.getWrappedIndent());
      if (config.isExtraNewlinesIfWrapped())
      {
        parts = ImmutableList.<String> builder().add("").addAll(parts).build();
      }
      return Joiner.on(config.getPreJoin() + ws + config.getPostJoin()).skipNulls().join(parts);
    }
    return Joiner.on(config.getPreJoin() + config.getPostJoin()).skipNulls().join(parts);
  }
}
