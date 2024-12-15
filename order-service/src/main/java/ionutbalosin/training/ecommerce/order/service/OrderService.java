package ionutbalosin.training.ecommerce.order.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import ionutbalosin.training.ecommerce.order.dao.OrderJdbcDao;
import ionutbalosin.training.ecommerce.order.model.Order;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/*
 * (c) 2022 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
@Service
public class OrderService {

  private final OrderJdbcDao orderJdbcDao;

  public OrderService(OrderJdbcDao orderJdbcDao) {
    this.orderJdbcDao = orderJdbcDao;
  }

  public List<Order> getOrders(UUID userId) {
    return orderJdbcDao.getAll(userId);
  }

  public int updateOrder(Order order) {
    return orderJdbcDao.update(order);
  }

  public UUID createOrder(Order order) {
    return orderJdbcDao.save(order);
  }

  public Order getOrder(UUID orderId) {
    return orderJdbcDao
        .get(orderId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Not found order id " + orderId));
  }
}
