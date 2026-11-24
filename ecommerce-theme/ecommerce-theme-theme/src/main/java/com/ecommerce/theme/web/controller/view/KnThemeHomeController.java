package com.ecommerce.theme.web.controller.view;

import com.ecommerce.theme.web.view.KnThemePageViewFactory;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.view.View;
import lombok.AllArgsConstructor;

/** Storefront pages. Each page is a template shell filled by /js/ecommerce.js. */
@Controller
@AllArgsConstructor
public class KnThemeHomeController {

    private final KnThemePageViewFactory views;

    /** Browsers ask for /favicon.ico whatever the page icon link says. */
    @DoGet(uri = "/favicon.ico", otherUris = {"/images/favicon.ico"})
    public ResponseEntity favicon() {
        return ResponseEntity.builder()
            .header("Content-Type", "image/svg+xml")
            .header("Cache-Control", "public, max-age=86400")
            .body(FAVICON_SVG)
            .build();
    }

    /** Filled green brilliant-cut diamond; same drawing as /images/favicon.svg. */
    private static final String FAVICON_SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 32 32\">"
            + "<g stroke=\"#053d30\" stroke-width=\"1\" stroke-linejoin=\"round\">"
            + "<path d=\"M2 12 9 6h14l7 6-14 17z\" fill=\"#059669\"/>"
            + "</g>"
            + "<path d=\"M2 12 9 6l3 6z\" fill=\"#34d399\"/>"
            + "<path d=\"M9 6h14l-3 6h-8z\" fill=\"#6ee7b7\"/>"
            + "<path d=\"M12 12l4-6 4 6z\" fill=\"#d1fae5\"/>"
            + "<path d=\"M23 6l7 6H20z\" fill=\"#10b981\"/>"
            + "<path d=\"M2 12h10l4 17z\" fill=\"#047857\"/>"
            + "<path d=\"M12 12h8l-4 17z\" fill=\"#10b981\"/>"
            + "<path d=\"M20 12h10L16 29z\" fill=\"#065f46\"/>"
            + "<path d=\"M2 12 9 6h14l7 6-14 17z\" fill=\"none\" stroke=\"#053d30\""
            + " stroke-width=\"1\" stroke-linejoin=\"round\"/>"
            + "</svg>";

    @DoGet("/")
    public View home() {
        return views.newView("home", "home", "page.home");
    }

    @DoGet("/products")
    public View products() {
        return views.newView("products", "products", "page.products");
    }

    @DoGet("/search")
    public View search() {
        return views.newView("products", "products", "page.search");
    }

    @DoGet("/shopping-cart")
    public View cart() {
        return views.newView("cart", "cart", "page.cart");
    }

    @DoGet("/checkout")
    public View checkout() {
        return views.newView("checkout", "checkout", "page.checkout");
    }

    @DoGet("/login")
    public View login() {
        return views.newView("login", "login", "page.login");
    }

    @DoGet("/register")
    public View register() {
        return views.newView("register", "register", "page.register");
    }

    @DoGet("/forgot-password")
    public View forgotPassword() {
        return views.newView("forgotpassword", "forgot-password", "page.forgotPassword");
    }

    @DoGet("/reset-password")
    public View resetPassword() {
        return views.newView("resetpassword", "reset-password", "page.resetPassword");
    }

    @DoGet("/wishlist")
    public View wishlist() {
        return views.newView("wishlist", "wishlist", "page.wishlist");
    }

    @DoGet("/newsletter/confirm")
    public View newsletterConfirm() {
        return views.newView("newsletter", "newsletter", "page.newsletter");
    }

    @DoGet("/newsletter/unsubscribe")
    public View newsletterUnsubscribe() {
        return views.newView("newsletter", "newsletter", "page.newsletter");
    }

    @DoGet("/contact")
    public View contact() {
        return views.newView("contact", "contact", "page.contact");
    }

    @DoGet("/book-an-appointment")
    public View bookAppointment() {
        return views.newView("appointment", "appointment", "page.appointment");
    }

    @DoGet("/account")
    public View account() {
        return views.newView("account", "account", "page.account");
    }

    @DoGet("/orders")
    public View orders() {
        return views.newView("account", "account", "page.account");
    }

    @DoGet("/customer-service")
    public View customerService() {
        return views.newView("service", "customer-service", "page.service");
    }

    @DoGet("/size-guide")
    public View sizeGuide() {
        return views.newView("sizeguide", "size-guide", "page.sizeGuide");
    }

    @DoGet("/order-status")
    public View orderStatus() {
        return views.newView("orderstatus", "order-status", "page.orderStatus");
    }

    @DoGet("/stores")
    public View stores() {
        return views.newView("stores", "stores", "page.stores");
    }
}
