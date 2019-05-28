databaseChangeLog {
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

    changeSet(id: "2", author: "cberg") {
        addColumn(tableName: "todo") {
            column(name: "done", type: "boolean", defaultValueBoolean: false) {
                constraints(nullable: false)
            }
        }
        rollback {
            dropColumn(tableName: "todo", columnName: "done")
        }
    }
}