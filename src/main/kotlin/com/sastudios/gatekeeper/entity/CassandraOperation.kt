package com.sastudios.gatekeeper.entity

import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.cql.Ordering
import org.springframework.data.cassandra.core.mapping.PrimaryKey

@Table("operations")
data class CassandraOperation(
    @PrimaryKeyColumn(name = "docid", type = PrimaryKeyType.PARTITIONED)
    val docId: String,

    @PrimaryKeyColumn(name = "revision", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    val revision: Int,

    //@Column("user_id")
    val userId: String,

    //@Column("base_revision")
    val baseRevision: Int,

    val delta: String
)


//@Table("operations")
//data class CassandraOperation(
//    @PrimaryKey
//    val key: OperationKey,
//
//    @Column("userid")
//    val userId: String,
//
//    @Column("baserevision")
//    val baseRevision: Int,
//
//    val delta: String
//)
//
