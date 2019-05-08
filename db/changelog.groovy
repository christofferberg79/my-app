databaseChangeLog(logicalFilePath: "src/main/resources/db/changelog.groovy") {
    changeSet(id: "1", author: "cberg") {
        createTable(tableName: "todo") {
            column(name: "id", type: "uuid") {
                constraints(primaryKey: true)
            }
            column(name: "description", type: "varchar(255)") {
                constraints(nullable: false)
            }
        }
        rollback {
            dropTable(tableName: "todo")
        }
    }
}