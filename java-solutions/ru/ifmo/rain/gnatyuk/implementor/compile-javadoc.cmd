@echo off

SET pkg_name=ru.ifmo.rain.gnatyuk.implementor
SET mod_dir=ru\ifmo\rain\gnatyuk\implementor
SET mod_name=java-advanced-2020-solutions


SET root_path=%cd%\..\..\..\..\..\..\..\java-advanced-2020
SET modules=%root_path%\..\java-advanced-2020-solutions\java-solutions
SET mod_path=%root_path%\artifacts;%root_path%\lib

SET k_mod=info.kgeorgiy.java.advanced
SET k_implementor=%k_mod%.implementor
SET k_base=%k_mod%.base
SET k_impl_dir=%root_path%\modules\%k_implementor%\info\kgeorgiy\java\advanced\implementor

SET out=%cd%\_build
SET run=%cd%

SET mod_path=%root_path%\artifacts;%root_path%\lib;%run%
cd ..\..\..\..\..\..\..
@echo on
echo %cd%

javadoc -d %run%\_javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api^
 --module-path %mod_path% -private -author %modules%/ru/ifmo/rain/gnatyuk/implementor/Implementor.java^
 %modules%/ru/ifmo/rain/gnatyuk/implementor/ImplementorUtils.java^
 %modules%/ru/ifmo/rain/gnatyuk/implementor/Packer.java^
 %modules%/ru/ifmo/rain/gnatyuk/implementor/JarImplementor.java^
 %k_impl_dir%\Impler.java^
 %k_impl_dir%\JarImpler.java^
 %k_impl_dir%\ImplerException.java
cd %run%