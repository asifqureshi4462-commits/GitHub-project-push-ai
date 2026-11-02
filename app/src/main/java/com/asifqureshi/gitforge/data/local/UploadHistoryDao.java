package com.asifqureshi.gitforge.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UploadHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UploadHistoryEntity entity);

    @Query("SELECT * FROM upload_history ORDER BY timestamp DESC")
    LiveData<List<UploadHistoryEntity>> getAllHistory();

    @Query("SELECT * FROM upload_history ORDER BY timestamp DESC")
    List<UploadHistoryEntity> getAllHistoryDirect();

    @Query("SELECT * FROM upload_history ORDER BY timestamp DESC LIMIT 5")
    LiveData<List<UploadHistoryEntity>> getRecentHistory();

    @Query("SELECT COUNT(*) FROM upload_history")
    LiveData<Integer> getTotalUploadCount();

    @Query("SELECT COUNT(*) FROM upload_history WHERE status = 'SUCCESS'")
    LiveData<Integer> getSuccessfulUploadCount();

    @Query("DELETE FROM upload_history")
    void clearAll();

    @Query("DELETE FROM upload_history WHERE id = :id")
    void deleteById(long id);
}
