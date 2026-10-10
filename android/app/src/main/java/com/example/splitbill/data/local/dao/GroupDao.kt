package com.example.splitbill.data.local.dao

import androidx.room.*
import com.example.splitbill.data.local.entity.GroupEntityLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
  @Query("SELECT * FROM groups_cache ORDER BY createdAt DESC")
  fun observeGroups(): Flow<List<GroupEntityLocal>>

  @Query("SELECT * FROM groups_cache ORDER BY createdAt DESC")
  suspend fun getGroups(): List<GroupEntityLocal>

  @Query("SELECT * FROM groups_cache WHERE id = :groupId")
  suspend fun getGroupById(groupId: String): GroupEntityLocal?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGroups(groups: List<GroupEntityLocal>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGroup(group: GroupEntityLocal)

  @Query("DELETE FROM groups_cache WHERE id = :groupId")
  suspend fun deleteGroup(groupId: String)

  @Query("DELETE FROM groups_cache")
  suspend fun clearGroups()
}
