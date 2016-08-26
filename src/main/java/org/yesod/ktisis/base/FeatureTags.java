/**
 * Copyright (c) 2016 Pelco. All rights reserved.
 * $Id$
 *
 * This file contains trade secrets of Pelco.  No part may be reproduced or
 * transmitted in any form by any means or for any purpose without the express
 * written permission of Pelco.
 */

package org.yesod.ktisis.base;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import org.yesod.ktisis.VariableResolver;

/**
 * Feature tags are used to determine which features get included in a generated POJO,
 * because it turns out including/excluding packages is just too unwieldy.
 */
public class FeatureTags
{
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Feature
  {
    String value();

    boolean includeByDefault() default false;
  }

  public static boolean hasTag(Feature tag, VariableResolver ctx)
  {
    return hasTag(tag.value(), ctx, tag.includeByDefault());
  }

  public static boolean hasTag(String tag, VariableResolver ctx, Boolean defaultValue)
  {
    return ctx.getAs("features", Map.class)
              .filter(m -> m.containsKey(tag))
              .map(m -> m.get(tag) == Boolean.TRUE)
              .orElseGet(() -> ctx.getAs("parent.features", Map.class)
                                  .filter(m -> m.containsKey(tag))
                                  .map(m -> m.get(tag) == Boolean.TRUE)
                                  .orElse(defaultValue));
  }
}
