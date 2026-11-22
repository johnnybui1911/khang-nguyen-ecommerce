set -e
mvn -pl . clean install
mvn -pl ecommerce-theme-theme clean install -Pexport,\!test
ezy.sh package
ezy.sh export
