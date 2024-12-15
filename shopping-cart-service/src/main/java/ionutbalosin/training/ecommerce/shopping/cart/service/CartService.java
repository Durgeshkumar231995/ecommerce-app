package ionutbalosin.training.ecommerce.shopping.cart.service;

import ionutbalosin.training.ecommerce.message.schema.order.OrderCreatedEvent;
import ionutbalosin.training.ecommerce.shopping.cart.dao.CartItemJdbcDao;
import ionutbalosin.training.ecommerce.shopping.cart.event.builder.OrderEventBuilder;
import ionutbalosin.training.ecommerce.shopping.cart.model.CartItem;
import ionutbalosin.training.ecommerce.shopping.cart.sender.OrderEventSender;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * (c) 2022 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
@Service
public class CartService {

  private final CartItemJdbcDao cartItemJdbcDao;
  private final OrderEventBuilder orderEventBuilder;
  private final OrderEventSender orderEventSender;

  public CartService(
      CartItemJdbcDao cartItemJdbcDao,
      OrderEventBuilder orderEventBuilder,
      OrderEventSender orderEventSender) {
    this.cartItemJdbcDao = cartItemJdbcDao;
    this.orderEventBuilder = orderEventBuilder;
    this.orderEventSender = orderEventSender;
  }

  @Transactional
  public void createCartItems(Collection<CartItem> cartItem) {
    cartItemJdbcDao.saveAll(cartItem);
  }

  public int deleteCartItems(UUID userId) {
    return cartItemJdbcDao.deleteAll(userId);
  }

  public List<CartItem> getCartItems(UUID userId) {
    return cartItemJdbcDao.getAll(userId);
  }

  @Async
  public void checkoutCartItems(UUID userId, List<CartItem> cartItems) {
    final OrderCreatedEvent event = orderEventBuilder.createEvent(userId, cartItems);
    orderEventSender.send(event);
  }
}
