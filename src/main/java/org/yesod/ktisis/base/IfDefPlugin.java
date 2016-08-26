/**
 * Copyright (c) 2016 Pelco. All rights reserved.
 *
 * This file contains trade secrets of Pelco.  No part may be reproduced or
 * transmitted in any form by any means or for any purpose without the express
 * written permission of Pelco.
 */

package org.yesod.ktisis.base;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yesod.ktisis.TemplatePlugin;
import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;

import com.google.common.base.Preconditions;

/**
 * 
 */
public class IfDefPlugin implements TemplatePlugin
{
  private static final Pattern pattern = Pattern.compile("#ifdef (.+?)#endif\\n?", Pattern.DOTALL);

  /**
   * @see org.yesod.ktisis.TemplatePlugin#process(java.util.regex.Matcher, org.yesod.ktisis.VariableResolver)
   */
  @Override
  public String process(Matcher match, VariableResolver context)
  {
    String body = match.group(1);
    String[] parts = body.split("\n", 2);
    Preconditions.checkArgument(parts.length == 2, "Failed to parse ifdef: ", body);
    if (context.apply(parts[0]) != null)
    {
      return TemplateProcessor.processTemplate(parts[1], context);
    }
    return null;
  }

  /**
   * @see org.yesod.ktisis.TemplatePlugin#pattern()
   */
  @Override
  public Pattern pattern()
  {
    return pattern;
  }
}
