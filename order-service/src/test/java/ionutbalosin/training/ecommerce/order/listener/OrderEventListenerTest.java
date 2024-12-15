package ionutbalosin.training.ecommerce.order.listener;

import static ionutbalosin.training.ecommerce.order.KafkaContainerConfiguration.consumerConfigs;
import static ionutbalosin.training.ecommerce.order.listener.OrderEventListener.ORDERS_TOPIC;
import static ionutbalosin.training.ecommerce.order.listener.OrderEventListener.PAYMENTS_IN_TOPIC;
import static java.util.List.of;
import static java.util.UUID.fromString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import ionutbalosin.training.ecommerce.message.schema.order.OrderCreatedEvent;
import ionutbalosin.training.ecommerce.message.schema.order.OrderCurrency;
import ionutbalosin.training.ecommerce.message.schema.order.ProductEvent;
import ionutbalosin.training.ecommerce.message.schema.payment.PaymentCurrency;
import ionutbalosin.training.ecommerce.message.schema.payment.TriggerPaymentCommand;
import ionutbalosin.training.ecommerce.order.KafkaContainerConfiguration;
import ionutbalosin.training.ecommerce.order.KafkaSingletonContainer;
import ionutbalosin.training.ecommerce.order.PostgresqlSingletonContainer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

/*
 * (c) 2022 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
@SpringBootTest()
@Import(KafkaContainerConfiguration.class)
public class OrderEventListenerTest {

  private final UUID PREFILLED_USER_ID = fromString("42424242-4242-4242-4242-424242424242");

  private final ProductEvent PRODUCT_EVENT = getProductEvent();
  private final OrderCreatedEvent ORDER_CREATED = getOrderCreatedEvent();

  @Container
  private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER =
      PostgresqlSingletonContainer.INSTANCE.getContainer();

  @Container
  private static final KafkaContainer KAFKA_CONTAINER =
      KafkaSingletonContainer.INSTANCE.getContainer();

  @Autowired private OrderEventListener classUnderTest;
  @Autowired private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

  @Test
  public void receive() {
    final KafkaConsumer<String, TriggerPaymentCommand> kafkaConsumer =
        new KafkaConsumer(consumerConfigs());
    kafkaConsumer.subscribe(of(PAYMENTS_IN_TOPIC));

    kafkaTemplate.send(ORDERS_TOPIC, ORDER_CREATED);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .until(
            () -> {
              final ConsumerRecords<String, TriggerPaymentCommand> records =
                  kafkaConsumer.poll(Duration.ofMillis(500));
              if (records.isEmpty()) {
                return false;
              }

              assertEquals(1, records.count());
              records.forEach(
                  record -> {
                    assertNotNull(record.value().getId());
                    assertNotNull(record.value().getOrderId());
                    assertEquals(ORDER_CREATED.getUserId(), record.value().getUserId());
                    assertEquals(ORDER_CREATED.getAmount(), record.value().getAmount());
                    assertEquals(
                        "Payment for user id " + record.value().getUserId(),
                        record.value().getDescription());
                    assertEquals(PaymentCurrency.EUR, record.value().getCurrency());
                  });
              return true;
            });
  }

  private ProductEvent getProductEvent() {
    final ProductEvent event = new ProductEvent();
    event.setProductId(fromString("02f85436-397f-11ed-a261-0242ac120002"));
    event.setName("Präsident Ganze Bohne");
    event.setBrand("Julius Meinl");
    event.setPrice(11);
    event.setCurrency(OrderCurrency.EUR);
    event.setQuantity(111);
    event.setDiscount(1);
    return event;
  }

  private OrderCreatedEvent getOrderCreatedEvent() {
    final OrderCreatedEvent event = new OrderCreatedEvent();
    event.setId(fromString("0b9b15a6-397f-11ed-a261-0242ac120002"));
    event.setUserId(PREFILLED_USER_ID);
    event.setProducts(List.of(PRODUCT_EVENT));
    event.setCurrency(OrderCurrency.EUR);
    event.setAmount(22);
    return event;
  }
}
