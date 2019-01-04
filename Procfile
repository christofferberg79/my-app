web: java -jar build/libs/my-app.jar
release: java -jar build/libs/liquibase-core.jar --changeLogFile=src/main/resources/db/changelog.xml --url=$JDBC_DATABASE_URL --classpath=build/libs/postgresql.jar update
