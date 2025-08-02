package com.greenhorn.neuronet.constant

internal object Constant {
    const val APP_MINIMISE_OR_EXITED = "APP_MINIMISE_OR_EXITED"
    
    /**
     * Default batch size for processing analytics events during synchronization.
     * 
     * This value determines how many events are processed in each batch when
     * syncing events from the local database to the server.
     */
    const val DEFAULT_SYNC_BATCH_SIZE = 10000
    
    /**
     * Minimum valid batch size for sync operations.
     * 
     * Batch sizes below this value are considered invalid and will be replaced
     * with the default batch size.
     */
    const val MIN_SYNC_BATCH_SIZE = 1000
    
    /**
     * Default batch size for database delete operations.
     * 
     * When deleting large numbers of events from the database, operations are
     * split into batches of this size to avoid SQLite limitations and improve
     * performance.
     */
    const val DEFAULT_DELETE_BATCH_SIZE = 900
}