package com.example.splitbill.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups_cache")
data class GroupEntityLocal(
  @PrimaryKey val id: String,
  val name: String,
  val description: String? = null,
  val createdBy: String,
  val createdByName: String = "",
  val memberCount: Int = 1,
  val createdAt: String? = null,
  val updatedAt: Long = System.currentTimeMillis()
)
