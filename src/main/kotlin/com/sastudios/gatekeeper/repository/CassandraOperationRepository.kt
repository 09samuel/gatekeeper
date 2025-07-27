package com.sastudios.gatekeeper.repository

import com.sastudios.gatekeeper.entity.CassandraOperation
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.query.Param

interface CassandraOperationRepository : CassandraRepository<CassandraOperation, String> {
    fun findByDocId(docId: String): List<CassandraOperation>
    fun findByDocIdAndRevisionGreaterThan(docId: String, revision: Int): List<CassandraOperation>

//    @Query("SELECT * FROM operations WHERE docid = :docId AND revision > :revision")
//    fun findOpsAfterRevision(
//        @Param("docId") docId: String,
//        @Param("revision") revision: Int
//    ): List<CassandraOperation>


    fun deleteByDocIdAndRevisionLessThanEqual(docId: String, revision: Int)

    @Query("SELECT * FROM operations WHERE docid = :docId AND revision > :fromRev AND revision <= :toRev")
    fun findByDocIdAndRevisionBetween(
        @Param("docId") docId: String,
        @Param("fromRev") fromRev: Int,
        @Param("toRev") toRev: Int
    ): List<CassandraOperation>
}


//interface CassandraOperationRepository : CassandraRepository<CassandraOperation, OperationKey> {
//
//    // Good: Uses correct property path
//    fun findByKeyDocId(docId: String): List<CassandraOperation>
//
//    // Good: Uses correct property path
//    fun findByKeyDocIdAndKeyRevisionGreaterThan(docId: String, revision: Int): List<CassandraOperation>
//
//    // Good: Uses correct property path
//    fun deleteByKeyDocIdAndKeyRevisionLessThanEqual(docId: String, revision: Int)
//
//    // Also good: custom CQL query — make sure it matches your schema
//    @Query("SELECT * FROM operations WHERE docid = :docId AND revision > :fromRev AND revision <= :toRev")
//    fun findByDocIdAndRevisionBetween(
//        @Param("docId") docId: String,
//        @Param("fromRev") fromRev: Int,
//        @Param("toRev") toRev: Int
//    ): List<CassandraOperation>
//}
