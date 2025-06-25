package com.example.parasuratanuser;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
/*Ipinapakita nito na ang class ay isang Room database.

entities: mga tables ng database. Dito, may dalawa: LibraryEntity at FavoriteEntity.

version = 2: Ibig sabihin ay version 2 na ng database — ginagamit ito para sa migrations.

exportSchema = false: Ibig sabihin hindi sine-save ang database schema sa file system (usually for internal dev purposes lang).
*/

@Database(
        entities = {LibraryEntity.class, FavoriteEntity.class},
        version  = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
   // Dito Ko kinu-connect ang DAO (Data Access Object) sa Room database natin.
   // ung LibraryDao() at FavoriteDao() ay methods para ma-access ang tables (LibraryEntity, FavoriteEntity).
    public abstract LibraryDao  libraryDao();
    public abstract FavoriteDao favoriteDao();

    private static volatile AppDatabase INSTANCE;
    //Gumagamit tayo ng singleton pattern para hindi mag-create ng maraming instances ng database sa app — isa lang, para efficient.

    public static AppDatabase getDatabase(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDatabase.class,
                                    "parasuratan.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    /* Ito ang method na nagbibigay ng instance ng database.
    Room.databaseBuilder() ay ginagamit para gumawa ng database na may pangalan na "parasuratan.db".
    fallbackToDestructiveMigration() ibig sabihin, kung may database schema change at walang tamang migration code,
    ide-delete ang lumang database at magre-recreate ng bago (data loss alert!).

    Mag-declare ng database na may 2 tables (LibraryEntity at FavoriteEntity)
    Magbigay ng access sa data gamit ang DAO interfaces
    Gumamit ng singleton pattern para isang instance lang ng database ang ginagamit sa buong app

    Gamitin ang Room upang mas mapadali ang pag-manage ng local database sa Android.*/
}
