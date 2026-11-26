package com.ecommerce.ext.web.payment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Field;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.youngmonkeys.ezyplatform.model.UserModel;
import org.youngmonkeys.ezyplatform.service.UserService;

public class KnOrderViewKeyServiceTest {

    private static final String GUEST = "anonymous" + "a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6";

    private KnPaymentSettings settings;
    private UserService userService;
    private KnOrderViewKeyService service;

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static UserModel user(String username, String email) {
        UserModel user = mock(UserModel.class);
        when(user.getUsername()).thenReturn(username);
        when(user.getEmail()).thenReturn(email);
        return user;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        settings = mock(KnPaymentSettings.class);
        when(settings.get(KnPaymentSettings.SHOP_CONFIRM_KEY)).thenReturn("test-key-1");
        when(settings.get(KnPaymentSettings.PUBLIC_BASE_URL)).thenReturn("https://shop.test/");
        userService = mock(UserService.class);
        UserModel guest = user(GUEST, null);
        UserModel customer = user("khang", "khang@test.vn");
        when(userService.getUserById(7L)).thenReturn(guest);
        when(userService.getUserById(8L)).thenReturn(customer);
        service = new KnOrderViewKeyService();
        inject(service, "settings", settings);
        inject(service, "userService", userService);
    }

    @Test
    public void keyIsStablePerOrderAndDiffersBetweenOrders() {
        String key = service.keyOf(100L);
        assertEquals(key.length(), 32);
        assertEquals(service.keyOf(100L), key);
        assertNotEquals(service.keyOf(101L), key);
    }

    @Test
    public void keyOpensOnlyItsOwnOrder() {
        String key = service.keyOf(100L);
        assertTrue(service.matches(100L, key));
        assertFalse(service.matches(101L, key));
        assertFalse(service.matches(100L, null));
        assertFalse(service.matches(100L, ""));
        assertFalse(service.matches(100L, key.substring(0, 31) + "x"));
    }

    @Test
    public void noSigningKeyMeansNoLink() {
        when(settings.get(KnPaymentSettings.SHOP_CONFIRM_KEY)).thenReturn("");
        assertNull(service.keyOf(100L));
        assertFalse(service.matches(100L, ""));
        assertEquals(service.orderPath(100L, 7L), "/orders/100");
    }

    @Test
    public void onlyGuestOrdersCarryTheKey() {
        assertTrue(service.isGuest(7L));
        assertFalse(service.isGuest(8L));
        assertFalse(service.isGuest(0L));
        assertEquals(service.orderPath(100L, 8L), "/orders/100");
        assertEquals(service.orderUrl(100L, 7L),
            "https://shop.test/orders/100?key=" + service.keyOf(100L));
    }

    @Test
    public void anAnonymousLookingNameWithEmailIsNotAGuest() throws Exception {
        UserModel named = user(GUEST, "real@test.vn");
        when(userService.getUserById(9L)).thenReturn(named);
        assertFalse(service.isGuest(9L));
    }
}
