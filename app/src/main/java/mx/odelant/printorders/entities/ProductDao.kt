package mx.odelant.printorders.entities

import androidx.room.*

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertProduct(product: Product)

    @Update
    fun updateProduct(product: Product)

    @Query("UPDATE product SET isDeleted = 1 WHERE product_id = :productId")
    fun deleteProduct(productId: Int)

    @Query("DELETE FROM product")
    fun deleteAllProduct()

    @Query("SELECT * FROM product WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): List<Product>

    @Query("SELECT * FROM product WHERE name LIKE :productNameWithModifiers AND isDeleted = 0 ORDER BY name ASC")
    fun getAllProductsLikeName(productNameWithModifiers: String): List<Product>

    @Query("SELECT * FROM product where product_id = :productId LIMIT 1")
    fun getProduct(productId: Int): Product?

    @Query("SELECT * FROM product WHERE name = :productName LIMIT 1")
    fun getProductByName(productName: String): Product?

    @Query("SELECT COUNT(*) FROM product where name = :productName")
    fun getProductCountByName(productName: String): Int

    @Query("SELECT COUNT(*) FROM product where name = :productName and product_id = :productId")
    fun getProductCountByNameAndId(productName: String, productId: Int): Int
}