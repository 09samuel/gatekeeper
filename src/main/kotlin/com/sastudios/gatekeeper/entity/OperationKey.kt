//package com.sastudios.gatekeeper.entity
//
//import org.springframework.data.cassandra.core.cql.Ordering
//import org.springframework.data.cassandra.core.cql.PrimaryKeyType
//import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
//import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
//
//@PrimaryKeyClass
//data class OperationKey(
//    @PrimaryKeyColumn(name = "docid", type = PrimaryKeyType.PARTITIONED)
//    val docId: String,
//
//    @PrimaryKeyColumn(name = "revision", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
//    val revision: Int
//)
