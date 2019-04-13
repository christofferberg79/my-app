package db

databaseChangeLog {
    changeSet(id: "1", author: "cberg") {
        createTable(tableName: "visit") {
            column(name: "visited_at", type: "timestamp") {
                constraints(nullable: false)
            }
        }
    }

    changeSet(id: "2", author: "cberg") {
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