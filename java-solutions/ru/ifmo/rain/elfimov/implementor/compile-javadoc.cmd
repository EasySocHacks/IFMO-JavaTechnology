set path="C:\Program Files\Java\jdk-11.0.3\bin"
set kgeorgiyModulesClassesPath="../../../../../../../java-advanced-2020/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor"
set standardApiLink="https://docs.oracle.com/en/java/javase/11/docs/api"

javadoc JarImplementor.java Implementor.java -d _javadoc -link %standardApiLink% -private -author -version %kgeorgiyModulesClassesPath%/JarImpler.java %kgeorgiyModulesClassesPath%/Impler.java %kgeorgiyModulesClassesPath%/ImplerException.java