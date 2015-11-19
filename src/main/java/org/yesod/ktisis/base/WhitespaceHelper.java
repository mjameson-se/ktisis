package org.yesod.ktisis.base;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Joiner;

public class WhitespaceHelper
{
  public static String joinWithWrapIfNecessary(Collection<String> parts, String join, int column, int limit)
  {
    return joinWithWrapIfNecessary(parts, join, "", column, limit);
  }

  public static String joinWithWrapIfNecessary(Collection<String> parts, String joinPre, String joinPost, int column, int limit)
  {
    int total = parts.stream().mapToInt(String::length).sum() + (joinPost.length() + joinPre.length()) * (parts.size() - 1);
    String ws = total > limit ? System.lineSeparator() + spaces(column) : "";
    return Joiner.on(joinPre + ws + joinPost).skipNulls().join(parts);
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
}
