package ionutbalosin.training.ecommerce.product;

import static java.lang.String.format;
import static java.math.BigDecimal.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Path.of;
import static java.nio.file.Paths.get;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/*
 * (c) 2022 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
//
// Generates the product sql import based on predefined lists
//
// References:
// - https://en.m.wikipedia.org/wiki/List_of_brand_name_food_products
// - https://github.com/Lifyzer/Lifyzer-Database/tree/master/MySQL
//
public class DatabaseProductGenerator {

  private static final String FILE_NAME = "generated-products.sql";

  private final String INSERT =
      """
      INSERT INTO PRODUCT(NAME, BRAND, CATEGORY, PRICE, CURRENCY, QUANTITY, DAT_INS, USR_INS, STAT)
      VALUES('%s', '%s', '%s', %s, 'EUR', %s, CURRENT_TIMESTAMP, 'autoimport', 'A');
      """;

  private final int MAX_PRODUCTS = 1_000;

  public static void main(String[] args) throws IOException {
    final DatabaseProductGenerator generator = new DatabaseProductGenerator();

    final List<String> names = generator.readFromTxtFile("products/names.list");
    final List<String> brands = generator.readFromTxtFile("products/brands.list");
    final List<String> categories = generator.readFromTxtFile("products/categories.list");

    generator.generateSql(names, brands, categories, FILE_NAME);

    System.out.println("File " + FILE_NAME + " was successfully created!");
  }

  private void generateSql(
      List<String> names, List<String> brands, List<String> categories, String fileName)
      throws IOException {
    final int brandsSize = brands.size();
    final int categoriesSize = categories.size();
    final ThreadLocalRandom tlr = ThreadLocalRandom.current();
    final PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(fileName)));

    names.stream()
        .limit(MAX_PRODUCTS)
        .map(
            name -> {
              final String brand = brands.get(tlr.nextInt(brandsSize - 1));
              final String category = categories.get(tlr.nextInt(categoriesSize - 1));
              final double price = tlr.nextDouble(1, 100);
              final int quantity = tlr.nextInt(100, 200);
              return format(INSERT, name, brand, category, roundDouble(price), quantity);
            })
        .forEach(writer::println);

    writer.close();
  }

  private Set<String> importSql(String fileName) {
    final List<String> names = readFromTxtFile(fileName);
    return names.stream()
        .filter(name -> name.contains("VALUES ("))
        .map(line -> line.split(", '")[1])
        .map(name -> name.substring(0, name.length() - 1))
        .map(name -> name.replace("'", "’"))
        .map(String::trim)
        .map(StringUtils::capitalize)
        .collect(toCollection(() -> new TreeSet<>((s1, s2) -> s1.compareToIgnoreCase(s2))));
  }

  private List<String> readFromTxtFile(String fileName) {
    try {
      URL fileUrl = this.getClass().getResource(fileName);
      if (fileUrl == null) {
        fileUrl = this.getClass().getClassLoader().getResource(fileName);
      }
      return Files.lines(of(fileUrl.toURI()), UTF_8)
          .map(String::trim)
          .map(StringUtils::capitalize)
          .distinct()
          .collect(Collectors.toList());
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Could not read file", e);
    }
  }

  private void writeToTxtFile(Set<String> lines, String fileName) throws IOException {
    final PrintWriter writer = new PrintWriter(newBufferedWriter(get(fileName)));
    lines.stream().forEach(line -> writer.println(line));
    writer.close();
  }

  private double roundDouble(double value) {
    return valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
