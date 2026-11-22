mvn -pl . clean install & ^
mvn -pl ecommerce-theme-theme clean install -Pexport,\!test & ^
ezy.bat package & ^
ezy.bat export
