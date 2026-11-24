package com.ecommerce.ext.web.controller.api;

import org.youngmonkeys.ecommerce.web.controller.api.WebApiCheckoutController;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;

/** Activates the inherited routes of its base controller. */
@Api
@Authenticated
@Controller("/api/v1")
public class KnCheckoutApiController extends WebApiCheckoutController {
}
