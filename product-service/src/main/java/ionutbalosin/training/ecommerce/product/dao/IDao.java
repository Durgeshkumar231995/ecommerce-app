package ionutbalosin.training.ecommerce.product.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
 * (c) 2022 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
public interface IDao<T> {

  Optional<T> get(UUID productId);

  List<T> getAll(List<UUID> productIds);

  UUID save(T t);

  int update(T t);

  int delete(UUID id);
}
