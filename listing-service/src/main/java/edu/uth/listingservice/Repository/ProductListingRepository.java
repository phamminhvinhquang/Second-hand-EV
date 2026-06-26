package edu.uth.listingservice.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;


@Repository
public interface ProductListingRepository extends JpaRepository<ProductListing, Long> {
    

    @EntityGraph(attributePaths = {"product", "product.images"})
    Page<ProductListing> findByListingStatus(ListingStatus status, Pageable pageable);


    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("SELECT pl FROM ProductListing pl WHERE pl.listingStatus = :status AND pl.product.productType = :type")
    Page<ProductListing> findByStatusAndProductType(@Param("status") ListingStatus status, @Param("type") String type, Pageable pageable);


    @EntityGraph(attributePaths = {"product", "product.images"})
    Page<ProductListing> findByUserId(Long userId, Pageable pageable);
 
    ProductListing findByProduct_ProductId(Long productId);


 
    @Query(value = "SELECT pl.* FROM product_listings pl JOIN products p ON pl.product_id = p.product_id " +
                   "WHERE pl.listing_status = 'ACTIVE' " +
                   "AND p.product_type = :productType " +
                   "AND p.product_id != :excludeProductId " +
                   "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<ProductListing> findRandomRelatedProducts(@Param("productType") String productType,
                                                   @Param("excludeProductId") Long excludeProductId,
                                                   @Param("limit") int limit);


    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("SELECT pl FROM ProductListing pl WHERE " +
           "LOWER(pl.product.productName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "CAST(pl.userId AS string) LIKE CONCAT('%', :query, '%')")
    Page<ProductListing> searchByProductNameOrUserId(@Param("query") String query, Pageable pageable);

    @Query(value = """
        WITH RankedListings AS (
            SELECT
                listing_id,
                (ROW_NUMBER() OVER(ORDER BY updated_at DESC)) - 1 AS zero_based_index
            FROM
                product_listings 
            WHERE
                user_id = :userId
        )
        SELECT
            rl.zero_based_index
        FROM
            RankedListings rl
        WHERE
            rl.listing_id = :listingId
        """, nativeQuery = true)
    Optional<Integer> findZeroBasedIndexByUserIdAndListingId(
            @Param("userId") Long userId, 
            @Param("listingId") Long listingId
    );
    

    List<ProductListing> findByUserId(Long userId);
}