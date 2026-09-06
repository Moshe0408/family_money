package com.familymoney.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProfileEntity::class,
        FamilyMemberEntity::class,
        AccountEntity::class,
        CardEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        InvestmentEntity::class,
        ChildEntity::class,
        RecurringEntity::class,
        CategoryRuleEntity::class,
        NetWorthSnapshotEntity::class,
        LiabilityEntity::class,
        AuditEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun cardDao(): CardDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun childDao(): ChildDao
    abstract fun recurringDao(): RecurringDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun liabilityDao(): LiabilityDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "family_money.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
