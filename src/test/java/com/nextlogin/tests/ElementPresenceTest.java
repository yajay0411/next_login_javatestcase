package com.nextlogin.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.nextlogin.core.BaseTest;
import com.nextlogin.pages.LoginPage;
import com.nextlogin.pages.RegisterPage;
import org.testng.annotations.Test;

/** Every element located by XPath, asserted visible with the expected text. */
public class ElementPresenceTest extends BaseTest {

  @Test(description = "Login page renders every field, button and link")
  public void loginPageShowsAllElements() {
    LoginPage page = new LoginPage(driver).open();

    page.find(LoginPage.HEADING);
    page.visible(LoginPage.SUBHEADING);
    assertEquals(page.textOf(LoginPage.EMAIL_LABEL), "Email");
    assertEquals(page.textOf(LoginPage.PASSWORD_LABEL), "Password");
    page.visible(LoginPage.EMAIL_INPUT);
    page.visible(LoginPage.PASSWORD_INPUT);
    page.visible(LoginPage.SUBMIT);
    page.visible(LoginPage.REGISTER_LINK);
  }

  @Test(description = "Login inputs carry the placeholders and autocomplete hints")
  public void loginInputsAreConfiguredForPasswordManagers() {
    LoginPage page = new LoginPage(driver).open();

    assertEquals(page.attributeOf(LoginPage.EMAIL_INPUT, "placeholder"), "you@example.com");
    assertEquals(page.attributeOf(LoginPage.EMAIL_INPUT, "autocomplete"), "email");
    assertEquals(
        page.attributeOf(LoginPage.PASSWORD_INPUT, "autocomplete"), "current-password");
    assertEquals(page.propertyOf(LoginPage.EMAIL_INPUT, "required"), "true");
  }

  @Test(description = "Register page renders all four fields plus the hint and back-link")
  public void registerPageShowsAllElements() {
    RegisterPage page = new RegisterPage(driver).open();

    page.visible(RegisterPage.HEADING);
    page.visible(RegisterPage.SUBHEADING);
    assertEquals(page.textOf(RegisterPage.NAME_LABEL), "Full name");
    assertEquals(page.textOf(RegisterPage.EMAIL_LABEL), "Email");
    assertEquals(page.textOf(RegisterPage.PASSWORD_LABEL), "Password");
    assertEquals(page.textOf(RegisterPage.CONFIRM_LABEL), "Confirm password");
    page.visible(RegisterPage.PASSWORD_HINT);
    page.visible(RegisterPage.SUBMIT);
    page.visible(RegisterPage.LOGIN_LINK);
  }

  @Test(description = "The two auth pages link to each other")
  public void authPagesCrossLink() {
    LoginPage login = new LoginPage(driver).open();
    login.click(LoginPage.REGISTER_LINK);
    login.waitForPath("/register");

    RegisterPage register = new RegisterPage(driver);
    register.click(RegisterPage.LOGIN_LINK);
    register.waitForPath("/login");
  }
}
