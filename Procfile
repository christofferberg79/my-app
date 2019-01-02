web: java -jar build/libs/my-app.jar
release: java -jar build/libs/liquibase/liquibase-core-3.6.2.jar --changeLogFile=src/main/resources/db/changelog.groovy --url=$JDBC_DATABASE_URL --classpath=build/libs/liquibase/* updateSQL
