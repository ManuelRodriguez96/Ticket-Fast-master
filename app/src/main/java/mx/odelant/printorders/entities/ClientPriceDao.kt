package mx.odelant.printorders.entities

import androidx.room.*

@Dao
interface ClientPriceDao {
    @Insert
    fun insertClientPrice(clientPrice: ClientPrice)

    @Update
    fun updateClientPrice(clientPrice: ClientPrice)

    @Delete
    fun deleteClientPrice(clientPrice: ClientPrice)

    @Query("DELETE FROM clientPrice")
    fun deleteAllClientPrice()

    @Query(
        """
        SELECT cp.* FROM clientprice AS cp 
            JOIN product AS p ON cp.productId = p.product_id
        WHERE cp.clientId = :clientId 
        AND p.name LIKE :formattedProductName 
        AND p.isDeleted = 0 
        ORDER BY p.name ASC
    """
    )
    fun getAllClientPricesLikeFormattedProductName(
        clientId: Int,
        formattedProductName: String
    ): List<ClientPrice>

    @Query(
        """
        SELECT * FROM clientprice
        WHERE clientId = :clientId 
        AND productId = :productId 
        LIMIT 1
    """
    )
    fun getByClientAndProduct(clientId: Int, productId: Int): ClientPrice?
}