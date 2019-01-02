package db

databaseChangeLog {
    changeSet(id: "1", author: "cberg") {
        createTable(tableName: "visit") {
            column(name: "visited_at", type: "timestamp") {
                constraints(nullable: false)
            }
        }
    }
}