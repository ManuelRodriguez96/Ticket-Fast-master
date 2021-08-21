package mx.odelant.printorders.entities

import androidx.room.*

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertClient(client: Client) : Long

    @Update
    fun updateClient(client: Client)

    @Query("UPDATE client SET isDeleted = 1 WHERE id = :clientId")
    fun deleteClient(clientId: Int)

    @Query("DELETE FROM client")
    fun deleteAllClient()

    @Query("SELECT * FROM client WHERE isDeleted = 0")
    fun getAllClients(): List<Client>

    @Query("SELECT * FROM client WHERE name LIKE :clientNameWithModifiers AND isDeleted = 0 ORDER BY name ASC")
    fun getAllClientsLikeName(clientNameWithModifiers: String): List<Client>

    @Query("SELECT * FROM client where id = :clientId LIMIT 1")
    fun getClient(clientId: Int): Client?

    @Query("SELECT * FROM client where name = :clientName LIMIT 1")
    fun getClient(clientName: String): Client?

    @Query("SELECT COUNT(*) FROM client where name = :clientName")
    fun getClientCountByName(clientName: String): Int

    @Query("SELECT COUNT(*) FROM client where name = :clientName and id = :clientId")
    fun getClientCountByNameAndId(clientName: String, clientId: Int): Int
}