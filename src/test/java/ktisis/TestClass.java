package ktisis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.Test;
import org.yesod.ktisis.TemplateProcessor;
import org.yesod.ktisis.base.ExtensionRegistry;
import org.yesod.ktisis.base.FunctionsPlugin;
import org.yesod.ktisis.base.SubstitutionPlugin;
import org.yesod.ktisis.java.AnnotationPlugin;

import com.fasterxml.jackson.jr.ob.JSON;
import com.google.common.collect.ImmutableMap;

public class TestClass
{
  @Test
  public void test() throws IOException
  {
    System.setProperty("file.comment", "resources/test/Test.comment");
    InputStream is = Files.newInputStream(Paths.get("resources/main/templates/ktisis/java/FileBase.template"), StandardOpenOption.READ);
    InputStream fis = Files.newInputStream(Paths.get("resources/test/TestClass.json"), StandardOpenOption.READ);

    ExtensionRegistry extensionPlugin = new ExtensionRegistry();
    extensionPlugin.loadPackage("org.yesod.ktisis.java");
    TemplateProcessor.registerPlugin(extensionPlugin);
    AnnotationPlugin annotationPlugin = new AnnotationPlugin();
    annotationPlugin.registerAnnotation("field", "@Field(\"${type}\") ");
    annotationPlugin.registerAnnotation("ctor_arg", "@Arg(\"${name}\") ");
    TemplateProcessor.registerPlugin(annotationPlugin);
    TemplateProcessor.registerPlugin(new SubstitutionPlugin());
    TemplateProcessor.registerPlugin(new FunctionsPlugin());
    System.out.println(TemplateProcessor.processTemplate(is, JSON.std.mapFrom(fis)::get));
    Matcher matcher = new FunctionsPlugin().pattern().matcher("#{toUpper(a)}");
    Assert.assertTrue(matcher.find());
    Assert.assertEquals(new FunctionsPlugin().process(matcher, ImmutableMap.of("a", "b")::get), "A");
  }
}
