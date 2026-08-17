package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * StorageManager provides utilities for Storage Access Framework (SAF) using OpenDocumentTree,
 * requesting persistent folder access permissions and enabling reading/writing actual model files via URIs.
 */
class StorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("sora_saf_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_PERSISTED_TREE_URI = "pref_persisted_tree_uri"
    }

    fun savePersistedTreeUri(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            prefs.edit().putString(PREF_PERSISTED_TREE_URI, uri.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getPersistedTreeUri(): Uri? {
        val uriStr = prefs.getString(PREF_PERSISTED_TREE_URI, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isWritePermission
            }
            if (hasPerm) uri else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveModelFileToUri(treeUri: Uri, fileName: String, sourceFile: File): Uri? {
        return try {
            val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            if (!docTree.canWrite()) return null

            val targetDoc = docTree.createFile("application/octet-stream", fileName) ?: return null
            context.contentResolver.openOutputStream(targetDoc.uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            targetDoc.uri
        } catch (_: Exception) {
            null
        }
    }

    fun readModelStreamFromUri(fileUri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(fileUri)
        } catch (_: Exception) {
            null
        }
    }
}
