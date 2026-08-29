package com.nextlogin.pages;

import com.nextlogin.core.BaseTest;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

  public static final String PATH = "/login";

  // ---- XPath locators -------------------------------------------------
  public static final String HEADING =
      "//div[@data-slot='card-title' and normalize-space()='Welcome back']";
  public static final String SUBHEADING =
      "//div[@data-slot='card-description'"
          + " and normalize-space()='Sign in to continue to your account.']";
  public static final String EMAIL_LABEL = "//label[@for='email']";
  public static final String EMAIL_INPUT = "//input[@id='email']";
  public static final String PASSWORD_LABEL = "//label[@for='password']";
  public static final String PASSWORD_INPUT = "//input[@id='password' and @type='password']";
  public static final String SUBMIT =
      "//button[@type='submit' and normalize-space()='Sign in']";
  public static final String REGISTER_LINK =
      "//a[@href='/register' and normalize-space()='Create one']";
  public static final String ALERT = "//div[@role='alert']";
  public static final String ALERT_TEXT =
      "//div[@role='alert']//div[@data-slot='alert-description']";

  public LoginPage(WebDriver driver) {
    super(driver);
  }

  public LoginPage open() {
    driver.get(BaseTest.baseUrl() + PATH);
    visible(SUBMIT);
    return this;
  }

  public LoginPage fill(String email, String password) {
    type(EMAIL_INPUT, email);
    type(PASSWORD_INPUT, password);
    return this;
  }

  public void submit() {
    click(SUBMIT);
  }

  public AccountPage loginAs(String email, String password) {
    open().fill(email, password).submit();
    waitForPath("/");
    return new AccountPage(driver);
  }

  public String errorText() {
    return textOf(ALERT_TEXT);
  }

  public boolean hasAlert() {
    return isPresent(ALERT);
  }
}
