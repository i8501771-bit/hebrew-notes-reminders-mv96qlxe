package com.generated.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NotesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "jewish_notes.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NOTES = "notes";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_TYPE = "note_type";
    public static final String COL_COLOR = "color_index";
    public static final String COL_TAG = "tag";
    public static final String COL_REMINDER = "reminder_timestamp";
    public static final String COL_PINNED = "is_pinned";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at";

    public NotesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NOTES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT, "
                + COL_CONTENT + " TEXT, "
                + COL_TYPE + " TEXT, "
                + COL_COLOR + " INTEGER DEFAULT 0, "
                + COL_TAG + " TEXT, "
                + COL_REMINDER + " INTEGER DEFAULT 0, "
                + COL_PINNED + " INTEGER DEFAULT 0, "
                + COL_CREATED_AT + " INTEGER, "
                + COL_UPDATED_AT + " INTEGER"
                + ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    public long insertNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, note.title);
        cv.put(COL_CONTENT, note.content);
        cv.put(COL_TYPE, note.noteType);
        cv.put(COL_COLOR, note.colorIndex);
        cv.put(COL_TAG, note.tag);
        cv.put(COL_REMINDER, note.reminderTimestamp);
        cv.put(COL_PINNED, note.isPinned ? 1 : 0);
        cv.put(COL_CREATED_AT, note.createdAt);
        cv.put(COL_UPDATED_AT, System.currentTimeMillis());
        long id = db.insert(TABLE_NOTES, null, cv);
        note.id = id;
        return id;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, note.title);
        cv.put(COL_CONTENT, note.content);
        cv.put(COL_TYPE, note.noteType);
        cv.put(COL_COLOR, note.colorIndex);
        cv.put(COL_TAG, note.tag);
        cv.put(COL_REMINDER, note.reminderTimestamp);
        cv.put(COL_PINNED, note.isPinned ? 1 : 0);
        cv.put(COL_UPDATED_AT, System.currentTimeMillis());
        return db.update(TABLE_NOTES, cv, COL_ID + " = ?", new String[]{String.valueOf(note.id)});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NOTES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Note getNote(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, null, COL_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Note note = null;
        if (cursor != null && cursor.moveToFirst()) {
            note = cursorToNote(cursor);
            cursor.close();
        }
        return note;
    }

    public List<Note> getAllNotes(String searchQuery, String filter) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder whereClause = new StringBuilder();
        List<String> argsList = new ArrayList<>();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            whereClause.append("(")
                    .append(COL_TITLE).append(" LIKE ? OR ")
                    .append(COL_CONTENT).append(" LIKE ? OR ")
                    .append(COL_TAG).append(" LIKE ?")
                    .append(")");
            String queryArg = "%" + searchQuery.trim() + "%";
            argsList.add(queryArg);
            argsList.add(queryArg);
            argsList.add(queryArg);
        }

        if (filter != null) {
            if ("REMINDERS".equals(filter)) {
                if (whereClause.length() > 0) whereClause.append(" AND ");
                whereClause.append(COL_REMINDER).append(" > 0");
            } else if ("TASKS".equals(filter)) {
                if (whereClause.length() > 0) whereClause.append(" AND ");
                whereClause.append(COL_TYPE).append(" = ?");
                argsList.add(Note.TYPE_CHECKLIST);
            } else if ("PINNED".equals(filter)) {
                if (whereClause.length() > 0) whereClause.append(" AND ");
                whereClause.append(COL_PINNED).append(" = 1");
            }
        }

        String selection = whereClause.length() > 0 ? whereClause.toString() : null;
        String[] selectionArgs = argsList.isEmpty() ? null : argsList.toArray(new String[0]);
        String orderBy = COL_PINNED + " DESC, " + COL_UPDATED_AT + " DESC";

        Cursor cursor = db.query(TABLE_NOTES, null, selection, selectionArgs, null, null, orderBy);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToNote(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    private Note cursorToNote(Cursor cursor) {
        Note note = new Note();
        note.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        note.title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
        note.content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
        note.noteType = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE));
        note.colorIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLOR));
        note.tag = cursor.getString(cursor.getColumnIndexOrThrow(COL_TAG));
        note.reminderTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REMINDER));
        note.isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PINNED)) == 1;
        note.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT));
        note.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT));
        return note;
    }
}
