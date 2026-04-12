package com.example.rabit.domain.model

data class RemoteFile(
    val name: String,
    val path: String,
    val size: Long,
    val isFolder: Boolean,
    val extension: String,
    val modifiedTime: Long
)
