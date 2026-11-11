package com.guanjiazhushou.assistant.data.backup

import android.content.Context
import com.guanjiazhushou.assistant.data.local.AppDatabase
import com.guanjiazhushou.assistant.domain.exception.InvalidBackupPasswordException
import com.guanjiazhushou.assistant.domain.model.BackupExportResult
import com.guanjiazhushou.assistant.domain.model.BackupImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val databaseFile: File
        get() = context.getDatabasePath(AppDatabase.DATABASE_NAME)

    private val featureIconsDir: File
        get() = File(context.filesDir, FEATURE_ICONS_DIR)

    suspend fun exportBackup(password: String, outputFile: File): BackupExportResult {
        val workDir = File(context.cacheDir, "backup_export_${System.currentTimeMillis()}")
        val packageDir = File(workDir, PACKAGE_ROOT)
        try {
            packageDir.mkdirs()
            copyDatabaseSnapshot(packageDir)
            copyFeatureIcons(packageDir)
            val roomCount = countRoomsInDatabase()
            val exportedAt = System.currentTimeMillis()
            val manifest = buildManifest(
                packageType = "manual_backup",
                encrypted = true,
                roomCount = roomCount,
                exportedAt = exportedAt,
            )
            writeManifest(packageDir, manifest)
            val zipFile = File(workDir, "payload.zip")
            PackageIO.zipDirectory(packageDir, zipFile)
            val checksum = PackageIO.sha256Hex(zipFile)
            File(packageDir, CHECKSUM_FILE).writeText(checksum)
            PackageIO.zipDirectory(packageDir, zipFile)

            val zipBytes = zipFile.readBytes()
            val encrypted = BackupCrypto.encrypt(zipBytes, password)
            writeEncryptedBackup(outputFile, encrypted)

            return BackupExportResult(
                filePath = outputFile.absolutePath,
                roomCount = roomCount,
                fileSizeBytes = outputFile.length(),
            )
        } finally {
            PackageIO.deleteRecursively(workDir)
        }
    }

    suspend fun importBackup(password: String, inputFile: File): BackupImportResult {
        val workDir = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
        try {
            val encrypted = readEncryptedBackup(inputFile)
            val zipBytes = try {
                BackupCrypto.decrypt(encrypted, password)
            } catch (_: Exception) {
                throw InvalidBackupPasswordException()
            }
            val zipFile = File(workDir, "payload.zip")
            zipFile.writeBytes(zipBytes)
            val extractDir = File(workDir, "extracted")
            PackageIO.unzipToDirectory(zipFile, extractDir)

            val packageDir = File(extractDir, PACKAGE_ROOT)
            if (!packageDir.exists()) {
                throw InvalidBackupPasswordException()
            }

            val rollbackDir = File(context.cacheDir, "backup_rollback_${System.currentTimeMillis()}")
            createRollbackSnapshot(rollbackDir)

            try {
                restoreDatabase(File(packageDir, DATABASE_FILE))
                restoreFeatureIcons(File(packageDir, "$ASSETS_DIR/$FEATURE_ICONS_DIR"))
                val roomCount = countRoomsInDatabase()
                return BackupImportResult(
                    roomCount = roomCount,
                    importedAt = System.currentTimeMillis(),
                )
            } catch (error: Exception) {
                restoreFromRollback(rollbackDir)
                throw error
            } finally {
                PackageIO.deleteRecursively(rollbackDir)
            }
        } finally {
            PackageIO.deleteRecursively(workDir)
        }
    }

    fun countRoomsInDatabase(): Int {
        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
        )
        return db.use { database ->
            database.rawQuery("SELECT COUNT(*) FROM rooms", null).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        }
    }

    private fun copyDatabaseSnapshot(packageDir: File) {
        val target = File(packageDir, DATABASE_FILE)
        target.parentFile?.mkdirs()
        databaseFile.copyTo(target, overwrite = true)
    }

    private fun copyFeatureIcons(packageDir: File) {
        val target = File(packageDir, "$ASSETS_DIR/$FEATURE_ICONS_DIR")
        PackageIO.copyRecursively(featureIconsDir, target)
    }

    private fun restoreDatabase(sourceDb: File) {
        if (!sourceDb.exists()) return
        val parent = databaseFile.parentFile ?: return
        parent.mkdirs()
        val walFile = File(parent, "${databaseFile.name}-wal")
        val shmFile = File(parent, "${databaseFile.name}-shm")
        walFile.delete()
        shmFile.delete()
        sourceDb.copyTo(databaseFile, overwrite = true)
    }

    private fun restoreFeatureIcons(sourceDir: File) {
        PackageIO.deleteRecursively(featureIconsDir)
        PackageIO.copyRecursively(sourceDir, featureIconsDir)
    }

    private fun createRollbackSnapshot(rollbackDir: File) {
        val packageDir = File(rollbackDir, PACKAGE_ROOT)
        packageDir.mkdirs()
        if (databaseFile.exists()) {
            databaseFile.copyTo(File(packageDir, DATABASE_FILE), overwrite = true)
        }
        PackageIO.copyRecursively(featureIconsDir, File(packageDir, "$ASSETS_DIR/$FEATURE_ICONS_DIR"))
    }

    private fun restoreFromRollback(rollbackDir: File) {
        val packageDir = File(rollbackDir, PACKAGE_ROOT)
        restoreDatabase(File(packageDir, DATABASE_FILE))
        restoreFeatureIcons(File(packageDir, "$ASSETS_DIR/$FEATURE_ICONS_DIR"))
    }

    private fun buildManifest(
        packageType: String,
        encrypted: Boolean,
        roomCount: Int,
        exportedAt: Long,
    ): JSONObject {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault())
        return JSONObject()
            .put("format_version", FORMAT_VERSION)
            .put("package_type", packageType)
            .put("app_version", APP_VERSION)
            .put("exported_at", formatter.format(Instant.ofEpochMilli(exportedAt)))
            .put("room_count", roomCount)
            .put("encrypted", encrypted)
            .put("includes_custom_icons", featureIconsDir.exists() && featureIconsDir.listFiles()?.isNotEmpty() == true)
    }

    private fun writeManifest(packageDir: File, manifest: JSONObject) {
        File(packageDir, MANIFEST_FILE).writeText(manifest.toString(2))
    }

    private fun writeEncryptedBackup(outputFile: File, payload: BackupCrypto.EncryptedPayload) {
        outputFile.parentFile?.mkdirs()
        DataOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { out ->
            out.write(MAGIC.toByteArray(Charsets.US_ASCII))
            out.writeShort(FORMAT_VERSION)
            out.write(payload.salt)
            out.write(payload.iv)
            out.writeInt(payload.ciphertext.size)
            out.write(payload.ciphertext)
        }
    }

    private fun readEncryptedBackup(inputFile: File): BackupCrypto.EncryptedPayload {
        DataInputStream(BufferedInputStream(FileInputStream(inputFile))).use { input ->
            val magic = ByteArray(MAGIC.length)
            input.readFully(magic)
            if (!magic.contentEquals(MAGIC.toByteArray(Charsets.US_ASCII))) {
                throw InvalidBackupPasswordException()
            }
            val version = input.readShort().toInt()
            if (version != FORMAT_VERSION) {
                throw InvalidBackupPasswordException()
            }
            val salt = ByteArray(16)
            val iv = ByteArray(12)
            input.readFully(salt)
            input.readFully(iv)
            val cipherSize = input.readInt()
            val ciphertext = ByteArray(cipherSize)
            input.readFully(ciphertext)
            return BackupCrypto.EncryptedPayload(salt = salt, iv = iv, ciphertext = ciphertext)
        }
    }

    companion object {
        const val PACKAGE_ROOT = "pm_package"
        const val MANIFEST_FILE = "manifest.json"
        const val DATABASE_FILE = "database.db"
        const val ASSETS_DIR = "assets"
        const val FEATURE_ICONS_DIR = "feature_icons"
        const val CHECKSUM_FILE = "checksum.sha256"
        private const val MAGIC = "PMBK"
        private const val FORMAT_VERSION = 1
        private const val APP_VERSION = "1.0.0"
    }
}
