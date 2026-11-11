package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.guanjiazhushou.assistant.data.local.entity.SearchMatchRow

@Dao
interface SearchDao {
    @Query(
        """
        SELECT 'building' AS match_type,
               b.id AS building_id,
               b.name AS building_name,
               NULL AS room_id,
               NULL AS room_number,
               b.name AS snippet
        FROM buildings b
        WHERE b.name LIKE :pattern ESCAPE '\'

        UNION ALL

        SELECT 'room' AS match_type,
               r.building_id AS building_id,
               b.name AS building_name,
               r.id AS room_id,
               r.room_number AS room_number,
               r.room_number AS snippet
        FROM rooms r
        INNER JOIN buildings b ON b.id = r.building_id
        WHERE r.room_number LIKE :pattern ESCAPE '\'

        UNION ALL

        SELECT 'owner_feedback' AS match_type,
               r.building_id AS building_id,
               b.name AS building_name,
               r.id AS room_id,
               r.room_number AS room_number,
               r.owner_feedback AS snippet
        FROM rooms r
        INNER JOIN buildings b ON b.id = r.building_id
        WHERE r.owner_feedback != ''
          AND r.owner_feedback LIKE :pattern ESCAPE '\'

        UNION ALL

        SELECT 'notes' AS match_type,
               r.building_id AS building_id,
               b.name AS building_name,
               r.id AS room_id,
               r.room_number AS room_number,
               r.other_notes AS snippet
        FROM rooms r
        INNER JOIN buildings b ON b.id = r.building_id
        WHERE r.other_notes != ''
          AND r.other_notes LIKE :pattern ESCAPE '\'

        UNION ALL

        SELECT 'communication' AS match_type,
               r.building_id AS building_id,
               b.name AS building_name,
               r.id AS room_id,
               r.room_number AS room_number,
               rc.content AS snippet
        FROM room_communications rc
        INNER JOIN rooms r ON r.id = rc.room_id
        INNER JOIN buildings b ON b.id = r.building_id
        WHERE rc.content != ''
          AND rc.content LIKE :pattern ESCAPE '\'

        ORDER BY building_name ASC, room_number ASC, match_type ASC
        LIMIT :limit
        """,
    )
    suspend fun search(pattern: String, limit: Int = 200): List<SearchMatchRow>
}
