package com.contractproof.domain

enum class TaskExecutionState {
    Missing,
    PendingEvidence,
    Satisfied,
    PendingException,
    Exception,
}

enum class TaskNextAction {
    CapturePhoto,
    MarkDone,
    ViewTask,
    ReportException,
}
