package com.familymoney.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = :id")
    fun observe(id: String = "me"): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun get(id: String = "me"): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProfileEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, updatedAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun range(from: Long, to: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tx: List<TransactionEntity>)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Query("DELETE FROM transactions WHERE date BETWEEN :from AND :to")
    suspend fun deleteRange(from: Long, to: Long): Int

    @Query("DELETE FROM transactions WHERE source = :source")
    suspend fun deleteBySource(source: String): Int

    @Query("DELETE FROM transactions WHERE category = :category")
    suspend fun deleteByCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :from AND :to")
    suspend fun countRange(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY type, name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE archived = 0")
    suspend fun all(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(a: AccountEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(a: List<AccountEntity>)
    @Delete suspend fun delete(a: AccountEntity)
    @Query("DELETE FROM accounts") suspend fun clear()
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE archived = 0 ORDER BY provider")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE archived = 0")
    suspend fun all(): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: CardEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(c: List<CardEntity>)
    @Delete suspend fun delete(c: CardEntity)
    @Query("DELETE FROM cards") suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets") fun observeAll(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budgets") suspend fun all(): List<BudgetEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(b: BudgetEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(b: List<BudgetEntity>)
    @Delete suspend fun delete(b: BudgetEntity)
    @Query("DELETE FROM budgets") suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY status, targetDate")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals") suspend fun all(): List<GoalEntity>
    @Query("SELECT * FROM goals WHERE id = :id") suspend fun byId(id: String): GoalEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(g: GoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(g: List<GoalEntity>)
    @Delete suspend fun delete(g: GoalEntity)
    @Query("DELETE FROM goals") suspend fun clear()
}

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY name")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments") suspend fun all(): List<InvestmentEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(i: InvestmentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(i: List<InvestmentEntity>)
    @Delete suspend fun delete(i: InvestmentEntity)
    @Query("DELETE FROM investments") suspend fun clear()
}

@Dao
interface ChildDao {
    @Query("SELECT * FROM children ORDER BY name") fun observeAll(): Flow<List<ChildEntity>>
    @Query("SELECT * FROM children") suspend fun all(): List<ChildEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: ChildEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(c: List<ChildEntity>)
    @Delete suspend fun delete(c: ChildEntity)
    @Query("DELETE FROM children") suspend fun clear()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring WHERE active = 1 ORDER BY dayOfMonth")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE active = 1") suspend fun all(): List<RecurringEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(r: RecurringEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(r: List<RecurringEntity>)
    @Delete suspend fun delete(r: RecurringEntity)
    @Query("DELETE FROM recurring") suspend fun clear()
}

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules WHERE merchantKey = :key")
    suspend fun byMerchant(key: String): CategoryRuleEntity?

    @Query("SELECT * FROM category_rules ORDER BY hits DESC")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(r: CategoryRuleEntity)
    @Query("DELETE FROM category_rules") suspend fun clear()
}

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members") fun observeAll(): Flow<List<FamilyMemberEntity>>
    @Query("SELECT * FROM family_members") suspend fun all(): List<FamilyMemberEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(m: FamilyMemberEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(m: List<FamilyMemberEntity>)
    @Delete suspend fun delete(m: FamilyMemberEntity)
    @Query("DELETE FROM family_members") suspend fun clear()
}

@Dao
interface LiabilityDao {
    @Query("SELECT * FROM liabilities") fun observeAll(): Flow<List<LiabilityEntity>>
    @Query("SELECT * FROM liabilities") suspend fun all(): List<LiabilityEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(l: LiabilityEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(l: List<LiabilityEntity>)
    @Delete suspend fun delete(l: LiabilityEntity)
    @Query("DELETE FROM liabilities") suspend fun clear()
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM net_worth_snapshots ORDER BY yearMonth")
    fun observeAll(): Flow<List<NetWorthSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(s: NetWorthSnapshotEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(s: List<NetWorthSnapshotEntity>)
    @Query("DELETE FROM net_worth_snapshots") suspend fun clear()
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_log ORDER BY at DESC LIMIT 200")
    fun observeAll(): Flow<List<AuditEntity>>

    @Insert suspend fun add(a: AuditEntity)
    @Query("DELETE FROM audit_log") suspend fun clear()
}
