package com.sastudios.gatekeeper.service

import com.sastudios.gatekeeper.model.Operation

interface OperationalTransformer {
    fun transform(incoming: Operation, history: List<Operation>): Operation
}
