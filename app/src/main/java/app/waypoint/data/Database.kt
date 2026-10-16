package app.waypoint.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A goal and the method that drives it. Everything the method breaks it into lives in [Node]s;
 * free-text answers of the method and its lenses (SMART, WOOP, GROW…) live in [Field]s.
 */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** [app.waypoint.domain.Method] name of the structure that drives the goal. */
    val method: String,
    val status: Int = ACTIVE,
    /** The short answer to "why does this matter?", shown on the goal card. */
    val why: String = "",
    /** [app.waypoint.domain.Area] ordinal, or -1. */
    val area: Int = -1,
    /** Epoch day the goal (or its 12-week year, or its pact) started. */
    val startDay: Long,
    /** Epoch day of the deadline, if any. */
    val endDay: Long? = null,
    /** An optional number the goal is measured by: 4DX lag measure, kaizen base, a target weight… */
    val unit: String = "",
    val startValue: Double = 0.0,
    val targetValue: Double = 0.0,
    val currentValue: Double = 0.0,
    /** Picks the shape of the goal's constellation. */
    val seed: Int = 0,
    val createdAt: Long,
    val closedAt: Long? = null,
    /** What the user wrote when closing the goal. */
    val note: String = "",
    val sort: Int = 0,
) {
    val hasMetric: Boolean get() = targetValue != startValue

    companion object {
        const val ACTIVE = 0
        /** Waiting for a free slot under the focus limit. */
        const val QUEUED = 1
        const val DONE = 2
        const val DROPPED = 3
    }
}

/**
 * One piece of a goal: a mandala pillar or action, an OKR key result, a 12-week tactic, a 4DX lead measure,
 * a ladder step, an if-then plan, a pre-mortem risk… [kind] is a [app.waypoint.domain.NodeKind] name.
 */
@Entity(tableName = "nodes", indices = [Index("goalId")])
data class Node(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val parentId: Long = 0,
    val kind: String,
    /** Position among siblings; for the mandala, the cell 0..7 around the centre. */
    val slot: Int = 0,
    val title: String = "",
    /** Second text: the "then" of an if-then plan, the prevention of a risk, a description. */
    val note: String = "",
    /** Weekdays it repeats on, Monday = bit 0; 0 means a one-off. */
    val days: Int = 0,
    /** How many times a week it should happen (4DX lead measures, flexible habits); 0 = not counted. */
    val perWeek: Int = 0,
    /** 12-week year: the week (1..12) a tactic belongs to; 0 = every week. */
    val week: Int = 0,
    /** Epoch day it is due on, for one-offs and ladder steps. */
    val dueDay: Long? = null,
    val start: Double = 0.0,
    val target: Double = 0.0,
    val current: Double = 0.0,
    val unit: String = "",
    /** A one-off is finished, a mandala cell is lit, a GROW option is chosen. */
    val done: Boolean = false,
    val doneAt: Long? = null,
    /** Minutes after midnight for a reminder, or -1. */
    val remindAt: Int = -1,
) {
    val repeats: Boolean get() = days != 0 || perWeek > 0
}

/** What happened to a node on one day: a habit ticked (1), a lead measure counted (n), a value logged. */
@Entity(tableName = "logs", primaryKeys = ["nodeId", "day"], indices = [Index("goalId")])
data class Log(
    val nodeId: Long,
    val goalId: Long,
    val day: Long,
    val value: Double = 1.0,
    val at: Long,
)

/** A free-text answer, e.g. "smart.m" or "woop.obstacle". goalId 0 holds app-wide answers. */
@Entity(tableName = "fields", primaryKeys = ["goalId", "key"])
data class Field(
    val goalId: Long,
    val key: String,
    val value: String,
)

/** The weekly review; [day] is the epoch day of the Monday that starts the week. */
@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey val day: Long,
    val went: String = "",
    val blocked: String = "",
    val change: String = "",
    /** How the week felt, 1..5; 0 = not set. */
    val mood: Int = 0,
    val at: Long,
)

/** A wheel-of-life self-check: eight scores 1..10 in [app.waypoint.domain.Area] order. */
@Entity(tableName = "wheel")
data class WheelCheck(
    @PrimaryKey val day: Long,
    val scores: String,
    val at: Long,
) {
    val values: List<Int> get() = scores.split(',').mapNotNull { it.trim().toIntOrNull() }.let { l -> List(8) { l.getOrElse(it) { 5 } } }

    companion object {
        fun of(day: Long, values: List<Int>, at: Long) = WheelCheck(day, values.joinToString(","), at)
    }
}

