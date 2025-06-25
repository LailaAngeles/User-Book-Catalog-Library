package com.example.parasuratanuser;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LibraryDao {
    @Update
    void update(LibraryEntity entity);
    @Delete
    void delete(LibraryEntity library);
    @Query("SELECT * FROM library")
    List<LibraryEntity> getAll();

    @Query("SELECT * FROM library WHERE id = :id LIMIT 1")
    LibraryEntity getLibraryItemById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LibraryEntity item);

    @Query("UPDATE library SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);

    @Query("DELETE FROM library WHERE id = :id")
    void deleteById(String id);
}
