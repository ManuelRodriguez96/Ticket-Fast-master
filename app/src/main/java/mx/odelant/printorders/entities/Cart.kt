package mx.odelant.printorders.entities

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.*

@Entity(
    indices = [Index("clientId")],
    foreignKeys = [ForeignKey(
        entity = Client::class,
        parentColumns = ["id"],
        childColumns = ["clientId"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class Cart(
    @PrimaryKey(autoGenerate = true) val cart_id: Int,
    @ColumnInfo(name = "clientId") val clientId: Int?,
    @ColumnInfo(name = "dateCreated") val dateCreated: Date,
    @ColumnInfo(name = "totalPriceInCents") val totalPriceInCents: Int,
    @ColumnInfo(name = "isPending") val isPending: Boolean,
    @ColumnInfo(name = "folio") val folio: Int
)

