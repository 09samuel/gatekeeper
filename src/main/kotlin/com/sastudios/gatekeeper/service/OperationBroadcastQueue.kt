package com.sastudios.gatekeeper.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sastudios.gatekeeper.model.Operation
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

interface OperationBroadcastQueue {
    fun publish(op: Operation)
}

