package org.keycloak.protocol.cas.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Paths;
import java.util.List;

public class PlaywrightDefaults {
  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  private Page page;


  public Page setUp() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
        .setHeadless(true)
        .setArgs(List.of("--disable-dev-shm-usage", "--no-sandbox")));
    context = browser.newContext(
        new Browser.NewContextOptions()
            .setIgnoreHTTPSErrors(true)
            .setRecordVideoDir(Paths.get("target/"))
            .setRecordVideoSize(1920, 1088));
    page = context.newPage();
    return page;
  }

  public void tearDown() {
    if (page != null) {
      page.close();
    }
    if (context != null) {
      context.close();
    }
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }
}
