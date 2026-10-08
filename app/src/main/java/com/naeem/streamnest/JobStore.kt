package com.naeem.streamnest

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Durable queue with explicit transactions; all callers run on Dispatchers.IO. */
class JobStore(context: Context, name: String = "transfers.db") : SQLiteOpenHelper(context, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE jobs (
            id TEXT PRIMARY KEY, media_id TEXT NOT NULL, url TEXT NOT NULL, title TEXT NOT NULL,
            thumbnail TEXT NOT NULL, creator TEXT NOT NULL, duration INTEGER NOT NULL,
            height INTEGER NOT NULL, audio INTEGER NOT NULL, status TEXT NOT NULL,
            progress REAL NOT NULL, detail TEXT NOT NULL, output_uri TEXT NOT NULL,
            bytes INTEGER NOT NULL, created_at INTEGER NOT NULL)""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun all(): List<DownloadJob> = readableDatabase.query("jobs", null, null, null, null, null, "created_at DESC").use { c ->
        buildList {
            fun s(key: String) = c.getString(c.getColumnIndexOrThrow(key))
            fun n(key: String) = c.getLong(c.getColumnIndexOrThrow(key))
            while (c.moveToNext()) add(DownloadJob(
                id = s("id"), media = MediaItem(s("media_id"), s("url"), s("title"), s("thumbnail"), s("creator"), n("duration")),
                choice = DownloadChoice(n("height").toInt(), n("audio") == 1L), status = JobStatus.valueOf(s("status")),
                progress = c.getFloat(c.getColumnIndexOrThrow("progress")), detail = s("detail"),
                outputUri = s("output_uri"), bytes = n("bytes"), createdAt = n("created_at")
            ))
        }
    }
    fun save(job: DownloadJob) {
        writableDatabase.insertWithOnConflict("jobs", null, ContentValues().apply {
            put("id", job.id); put("media_id", job.media.id); put("url", job.media.url); put("title", job.media.title)
            put("thumbnail", job.media.thumbnail); put("creator", job.media.creator); put("duration", job.media.duration)
            put("height", job.choice.height); put("audio", if (job.choice.audioOnly) 1 else 0)
            put("status", job.status.name); put("progress", job.progress); put("detail", job.detail)
            put("output_uri", job.outputUri); put("bytes", job.bytes); put("created_at", job.createdAt)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun remove(id: String) { writableDatabase.delete("jobs", "id = ?", arrayOf(id)) }
}
