#!/bin/bash
pathToKgeorgiyFolder="../../../../../../../java-advanced-2020/lib"
pathToJunit=$(find "$pathToKgeorgiyFolder" -name *junit*)
pathToHamcrest=$(find "$pathToKgeorgiyFolder" -name *hamcrest*)

java -cp "_build;$pathToJunit;$pathToHamcrest" org.junit.runner.JUnitCore ru.ifmo.rain.elfimov.bank.test.BankTests

exit $?
