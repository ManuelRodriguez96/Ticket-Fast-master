package mx.odelant.printorders.dataLayer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mx.odelant.printorders.entities.*

@Database(
    entities = arrayOf(
        Product::class,
        Client::class,
        ClientPrice::class,
        Cart::class,
        CartItem::class,
        CartReturnItem::class
    ),
    version = 3,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun cartItemDao(): CartItemDao
    abstract fun cartReturnItemDao(): CartReturnItemDao
    abstract fun clientDao(): ClientDao
    abstract fun clientPriceDao(): ClientPriceDao


    companion object {

        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
//            context.deleteDatabase("appdb")
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context, AppDatabase::class.java, "appdb")
                    .addMigrations(MIGRATION_2_3)
//                    .allowMainThreadQueries()
                    .build()
            }
            return INSTANCE as AppDatabase
        }
    }

}


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Cart ADD COLUMN folio INT NOT NULL DEFAULT 0")

    }
}


