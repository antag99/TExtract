-injars       build/libs/TExtractFat.jar
-outjars      build/libs/TExtract.jar
-libraryjars  <java.home>/lib/rt.jar
-printmapping build/proguard.map

-overloadaggressively
-repackageclasses ""
-allowaccessmodification

-keep class com.github.antag99.textract.TExtract {
    public static void main(java.lang.String[]);
}
