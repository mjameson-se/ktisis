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
import java.util.Collections;
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
  }

  public static boolean hasTag(String tag, VariableResolver ctx)
  {
    return ctx.getAs("parent.features", Map.class).map(m -> m.containsKey(tag)).orElse(Boolean.FALSE) ||
           ctx.getAs("features", Map.class).map(m -> m.containsKey(tag)).orElse(Boolean.FALSE);
  }

  public static VariableResolver getTag(String tag, VariableResolver ctx)
  {
    VariableResolver features = ctx.getAs("features", Map.class).orElse(Collections.emptyMap())::get;
    VariableResolver parentFeatures = ctx.getAs("parent.features", Map.class).orElse(Collections.emptyMap())::get;
    return features.getAs(tag, Map.class).orElse(parentFeatures.getAs(tag, Map.class).orElse(Collections.emptyMap()))::get;
  }
}
