web: java -jar build/libs/my-app.jar
release: java -jar build/libs/liquibase-core-3.5.5.jar --changeLogFile=src/main/resources/db/changelog.xml --url=$JDBC_DATABASE_URL --classpath=build/libs/postgresql-42.2.5.jar update
