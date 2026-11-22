mvn -pl . clean install & ^
mvn -pl ecommerce-ext-sdk clean install & ^
mvn -pl ecommerce-ext-admin-plugin clean install -Pexport,\!test & ^
mvn -pl ecommerce-ext-web-plugin clean install -Pexport,\!test & ^
ezy.bat package & ^
ezy.bat export
