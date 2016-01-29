package org.yesod.ktisis.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionRegistry;
import org.yesod.ktisis.base.FunctionsPlugin;
import org.yesod.ktisis.base.SubstitutionPlugin;
import org.yesod.reflection.ClasspathSearch;

import com.fasterxml.jackson.jr.ob.JSON;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Ktisis Java generator main entry point 
 */
public class KtisisJava
{
  public class ClassOutput
  {
    public String file;
    public String target;
    public String typeName;
  }

  /**
   * @throws IOException  
   * 
   */
  public KtisisJava() throws IOException
  {
    TemplateProcessor.reset();
    TemplateProcessor.registerPlugin(new ExtensionRegistry());
    TemplateProcessor.registerPlugin(new AnnotationPlugin());
    TemplateProcessor.registerPlugin(new SubstitutionPlugin());
    TemplateProcessor.registerPlugin(new FunctionsPlugin());
    TemplateProcessor.loadAll(new ClasspathSearch().includePackage("org.yesod.ktisis.java").classStream());
  }

  /**
   * Loads registrations for extentions, annotations, and functions from the given package 
   * @param packageName
   * @throws IOException
   */
  public void load(String packageName) throws IOException
  {
    TemplateProcessor.loadAll(new ClasspathSearch().includePackage(packageName).classStream());
  }

  public ClassOutput process(File def, File superdef) throws IOException
  {
    return process(def, Optional.of(superdef));
  }

  public ClassOutput process(File def) throws IOException
  {
    return process(def, Optional.absent());
  }

  private Map<Object, Object> map(File file) throws IOException
  {
    try (InputStream is = new FileInputStream(file))
    {
      return JSON.std.mapFrom(is);
    }
  }

  private ClassOutput process(File def, Optional<File> superdefOpt) throws IOException
  {
    Preconditions.checkArgument(def.exists());
    Map<Object, Object> definition = map(def);
    VariableResolver vr = definition::get;
    if (superdefOpt.isPresent())
    {
      Preconditions.checkArgument(superdefOpt.get().exists());
      Map<?, ?> superdef = map(superdefOpt.get());
      definition.put("supertype", superdef);
    }
    ClassOutput co = new ClassOutput();
    co.target = vr.getAs("package", String.class).orElse("").replace(".", "/");
    co.typeName = vr.getAs("name", String.class).orElse("");
    try (InputStream is = TemplateProcessor.getResource("templates/ktisis/java/FileBase.template", KtisisJava.class))
    {
      co.file = TemplateProcessor.processTemplate(is, vr);
    }
    return co;
  }
}
