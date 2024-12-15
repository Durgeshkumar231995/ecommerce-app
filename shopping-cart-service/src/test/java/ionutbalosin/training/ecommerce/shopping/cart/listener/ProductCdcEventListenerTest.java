package ionutbalosin.training.ecommerce.shopping.cart.listener;

import static ionutbalosin.training.ecommerce.shopping.cart.cache.ProductCache.CACHE_INSTANCE;
import static ionutbalosin.training.ecommerce.shopping.cart.listener.ProductCdcEventListener.PRODUCT_DATABASE_TOPIC;
import static java.util.UUID.fromString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import ionutbalosin.training.ecommerce.message.schema.product.ProductCdcKey;
import ionutbalosin.training.ecommerce.message.schema.product.ProductCdcValue;
import ionutbalosin.training.ecommerce.shopping.cart.KafkaContainerConfiguration;
import ionutbalosin.training.ecommerce.shopping.cart.KafkaSingletonContainer;
import ionutbalosin.training.ecommerce.shopping.cart.model.ProductItem;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;

/*
 * (c) 2022 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
@SpringBootTest(
    properties = {
      "product-service.name=localhost",
      "product-service-endpoint.url=http://localhost:8080"
    })
@Import(KafkaContainerConfiguration.class)
public class ProductCdcEventListenerTest {

  private final ProductCdcValue CDC_VALUE = getProductCdcValue();
  private final ProductCdcKey CDC_KEY = getProductCdcKey();

  @Container
  private static final KafkaContainer KAFKA_CONTAINER =
      KafkaSingletonContainer.INSTANCE.getContainer();

  @Autowired private ProductCdcEventListener classUnderTest;
  @Autowired private KafkaTemplate<ProductCdcKey, ProductCdcValue> kafkaTemplate;

  @Test
  public void receive() {
    final Optional<ProductItem> existingProduct =
        CACHE_INSTANCE.getProduct(fromString(CDC_VALUE.getId()));
    assertEquals(false, existingProduct.isPresent());

    kafkaTemplate.send(PRODUCT_DATABASE_TOPIC, CDC_KEY, CDC_VALUE);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .until(
            () -> {
              final Optional<ProductItem> cachedProductOpt =
                  CACHE_INSTANCE.getProduct(fromString(CDC_VALUE.getId()));
              if (cachedProductOpt.isEmpty()) {
                return false;
              }

              assertEquals(true, cachedProductOpt.isPresent());

              final ProductItem cachedProduct = cachedProductOpt.get();
              assertEquals(CDC_VALUE.getName(), cachedProduct.getName());
              assertEquals(CDC_VALUE.getBrand(), cachedProduct.getBrand());
              assertEquals(CDC_VALUE.getCategory(), cachedProduct.getCategory());
              assertEquals(CDC_VALUE.getCurrency(), cachedProduct.getCurrency().toString());
              assertEquals(CDC_VALUE.getQuantity(), cachedProduct.getQuantity());

              return true;
            });
  }

  private ProductCdcValue getProductCdcValue() {

    final ProductCdcValue cdcValue = new ProductCdcValue();
    cdcValue.setProductId(1);
    cdcValue.setId("b6b89618-4152-11ed-b878-0242ac120002");
    cdcValue.setName("Präsident Ganze Bohne");
    cdcValue.setBrand("Julius Meinl");
    cdcValue.setCategory("Beverage");
    cdcValue.setPrice(getPrice());
    cdcValue.setCurrency("EUR");
    cdcValue.setQuantity(111);
    cdcValue.setDatIns(12L);
    cdcValue.setUsrIns("anonymous");
    cdcValue.setStat("A");
    return cdcValue;
  }

  private ProductCdcKey getProductCdcKey() {
    final ProductCdcKey cdcKey = new ProductCdcKey();
    cdcKey.setProductId(1);
    return cdcKey;
  }

  private ByteBuffer getPrice() {
    final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
    byteBuffer.putDouble(11.0);
    byteBuffer.flip();
    return byteBuffer;
  }
}
