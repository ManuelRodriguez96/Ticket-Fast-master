package mx.odelant.printorders.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index("productId"), Index("clientId")],
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["product_id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ), ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClientPrice(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val productId: Int,
    val clientId: Int,
    val priceInCents: Int
)