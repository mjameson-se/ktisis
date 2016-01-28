package ktisis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.VariableResolver;
import org.yesod.ktisis.base.ExtensionRegistry;
import org.yesod.ktisis.base.FunctionsPlugin;
import org.yesod.ktisis.base.SubstitutionPlugin;
import org.yesod.ktisis.java.AnnotationPlugin;
import org.yesod.ktisis.java.AnnotationPlugin.AnnotationRegistration;
import org.yesod.reflection.ClassStream;
import org.yesod.reflection.ClasspathSearch;

import com.fasterxml.jackson.jr.ob.JSON;

public class TestClass
{
  @Test
  public void test() throws IOException
  {
    System.setProperty("file.comment", "resources/test/Test.comment");
    InputStream is = Files.newInputStream(Paths.get("resources/main/templates/ktisis/java/FileBase.template"), StandardOpenOption.READ);
    InputStream fis = Files.newInputStream(Paths.get("resources/test/TestClass.json"), StandardOpenOption.READ);

    TemplateProcessor.registerPlugin(new ExtensionRegistry());
    TemplateProcessor.registerPlugin(new AnnotationPlugin());
    TemplateProcessor.registerPlugin(new SubstitutionPlugin());
    TemplateProcessor.registerPlugin(new FunctionsPlugin());
    TemplateProcessor.loadAll(new ClasspathSearch().includePackage("org.yesod.ktisis.java").classStream());
    TemplateProcessor.loadAll(new ClassStream(AnnotationStuff.class));

    System.out.println(TemplateProcessor.processTemplate(is, JSON.std.mapFrom(fis)::get));
  }

  public static class AnnotationStuff
  {
    @AnnotationRegistration("field")
    public String field(VariableResolver ctx)
    {
      return TemplateProcessor.processTemplate("@Field(\"${type}\")", ctx);
    }

    @AnnotationRegistration("field")
    public String field2(VariableResolver ctx)
    {
      return TemplateProcessor.processTemplate("@Field2(\"${name}\")", ctx);
    }

    @AnnotationRegistration("ctor_arg")
    public String ctorArg(VariableResolver ctx)
    {
      return TemplateProcessor.processTemplate("@Arg(\"${name}\")", ctx);
    }
  }

  @Test
  public void generateWhitespaceHelper() throws Exception
  {
    System.setProperty("file.comment", "resources/test/Test.comment");

    InputStream is = Files.newInputStream(Paths.get("resources/main/templates/ktisis/java/FileBase.template"), StandardOpenOption.READ);
    InputStream fis = Files.newInputStream(Paths.get("resources/test/WhitespaceHelper.json"), StandardOpenOption.READ);

    TemplateProcessor.registerPlugin(new AnnotationPlugin());
    TemplateProcessor.registerPlugin(new ExtensionRegistry());
    TemplateProcessor.registerPlugin(new SubstitutionPlugin());
    TemplateProcessor.registerPlugin(new FunctionsPlugin());
    TemplateProcessor.loadAll(new ClasspathSearch().includePackage("org.yesod.ktisis.java").classStream());

    System.out.println(TemplateProcessor.processTemplate(is, JSON.std.mapFrom(fis)::get));

  }
}
