package org.yesod.reflection;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class ClasspathSearch
{
  private Set<ClassInfo> ci = new HashSet<>();
  private Stream<ClassInfo> stream = ci.stream();
  private final ClassLoader cl;

  public ClasspathSearch()
  {
    this(ClasspathSearch.class.getClassLoader());
  }

  public ClasspathSearch(ClassLoader classLoader)
  {
    this.cl = classLoader;
  }

  public ClasspathSearch includePackage(String packageName) throws IOException
  {
    ci.addAll(ClassPath.from(cl).getTopLevelClasses(packageName));
    return this;
  }

  public ClasspathSearch includePackageRecursive(String pacakgeName) throws IOException
  {
    ci.addAll(ClassPath.from(cl).getTopLevelClassesRecursive(pacakgeName));
    return this;
  }

  public ClasspathSearch excludePackage(String packageName)
  {
    stream = stream.filter((ci) -> !ci.getPackageName().startsWith(packageName));
    return this;
  }

  public ClasspathSearch excludeClass(String className)
  {
    stream = stream.filter((ci) -> !ci.getName().startsWith(className));
    return this;
  }

  public ClassStream classStream()
  {
    return new ClassStream(stream.map(ClassInfo::load));
  }
}
