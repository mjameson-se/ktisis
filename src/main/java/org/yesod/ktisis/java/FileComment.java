package org.yesod.ktisis.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionMethod.ExtensionPoint;

import com.google.common.base.Joiner;

public class FileComment
{

  @ExtensionPoint("file_comment")
  public String fileCommentFromFile(VariableResolver variableResolver) throws IOException
  {
    String filePath = variableResolver.getAs("file_comment", String.class).orElse(System.getProperty("file.comment"));
    try (InputStream is = Files.newInputStream(Paths.get(filePath), StandardOpenOption.READ);
         BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
    {
      Collection<String> lines = new ArrayList<>();
      lines.add("/**");
      br.lines().forEach((line) ->
      {
        lines.add(TemplateProcessor.processTemplate(line, variableResolver));
      });
      lines.add("*/");
      return Joiner.on("\n * ").skipNulls().join(lines);
    }
  }
}
