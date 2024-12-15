package ionutbalosin.training.ecommerce.order.listener;

import ionutbalosin.training.ecommerce.message.schema.payment.PaymentTriggeredEvent;
import ionutbalosin.training.ecommerce.order.model.Order;
import ionutbalosin.training.ecommerce.order.model.mapper.OrderMapper;
import ionutbalosin.training.ecommerce.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/*
 * (c) 2022 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
@Service
public class PaymentEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventListener.class);

  public static final String PAYMENTS_OUT_TOPIC = "ecommerce-payments-out-topic";

  private final OrderMapper orderMapper;
  private final OrderService orderService;

  public PaymentEventListener(OrderMapper orderMapper, OrderService orderService) {
    this.orderMapper = orderMapper;
    this.orderService = orderService;
  }

  @KafkaListener(topics = PAYMENTS_OUT_TOPIC, groupId = "ecommerce_group_id")
  public void receive(PaymentTriggeredEvent paymentEvent) {
    LOGGER.debug("Received message '{}' from Kafka topic '{}'", paymentEvent, PAYMENTS_OUT_TOPIC);
    final Order order = orderMapper.map(paymentEvent);
    orderService.updateOrder(order);
    LOGGER.debug(
        "Order id '{}' for user id '{}' was updated to status '{}'",
        order.getId(),
        order.getUserId(),
        order.getStatus());
  }
}
