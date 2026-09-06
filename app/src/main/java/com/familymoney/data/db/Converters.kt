package com.familymoney.data.db

import androidx.room.TypeConverter
import com.familymoney.data.model.AccountType
import com.familymoney.data.model.CardType
import com.familymoney.data.model.GoalStatus
import com.familymoney.data.model.InvestmentType
import com.familymoney.data.model.MemberRole
import com.familymoney.data.model.SyncStatus
import com.familymoney.data.model.TxSource
import com.familymoney.data.model.TxType

class Converters {
    @TypeConverter fun txType(v: TxType) = v.name
    @TypeConverter fun toTxType(v: String) = TxType.valueOf(v)

    @TypeConverter fun accType(v: AccountType) = v.name
    @TypeConverter fun toAccType(v: String) = AccountType.valueOf(v)

    @TypeConverter fun cardType(v: CardType) = v.name
    @TypeConverter fun toCardType(v: String) = CardType.valueOf(v)

    @TypeConverter fun role(v: MemberRole) = v.name
    @TypeConverter fun toRole(v: String) = MemberRole.valueOf(v)

    @TypeConverter fun sync(v: SyncStatus) = v.name
    @TypeConverter fun toSync(v: String) = SyncStatus.valueOf(v)

    @TypeConverter fun goal(v: GoalStatus) = v.name
    @TypeConverter fun toGoal(v: String) = GoalStatus.valueOf(v)

    @TypeConverter fun inv(v: InvestmentType) = v.name
    @TypeConverter fun toInv(v: String) = InvestmentType.valueOf(v)

    @TypeConverter fun src(v: TxSource) = v.name
    @TypeConverter fun toSrc(v: String) = TxSource.valueOf(v)
}
