set kgeorgiyArtifactsPath="../../../../../../../java-advanced-2020/artifacts"
set inKgeorgiyJarClassPath="info/kgeorgiy/java/advanced/implementor"
set inElfimovJarClassPath="ru/ifmo/rain/elfimov/implementor"
if not exist _build mkdir _build
cd _build
echo Main-Class: ru.ifmo.rain.elfimov.implementor.JarImplementor > Manifest

jar xf ../%kgeorgiyArtifactsPath%/info.kgeorgiy.java.advanced.implementor.jar %inKgeorgiyJarClassPath%/Impler.class %inKgeorgiyJarClassPath%/JarImpler.class %inKgeorgiyJarClassPath%/ImplerException.class
javac -d ./ ../Implementor.java ../JarImplementor.java
jar mcf Manifest ../_implemetor.jar %inElfimovJarClassPath%/Implementor.class %inElfimovJarClassPath%/JarImplementor.class %inKgeorgiyJarClassPath%/Impler.class %inKgeorgiyJarClassPath%/JarImpler.class %inKgeorgiyJarClassPath%/ImplerException.class