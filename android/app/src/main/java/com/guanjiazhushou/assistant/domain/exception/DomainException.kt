package com.guanjiazhushou.assistant.domain.exception

open class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ReadOnlyBuildingException(buildingId: Long) :
    DomainException("Building $buildingId is read-only and cannot be modified")

class InvalidVerifyCodeException :
    DomainException("Invalid or mismatched 6-digit verification code")

class InvalidBackupPasswordException :
    DomainException("Incorrect backup password or corrupted backup file")

class BackupPasswordTooShortException :
    DomainException("Backup password must be at least 6 characters")

class ActiveShareGroupExistsException :
    DomainException("Device is already in an active share group session")

class NoActiveShareGroupException :
    DomainException("No active share group session found")

class ShareGroupNotFoundException :
    DomainException("Share group not found")
