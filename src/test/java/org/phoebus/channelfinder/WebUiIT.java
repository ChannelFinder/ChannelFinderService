package org.phoebus.channelfinder;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.options.AriaRole;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
import org.phoebus.channelfinder.repository.ChannelRepository;
import org.phoebus.channelfinder.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/**
 * Drives the web UI in a headless browser against the running application.
 *
 * <p>The application listens on a random port with plain HTTP, so the test does not collide with a
 * ChannelFinder that already runs on the same machine.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = Application.class)
@TestPropertySource(
    locations = "classpath:application_test.properties",
    properties = {"server.ssl.enabled=false", "server.http.enable=false"})
class WebUiIT extends ElasticsearchTestBase {

  // The demo_auth user of application.properties that has the admin role.
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASSWORD = "adminPass";
  private static final String WRONG_PASSWORD = "notThePassword";
  private static final String OWNER = "testOwner";
  private static final String TAG = "testTag";
  private static final String CHANNEL_PREFIX = "ABC:DEF:";
  private static final String OTHER_CHANNEL = "XYZ:001";
  private static final int CHANNEL_COUNT = 120;
  private static final int PAGE_SIZE = 50;
  private static final int HTTP_OK = 200;

  @LocalServerPort int port;

  @Autowired ElasticConfig esService;

  @Autowired ChannelRepository channelRepository;

  @Autowired TagRepository tagRepository;

  private Playwright playwright;
  private Browser browser;
  private Page page;
  private final List<String> channelNames = new ArrayList<>();

  @BeforeAll
  void setupAll() {
    ElasticConfigIT.setUp(esService);
    playwright = Playwright.create();
    browser = playwright.chromium().launch();
  }

  @AfterAll
  void closeBrowser() {
    playwright.close();
  }

  @BeforeEach
  void openPage() {
    page = browser.newPage();
  }

  @AfterEach
  void cleanup() {
    page.close();
    channelRepository.deleteAllById(channelNames);
    channelNames.clear();
    tagRepository.findAll().forEach(tag -> tagRepository.deleteById(tag.getName()));
  }

  @Test
  void showsServiceStatusAndChannelCount() {
    createChannels();

    page.navigate(url(""));

    assertThat(page.locator("#cf-status")).containsText("Connected");
    assertThat(page.locator("#cf-total")).hasText(String.valueOf(CHANNEL_COUNT));
  }

  @Test
  void searchShowsHowManyOfTheMatchesAreShownAndPages() {
    createChannels();
    createChannel(OTHER_CHANNEL);

    page.navigate(url("?size=" + PAGE_SIZE));
    page.locator("#cf-query").fill(CHANNEL_PREFIX + "*");
    page.locator("#cf-query").press("Enter");

    Locator range = page.locator("#cf-range");
    assertThat(page.locator("#cf-count")).containsText(CHANNEL_COUNT + " channels");
    assertThat(range).hasText("1–" + PAGE_SIZE + " of " + CHANNEL_COUNT);
    assertThat(page.locator(".cf-row")).hasCount(PAGE_SIZE);

    page.getByLabel("Next page").click();

    assertThat(range).hasText((PAGE_SIZE + 1) + "–" + (2 * PAGE_SIZE) + " of " + CHANNEL_COUNT);
    assertThat(page.locator(".cf-row .cf-mono").first()).hasText(channelName(PAGE_SIZE));
  }

  /**
   * A name is one path segment of the request. Left as it is, {@code ?} or {@code #} would end the
   * path early, and the request would delete the tag {@code test} instead.
   */
  @ParameterizedTest(name = "{0}")
  @CsvSource({"test?Tag, test%3FTag", "test#Tag, test%23Tag"})
  void deleteRequestEncodesTheTagName(String name, String encoded) {
    tagRepository.indexAll(List.of(new Tag("test", OWNER), new Tag(name, OWNER)));

    page.navigate(url(""));
    openDeleteDialog(name);
    Request request = page.waitForRequest(this::isDelete, () -> sendWithPassword(ADMIN_PASSWORD));

    Assertions.assertEquals(
        "/ChannelFinder/resources/tags/" + encoded, URI.create(request.url()).getRawPath());
    Assertions.assertEquals(HTTP_OK, request.response().status());
    Assertions.assertFalse(tagRepository.existsById(name), "the tag was not deleted");
    Assertions.assertTrue(tagRepository.existsById("test"), "the tag 'test' was deleted");
  }

  @Test
  void writeWithWrongPasswordShowsTheErrorInTheDialog() {
    tagRepository.index(new Tag(TAG, OWNER));

    page.navigate(url(""));
    openDeleteDialog(TAG);
    sendWithPassword(WRONG_PASSWORD);

    assertThat(page.locator("#cf-write-failure")).containsText("401");
    assertThat(page.locator("#cf-dialog")).hasAttribute("open", "");
    Assertions.assertTrue(tagRepository.existsById(TAG), "the tag was deleted");
  }

  private String url(String query) {
    return "http://localhost:" + port + "/" + query;
  }

  private static String channelName(int index) {
    return CHANNEL_PREFIX + String.format("%03d", index);
  }

  private void createChannel(String name) {
    channelRepository.index(new Channel(name, OWNER));
    channelNames.add(name);
  }

  private void createChannels() {
    IntStream.range(0, CHANNEL_COUNT).mapToObj(WebUiIT::channelName).forEach(this::createChannel);
  }

  private void openDeleteDialog(String tag) {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Manage")).click();
    page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Tags")).click();
    page.getByLabel("Delete tag " + tag, new Page.GetByLabelOptions().setExact(true)).click();
    assertThat(page.locator("#cf-write-send")).isVisible();
  }

  private void sendWithPassword(String password) {
    page.locator("#cf-login-user").fill(ADMIN_USER);
    page.locator("#cf-login-password").fill(password);
    page.locator("#cf-write-send").click();
  }

  private boolean isDelete(Request request) {
    return "DELETE".equals(request.method());
  }
}
