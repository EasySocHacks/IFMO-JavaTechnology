#!/bin/bash
mkdir -p _build
sourceCodeFolder="../../../../../../java-solutions/ru/ifmo/rain/elfimov/bank"
kgeorgiyJunit=$(find "../../../../../../../java-advanced-2020" -name "*junit*")
javac -cp "$kgeorgiyJunit;$sourceCodeFolder" -d _build $(find "$sourceCodeFolder" -name "*.java")

