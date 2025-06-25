package com.example.parasuratanuser;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Update
    void update(FavoriteEntity entity);
    @Delete
    void delete(FavoriteEntity favorite);

    @Query("SELECT * FROM favorites")
    List<FavoriteEntity> getAllFavorites();

    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    FavoriteEntity getFavoriteItemById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteEntity item);

    @Query("UPDATE favorites SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);

    @Query("DELETE FROM favorites WHERE id = :id")
    void deleteById(String id);
}