/**
 * An entry of a goal-choosing tool, before it becomes a goal: one of three Odyssey lives, one of Buffett's 25,
 * a Dreamline dream. [tool] is a [app.waypoint.domain.Method] name.
 */
@Entity(tableName = "ideas")
data class Idea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tool: String,
    /** Odyssey life 0..2; Dreamline category 0 having, 1 being, 2 doing; list order elsewhere. */
    val slot: Int = 0,
    val title: String,
    val text: String = "",
    /** Odyssey gauges 0..100: resources, liking, confidence, coherence. */
    val a: Int = 0,
    val b: Int = 0,
    val c: Int = 0,
    val d: Int = 0,
    /** Dreamline: one-off cost. */
    val amount: Double = 0.0,
    /** Dreamline: monthly cost. */
    val monthly: Double = 0.0,
    /** Buffett: among the top five. */
    val chosen: Boolean = false,
    /** The goal made from it, or 0. */
    val goalId: Long = 0,
    val sort: Int = 0,
    val at: Long,
)

class Snapshot(
    val goals: List<Goal>,
    val nodes: List<Node>,
    val logs: List<Log>,
    val fields: List<Field>,
    val reviews: List<Review>,
    val wheel: List<WheelCheck>,
    val ideas: List<Idea>,
)

@Dao
abstract class WaypointDao {
    // Goals
    @Query("SELECT * FROM goals ORDER BY sort, createdAt")
    abstract fun observeGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY sort, createdAt")
    abstract suspend fun goals(): List<Goal>

    @Query("SELECT * FROM goals WHERE id = :id")
    abstract suspend fun goal(id: Long): Goal?

    @Insert
    abstract suspend fun insertGoal(g: Goal): Long

    @Upsert
    abstract suspend fun upsertGoal(g: Goal)

    @Transaction
    open suspend fun deleteGoal(id: Long) {
        clearGoal(id); clearGoalNodes(id); clearGoalLogs(id); clearGoalFields(id); unlinkIdeas(id)
    }

    @Query("DELETE FROM goals WHERE id = :id") protected abstract suspend fun clearGoal(id: Long)
    @Query("DELETE FROM nodes WHERE goalId = :id") protected abstract suspend fun clearGoalNodes(id: Long)
    @Query("DELETE FROM logs WHERE goalId = :id") protected abstract suspend fun clearGoalLogs(id: Long)
    @Query("DELETE FROM fields WHERE goalId = :id") protected abstract suspend fun clearGoalFields(id: Long)
    @Query("UPDATE ideas SET goalId = 0 WHERE goalId = :id") protected abstract suspend fun unlinkIdeas(id: Long)

    // Nodes
    @Query("SELECT * FROM nodes ORDER BY goalId, parentId, slot, id")
    abstract fun observeNodes(): Flow<List<Node>>

    @Query("SELECT * FROM nodes ORDER BY goalId, parentId, slot, id")
    abstract suspend fun nodes(): List<Node>

    @Query("SELECT * FROM nodes WHERE goalId = :goalId ORDER BY parentId, slot, id")
    abstract suspend fun nodesOf(goalId: Long): List<Node>

    @Query("SELECT * FROM nodes WHERE id = :id")
    abstract suspend fun node(id: Long): Node?

    @Insert
    abstract suspend fun insertNode(n: Node): Long

    @Insert
    abstract suspend fun insertNodes(n: List<Node>): List<Long>

    @Upsert
    abstract suspend fun upsertNode(n: Node): Long

    /** Removes the node, its children and their logs. */
    @Transaction
    open suspend fun deleteNode(id: Long) {
        childIds(id).forEach { deleteNode(it) }
        clearNodeLogs(id)
        clearNode(id)
    }

    @Query("SELECT id FROM nodes WHERE parentId = :id") protected abstract suspend fun childIds(id: Long): List<Long>
    @Query("DELETE FROM nodes WHERE id = :id") protected abstract suspend fun clearNode(id: Long)
    @Query("DELETE FROM logs WHERE nodeId = :id") protected abstract suspend fun clearNodeLogs(id: Long)

