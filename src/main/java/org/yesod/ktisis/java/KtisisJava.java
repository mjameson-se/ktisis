package org.yesod.ktisis.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionRegistry;
import org.yesod.ktisis.base.FunctionsPlugin;
import org.yesod.ktisis.base.IfDefPlugin;
import org.yesod.ktisis.base.SubstitutionPlugin;
import org.yesod.ktisis.base.WhitespaceHelper;
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

  private Map<Object, Object> parentConfig = Collections.emptyMap();

  /**
   * @throws IOException  
   * 
   */
  public KtisisJava() throws IOException
  {
    TemplateProcessor.reset();
    TemplateProcessor.registerPlugin(new ExtensionRegistry());
    TemplateProcessor.registerPlugin(new AnnotationPlugin());
    TemplateProcessor.registerPlugin(new IfDefPlugin());
    TemplateProcessor.registerPlugin(new SubstitutionPlugin());
    TemplateProcessor.registerPlugin(new FunctionsPlugin());
    TemplateProcessor.loadAll(new ClasspathSearch().includePackage("org.yesod.ktisis.java").classStream());
  }

  public void setParentConfig(File parentCfg) throws IOException
  {
    setParentConfig(map(parentCfg));
  }

  public void setParentConfig(Map<Object, Object> parentCfg)
  {
    this.parentConfig = parentCfg;
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
    return processInternal(def, Optional.fromNullable(superdef));
  }

  public ClassOutput process(File def) throws IOException
  {
    return processInternal(def, Optional.absent());
  }

  private Map<Object, Object> map(File file) throws IOException
  {
    try (InputStream is = new FileInputStream(file))
    {
      return JSON.std.mapFrom(is);
    }
  }

  private ClassOutput processInternal(File def, Optional<File> superdefOpt) throws IOException
  {
    Preconditions.checkArgument(def.exists());
    Map<Object, Object> definition = map(def);
    definition.put("parent", parentConfig);
    VariableResolver vr = VariableResolver.merge(definition::get, parentConfig::get);
    String pkg = vr.getAs("package", String.class).orElse("");
    if (superdefOpt.isPresent())
    {
      Preconditions.checkArgument(superdefOpt.get().exists());
      Map<?, ?> superdef = map(superdefOpt.get());
      String superpkg = superdef.get("package").toString();
      if (!pkg.equals(superpkg))
      {
        Imports.addImport(superpkg + "." + superdef.get("name").toString());
      }
      definition.put("supertype", superdef);
    }
    ClassOutput co = new ClassOutput();
    co.target = pkg.replace(".", "/");
    co.typeName = vr.getAs("name", String.class).orElse("");
    try (InputStream is = TemplateProcessor.getResource("templates/ktisis/java/FileBase.template", KtisisJava.class))
    {
      co.file = WhitespaceHelper.postProcess(TemplateProcessor.processTemplate(is, vr));
    }
    return co;
  }
}
