package com.nextlogin.pages;

import java.net.URI;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Shared XPath helpers. Every locator in this suite is an XPath, by design. */
public abstract class BasePage {

  protected static final Duration TIMEOUT = Duration.ofSeconds(30);

  protected final WebDriver driver;
  protected final WebDriverWait wait;

  protected BasePage(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, TIMEOUT);
  }

  public WebElement visible(String xpath) {
    return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
  }

  public WebElement find(String xpath) {
    return driver.findElement(By.xpath(xpath));
  }

  protected void type(String xpath, String text) {
    WebElement field = visible(xpath);
    field.clear();
    field.sendKeys(text);
  }

  public void click(String xpath) {
    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath))).click();
  }

  public boolean isPresent(String xpath) {
    try {
      return driver.findElement(By.xpath(xpath)).isDisplayed();
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  public String textOf(String xpath) {
    return visible(xpath).getText().trim();
  }

  /** Raw HTML attribute, e.g. placeholder or autocomplete. */
  public String attributeOf(String xpath, String name) {
    return visible(xpath).getDomAttribute(name);
  }

  /** Live DOM property, the reliable read for booleans like required and for validationMessage. */
  public String propertyOf(String xpath, String name) {
    return visible(xpath).getDomProperty(name);
  }

  /**
   * Server actions redirect, so tests wait on the resulting path rather than on a spinner.
   * Compares the parsed path exactly: urlContains("/") is true of every URL and waits for nothing.
   */
  public void waitForPath(String path) {
    wait.until(d -> path.equals(pathOf(d.getCurrentUrl())));
  }

  public String currentPath() {
    return pathOf(driver.getCurrentUrl());
  }

  private static String pathOf(String url) {
    String path = URI.create(url).getPath();
    return path == null || path.isEmpty() ? "/" : path;
  }
}