    // Logs
    @Query("SELECT * FROM logs ORDER BY day")
    abstract fun observeLogs(): Flow<List<Log>>

    @Query("SELECT * FROM logs ORDER BY day")
    abstract suspend fun logs(): List<Log>

    @Query("SELECT * FROM logs WHERE nodeId = :nodeId AND day = :day")
    abstract suspend fun log(nodeId: Long, day: Long): Log?

    @Upsert
    abstract suspend fun upsertLog(l: Log)

    @Query("DELETE FROM logs WHERE nodeId = :nodeId AND day = :day")
    abstract suspend fun deleteLog(nodeId: Long, day: Long)

    // Fields
    @Query("SELECT * FROM fields")
    abstract fun observeFields(): Flow<List<Field>>

    @Query("SELECT * FROM fields")
    abstract suspend fun fields(): List<Field>

    @Upsert
    abstract suspend fun upsertField(f: Field)

    @Query("DELETE FROM fields WHERE goalId = :goalId AND `key` = :key")
    abstract suspend fun deleteField(goalId: Long, key: String)

    // Reviews
    @Query("SELECT * FROM reviews ORDER BY day DESC")
    abstract fun observeReviews(): Flow<List<Review>>

    @Query("SELECT * FROM reviews ORDER BY day DESC")
    abstract suspend fun reviews(): List<Review>

    @Upsert
    abstract suspend fun upsertReview(r: Review)

    // Wheel
    @Query("SELECT * FROM wheel ORDER BY day")
    abstract fun observeWheel(): Flow<List<WheelCheck>>

    @Query("SELECT * FROM wheel ORDER BY day")
    abstract suspend fun wheel(): List<WheelCheck>

    @Upsert
    abstract suspend fun upsertWheel(w: WheelCheck)

    @Delete
    abstract suspend fun deleteWheel(w: WheelCheck)

    // Ideas
    @Query("SELECT * FROM ideas ORDER BY tool, slot, sort, id")
    abstract fun observeIdeas(): Flow<List<Idea>>

    @Query("SELECT * FROM ideas ORDER BY tool, slot, sort, id")
    abstract suspend fun ideas(): List<Idea>

    @Upsert
    abstract suspend fun upsertIdea(i: Idea): Long

    @Delete
    abstract suspend fun deleteIdea(i: Idea)

    // Backup
    @Transaction
    open suspend fun snapshot() = Snapshot(goals(), nodes(), logs(), fields(), reviews(), wheel(), ideas())

    @Query("DELETE FROM goals") protected abstract suspend fun clearGoals()
    @Query("DELETE FROM nodes") protected abstract suspend fun clearNodes()
    @Query("DELETE FROM logs") protected abstract suspend fun clearLogs()
    @Query("DELETE FROM fields") protected abstract suspend fun clearFields()
    @Query("DELETE FROM reviews") protected abstract suspend fun clearReviews()
    @Query("DELETE FROM wheel") protected abstract suspend fun clearWheel()
    @Query("DELETE FROM ideas") protected abstract suspend fun clearIdeas()

    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun insertGoals(l: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun insertAllNodes(l: List<Node>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun insertLogs(l: List<Log>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun insertFields(l: List<Field>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun insertReviews(l: List<Review>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun insertWheel(l: List<WheelCheck>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun insertIdeas(l: List<Idea>)

    @Transaction
    open suspend fun replaceAll(s: Snapshot) {
        clearGoals(); clearNodes(); clearLogs(); clearFields(); clearReviews(); clearWheel(); clearIdeas()
        insertGoals(s.goals); insertAllNodes(s.nodes); insertLogs(s.logs); insertFields(s.fields)
        insertReviews(s.reviews); insertWheel(s.wheel); insertIdeas(s.ideas)
    }
}

@Database(
    entities = [Goal::class, Node::class, Log::class, Field::class, Review::class, WheelCheck::class, Idea::class],
    version = 1,
    exportSchema = true,
)
abstract class WaypointDatabase : RoomDatabase() {
    abstract fun dao(): WaypointDao

    companion object {
        fun create(context: Context): WaypointDatabase =
            Room.databaseBuilder(context.applicationContext, WaypointDatabase::class.java, "waypoint.db").build()
    }
}
