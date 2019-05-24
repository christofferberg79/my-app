web: java -DjdbcDatabaseUrl=$JDBC_DATABASE_URL -jar build/libs/my-app.jar
release: java -cp "build/libs/liquibase/*" liquibase.integration.commandline.Main --changeLogFile=db/changelog.groovy --url=$JDBC_DATABASE_URL --classpath=build/libs/liquibase/postgresql.jar update
